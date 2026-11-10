"""Generates the Fabric glue for the NeoForge shim from the access transformers and the shim's injected interfaces.

Outputs (all committed, re-run this script after changing the inputs):
 - fabric/src/main/resources/mekanism.accesswidener: access widener equivalent of NeoForge's, Mekanism's and the shim's access transformers
 - the "loom:injected_interfaces" entry of fabric/src/main/resources/fabric.mod.json
 - fabric/src/main/java/mekanism/fabric/mixin/shim/*: mixins that make vanilla classes implement the injected interfaces at runtime
   (Loom's interface injection only applies at compile time), and the mekanism-shim.mixins.json config listing them

Usage: python fabric/gen_fabric_glue.py <path to a merged Minecraft jar (Mojang names)>
"""
import json
import os
import re
import struct
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FABRIC = os.path.join(REPO, 'fabric')
SHIM_META = os.path.join(REPO, 'shim', 'src', 'main', 'resources', 'META-INF')
AT_FILES = [
    os.path.join(SHIM_META, 'shim-accesstransformer.cfg'),
    os.path.join(SHIM_META, 'neoforge-accesstransformer.cfg'),
    os.path.join(SHIM_META, 'neoforge-accesstransformergenerated.cfg'),
    os.path.join(REPO, 'neoforge', 'src', 'main', 'resources', 'META-INF', 'accesstransformer.cfg'),
    os.path.join(REPO, 'neoforge', 'src', 'additions', 'resources', 'META-INF', 'accesstransformer.cfg'),
    os.path.join(REPO, 'neoforge', 'src', 'tools', 'resources', 'META-INF', 'accesstransformer.cfg'),
]
MIXIN_PACKAGE = 'mekanism.fabric.mixin.shim'

jar = zipfile.ZipFile(sys.argv[1])
_class_cache = {}


def read_class(internal_name):
    """Returns (access_flags, {field name: descriptor}) for the class, or None if it is not in the jar."""
    if internal_name in _class_cache:
        return _class_cache[internal_name]
    try:
        data = jar.read(internal_name + '.class')
    except KeyError:
        _class_cache[internal_name] = None
        return None
    pos = 8
    count = struct.unpack_from('>H', data, pos)[0]
    pos += 2
    utf8 = {}
    i = 1
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            length = struct.unpack_from('>H', data, pos)[0]
            utf8[i] = data[pos + 2:pos + 2 + length].decode('utf-8', 'replace')
            pos += 2 + length
        elif tag in (3, 4):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            pos += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            pos += 4
        elif tag == 15:
            pos += 3
        else:
            raise ValueError(f'Unknown constant pool tag {tag} in {internal_name}')
        i += 1
    access = struct.unpack_from('>H', data, pos)[0]
    pos += 6
    interfaces = struct.unpack_from('>H', data, pos)[0]
    pos += 2 + 2 * interfaces
    fields = {}
    field_count = struct.unpack_from('>H', data, pos)[0]
    pos += 2
    for _ in range(field_count):
        _, name_idx, desc_idx, attr_count = struct.unpack_from('>HHHH', data, pos)
        pos += 8
        fields[utf8[name_idx]] = utf8[desc_idx]
        for _ in range(attr_count):
            length = struct.unpack_from('>I', data, pos + 2)[0]
            pos += 6 + length
    _class_cache[internal_name] = (access, fields)
    return _class_cache[internal_name]


def is_interface(internal_name):
    info = read_class(internal_name)
    return info is not None and bool(info[0] & 0x0200)


# --- Access widener ---
entries = set()
missing = []
for at_file in AT_FILES:
    if not os.path.exists(at_file):
        continue
    for raw in open(at_file, encoding='utf-8'):
        line = raw.split('#', 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        modifier, cls = parts[0], parts[1].replace('.', '/')
        if modifier.startswith('private'):
            continue  # narrowing access is not supported (or needed) on Fabric
        remove_final = modifier.endswith('-f')
        base = modifier[:-2] if remove_final else modifier
        if read_class(cls) is None:
            missing.append(line)
            continue
        if len(parts) == 2:
            entries.add(f'accessible class {cls}')
            if remove_final or base == 'protected':
                entries.add(f'extendable class {cls}')
            continue
        member = parts[2]
        if '(' in member:
            name, desc = member[:member.index('(')], member[member.index('('):]
            if base == 'protected':
                # access wideners can't make methods protected, but extendable methods are protected and can be overridden
                entries.add(f'extendable method {cls} {name} {desc}')
            else:
                entries.add(f'accessible method {cls} {name} {desc}')
                if not name.startswith('<'):
                    # accessible alone makes private methods final; NeoForge's transformers leave them overridable
                    entries.add(f'extendable method {cls} {name} {desc}')
            # make sure the owner class is accessible as well
            entries.add(f'accessible class {cls}')
        else:
            desc = read_class(cls)[1].get(member)
            if desc is None:
                missing.append(line)
                continue
            entries.add(f'accessible field {cls} {member} {desc}')
            if remove_final:
                entries.add(f'mutable field {cls} {member} {desc}')
            entries.add(f'accessible class {cls}')
aw_path = os.path.join(FABRIC, 'src', 'main', 'resources', 'mekanism.accesswidener')
with open(aw_path, 'w', encoding='utf-8', newline='\n') as aw:
    aw.write('accessWidener v2 official\n')
    aw.write('# Generated by fabric/gen_fabric_glue.py from the access transformers of NeoForge, the shim and Mekanism. Do not edit manually.\n')
    for entry in sorted(entries):
        aw.write(entry + '\n')

# --- Injected interfaces ---
inj = json.load(open(os.path.join(SHIM_META, 'shim-injected-interfaces.json')))


def loom_signature(iface):
    # NeoForge style "pkg/Iface<T>" -> Loom style "pkg/Iface<TT;>"
    m = re.match(r'([\w/$]+)(?:<(.+)>)?$', iface)
    name, generics = m.group(1), m.group(2)
    if not generics:
        return name
    args = ''.join(f'T{g.strip()};' for g in generics.split(','))
    return f'{name}<{args}>'


fmj_path = os.path.join(FABRIC, 'src', 'main', 'resources', 'fabric.mod.json')
fmj = json.load(open(fmj_path, encoding='utf-8'))
fmj.setdefault('custom', {})['loom:injected_interfaces'] = {target: [loom_signature(i) for i in ifaces] for target, ifaces in sorted(inj.items())}
fmj['accessWidener'] = 'mekanism.accesswidener'
mixin_configs = [c for c in fmj.get('mixins', []) if c != 'mekanism-shim.mixins.json']
fmj['mixins'] = mixin_configs + ['mekanism-shim.mixins.json']
with open(fmj_path, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(fmj, f, indent=2)
    f.write('\n')

# --- Runtime mixins for the injected interfaces ---
mixin_dir = os.path.join(FABRIC, 'src', 'main', 'java', *MIXIN_PACKAGE.split('.'))
os.makedirs(mixin_dir, exist_ok=True)
for existing in os.listdir(mixin_dir):
    if existing.endswith('.java') and existing != 'package-info.java':
        os.remove(os.path.join(mixin_dir, existing))
common, client = [], []
for target, ifaces in sorted(inj.items()):
    if read_class(target) is None:
        missing.append(f'injection target {target}')
        continue
    simple = target.split('/')[-1].replace('$', '_') + 'ShimMixin'
    target_src = target.replace('/', '.').replace('$', '.')
    iface_srcs = [re.sub(r'<.*>$', '', i).replace('/', '.').replace('$', '.') for i in ifaces]
    kind = 'interface' if is_interface(target) else 'abstract class'
    relation = 'extends' if kind == 'interface' else 'implements'
    body = f'''package {MIXIN_PACKAGE};

import org.spongepowered.asm.mixin.Mixin;

/**
 * Generated by fabric/gen_fabric_glue.py: makes {{@link {target_src}}} implement the shim's injected interfaces at runtime.
 */
@Mixin({target_src}.class)
public {kind} {simple} {relation} {', '.join(iface_srcs)} {{
}}
'''
    with open(os.path.join(mixin_dir, simple + '.java'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(body)
    (client if target.startswith('net/minecraft/client/') else common).append(simple)
with open(os.path.join(mixin_dir, 'package-info.java'), 'w', encoding='utf-8', newline='\n') as f:
    f.write('@NullMarked\npackage ' + MIXIN_PACKAGE + ';\n\nimport org.jspecify.annotations.NullMarked;\n')
mixins_json = {
    'required': True,
    'package': MIXIN_PACKAGE,
    'compatibilityLevel': 'JAVA_25',
    'mixins': common,
    'client': client,
    'injectors': {'defaultRequire': 1},
}
with open(os.path.join(FABRIC, 'src', 'main', 'resources', 'mekanism-shim.mixins.json'), 'w', encoding='utf-8', newline='\n') as f:
    json.dump(mixins_json, f, indent=2)
    f.write('\n')

print(f'access widener: {len(entries)} entries; injected interfaces: {len(inj)} targets ({len(common)} common, {len(client)} client mixins)')
if missing:
    print('Skipped (not found in the jar):')
    for m in missing:
        print('  ' + m)
