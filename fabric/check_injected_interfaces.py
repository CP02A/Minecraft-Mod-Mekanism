"""Checks the shim's injected interfaces for default methods that conflict at runtime.

Interface injection makes vanilla classes implement the shim's extension interfaces. Fabric API (and other mods on the classpath) inject their own
interfaces into vanilla classes as well. If a class ends up implementing two unrelated interfaces (directly or through its superclasses and
interfaces) that both declare a default method with the same name and descriptor, and the class doesn't implement the method itself, the JVM throws an
IncompatibleClassChangeError when the method is invoked. The compiler doesn't catch this, as the vanilla classes are not compiled against all the
injected interfaces at once.

Reported (exit code 1):
- conflicting defaults where at least one of the conflicting interfaces is one of the shim's (or is added by a Mekanism mixin), whether the other one
  is another shim interface, a Fabric API/mod injected interface or a vanilla interface
- shim defaults that a vanilla class (or one of its superclasses) declares as a non-public instance method with the same signature, which the JVM
  selects when the interface method is invoked, throwing an IllegalAccessError
Notes (not failing, listed with --verbose): shim defaults that a vanilla class already implements publicly (the shim's default is never used there,
like on NeoForge).

Methods and interfaces added by mixins (Mekanism's and those of the mods on the classpath) are taken into account, so a mixin implementing the method
on the conflicting class (like TooltipFlagDefaultMixin) resolves a conflict.

Usage: python fabric/check_injected_interfaces.py [--verbose] [--mixin-config <Mekanism mixin config json>]... [--mixin-classes <dir or jar>]...
           <merged Minecraft jar (Mojang names)> <shim classes directory or jar> [mod jars (e.g. Fabric API modules) or directories]...
       Arguments can be passed in a file (one per line) with @<file>. ./gradlew :fabric:checkInjectedInterfaces runs it with the compile classpath.
"""
import argparse
import json
import os
import struct
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INJECTED = os.path.join(REPO, 'shim', 'src', 'main', 'resources', 'META-INF', 'shim-injected-interfaces.json')

ACC_PUBLIC, ACC_PRIVATE, ACC_STATIC, ACC_INTERFACE, ACC_ABSTRACT = 0x0001, 0x0002, 0x0008, 0x0200, 0x0400
MIXIN_ANNOTATION = 'Lorg/spongepowered/asm/mixin/Mixin;'


class Source:
    """A jar or classes directory."""

    def __init__(self, path):
        self.path = path
        self.zip = zipfile.ZipFile(path) if os.path.isfile(path) else None

    def read(self, name):
        if self.zip is not None:
            try:
                return self.zip.read(name)
            except KeyError:
                return None
        file = os.path.join(self.path, name)
        if os.path.isfile(file):
            with open(file, 'rb') as f:
                return f.read()
        return None

    def class_names(self):
        if self.zip is not None:
            return [n[:-6] for n in self.zip.namelist() if n.endswith('.class')]
        names = []
        for root, _, files in os.walk(self.path):
            for file in files:
                if file.endswith('.class'):
                    names.append(os.path.relpath(os.path.join(root, file), self.path).replace(os.sep, '/')[:-6])
        return names

    def nested_jars(self):
        """Jar-in-jar dependencies declared by a Fabric mod (extracted in memory)."""
        mod_json = self.read('fabric.mod.json')
        if mod_json is None or self.zip is None:
            return []
        result = []
        for jar in json.loads(mod_json.decode('utf-8')).get('jars', []):
            data = self.read(jar['file'])
            if data is not None:
                import io
                nested = Source.__new__(Source)
                nested.path = f'{self.path}!/{jar["file"]}'
                nested.zip = zipfile.ZipFile(io.BytesIO(data))
                result.append(nested)
        return result


def parse_class(data, name):
    """Returns a dict with access, super, interfaces, methods ({(name, descriptor): access}) and mixin targets (or None if not a mixin)."""
    pos = 8
    count = struct.unpack_from('>H', data, pos)[0]
    pos += 2
    utf8, classes = {}, {}
    i = 1
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            length = struct.unpack_from('>H', data, pos)[0]
            utf8[i] = data[pos + 2:pos + 2 + length].decode('utf-8', 'replace')
            pos += 2 + length
        elif tag == 7:
            classes[i] = struct.unpack_from('>H', data, pos)[0]
            pos += 2
        elif tag in (3, 4):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1
        elif tag in (8, 16, 19, 20):
            pos += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            pos += 4
        elif tag == 15:
            pos += 3
        else:
            raise ValueError(f'Unknown constant pool tag {tag} in {name}')
        i += 1
    access, _, super_idx, iface_count = struct.unpack_from('>HHHH', data, pos)
    pos += 8
    super_name = utf8[classes[super_idx]] if super_idx else None
    interfaces = []
    for _ in range(iface_count):
        interfaces.append(utf8[classes[struct.unpack_from('>H', data, pos)[0]]])
        pos += 2
    methods = {}
    for member_kind in ('fields', 'methods'):
        member_count = struct.unpack_from('>H', data, pos)[0]
        pos += 2
        for _ in range(member_count):
            m_access, name_idx, desc_idx, attr_count = struct.unpack_from('>HHHH', data, pos)
            pos += 8
            if member_kind == 'methods':
                methods[(utf8[name_idx], utf8[desc_idx])] = m_access
            for _ in range(attr_count):
                length = struct.unpack_from('>I', data, pos + 2)[0]
                pos += 6 + length
    mixin_targets = None
    attr_count = struct.unpack_from('>H', data, pos)[0]
    pos += 2
    for _ in range(attr_count):
        attr_name_idx, length = struct.unpack_from('>HI', data, pos)
        attr_start = pos + 6
        pos = attr_start + length
        if utf8.get(attr_name_idx) not in ('RuntimeInvisibleAnnotations', 'RuntimeVisibleAnnotations'):
            continue
        p = attr_start
        num_annotations = struct.unpack_from('>H', data, p)[0]
        p += 2
        for _ in range(num_annotations):
            p, annotation_type, values = read_annotation(data, p, utf8)
            if annotation_type == MIXIN_ANNOTATION:
                mixin_targets = []
                for value in values.get('value', []):
                    mixin_targets.append(value[1:-1])  # class descriptor
                for value in values.get('targets', []):
                    mixin_targets.append(value.replace('.', '/'))
    return {'access': access, 'super': super_name, 'interfaces': interfaces, 'methods': methods, 'mixin': mixin_targets}


def read_annotation(data, p, utf8):
    type_idx, pairs = struct.unpack_from('>HH', data, p)
    p += 4
    values = {}
    for _ in range(pairs):
        name_idx = struct.unpack_from('>H', data, p)[0]
        p, value = read_element_value(data, p + 2, utf8)
        values[utf8[name_idx]] = value if isinstance(value, list) else [value]
    return p, utf8[type_idx], values


def read_element_value(data, p, utf8):
    tag = chr(data[p])
    p += 1
    if tag in 'BCDFIJSZs':
        idx = struct.unpack_from('>H', data, p)[0]
        return p + 2, utf8.get(idx)
    if tag == 'c':
        idx = struct.unpack_from('>H', data, p)[0]
        return p + 2, utf8[idx]
    if tag == 'e':
        return p + 4, None
    if tag == '@':
        p, _, _ = read_annotation(data, p, utf8)
        return p, None
    if tag == '[':
        count = struct.unpack_from('>H', data, p)[0]
        p += 2
        values = []
        for _ in range(count):
            p, value = read_element_value(data, p, utf8)
            values.append(value)
        return p, values
    raise ValueError(f'Unknown annotation element tag {tag}')


def parse_class_tweaker(text):
    """Interface injections of an access widener/class tweaker (v2 access wideners and class tweakers)."""
    result = {}
    for line in text.splitlines():
        parts = line.split('#')[0].split()
        if len(parts) == 3 and parts[0] in ('inject-interface', 'transitive-inject-interface'):
            result.setdefault(parts[1], []).append(parts[2].split('<')[0])
    return result


def mixin_configs_of(mod_json):
    configs = []
    for entry in mod_json.get('mixins', []):
        configs.append(entry if isinstance(entry, str) else entry['config'])
    return configs


def mixin_classes_of(config):
    package = config.get('package', '').replace('.', '/')
    names = []
    for key in ('mixins', 'client', 'server'):
        for mixin in config.get(key, []) or []:
            names.append(f'{package}/{mixin.replace(".", "/")}')
    return names


def main():
    parser = argparse.ArgumentParser(description='Checks injected interfaces for conflicting default methods', fromfile_prefix_chars='@')
    parser.add_argument('--verbose', action='store_true', help='also list shim defaults that vanilla classes already implement')
    parser.add_argument('--mixin-config', action='append', default=[], help="Mekanism mixin config (json), its mixins' interfaces count as the shim's")
    parser.add_argument('--mixin-classes', action='append', default=[], help='directory or jar containing the classes of the --mixin-config mixins')
    parser.add_argument('minecraft')
    parser.add_argument('shim')
    parser.add_argument('classpath', nargs='*', help='mod jars (their injected interfaces and mixins are taken into account) or directories')
    args = parser.parse_args()

    mc = Source(args.minecraft)
    shim = Source(args.shim)
    mods = []
    for path in args.classpath:
        if os.path.exists(path) and os.path.abspath(path) != os.path.abspath(args.minecraft):
            source = Source(path)
            mods.append(source)
            mods.extend(source.nested_jars())
    mixin_class_sources = [Source(p) for p in args.mixin_classes]
    lookup = [mc, shim] + mixin_class_sources + mods

    cache = {}

    def raw(name):
        if name in cache:
            return cache[name]
        info = None
        for source in lookup:
            data = source.read(name + '.class')
            if data is not None:
                info = parse_class(data, name)
                break
        cache[name] = info
        return info

    shim_injected = {target: [i.split('<')[0] for i in ifaces] for target, ifaces in json.load(open(INJECTED)).items()}
    shim_ifaces = {i for ifaces in shim_injected.values() for i in ifaces}
    # target -> [(interface, origin)]
    injected = {}
    for target, ifaces in shim_injected.items():
        for iface in ifaces:
            injected.setdefault(target, []).append(iface)
    # target -> {(name, desc): (access, origin)} and target -> [interfaces] added by mixins
    mixin_methods, mixin_ifaces = {}, {}
    origins = {}

    def add_mixins(class_names, sources, origin, mekanism):
        for mixin_name in class_names:
            data = None
            for source in sources:
                data = source.read(mixin_name + '.class')
                if data is not None:
                    break
            if data is None:
                continue
            info = parse_class(data, mixin_name)
            for target in info['mixin'] or []:
                for key, access in info['methods'].items():
                    if key[0].startswith('<') or access & (ACC_ABSTRACT | ACC_STATIC | ACC_PRIVATE):
                        continue
                    mixin_methods.setdefault(target, {})[key] = (access, f'{origin} {mixin_name.split("/")[-1]}')
                for iface in info['interfaces']:
                    mixin_ifaces.setdefault(target, []).append(iface)
                    if mekanism:
                        shim_ifaces.add(iface)

    for config_path in args.mixin_config:
        with open(config_path, encoding='utf-8') as f:
            config = json.load(f)
        add_mixins(mixin_classes_of(config), mixin_class_sources, 'Mekanism mixin', True)

    for mod in mods:
        data = mod.read('fabric.mod.json')
        if data is None:
            continue
        mod_json = json.loads(data.decode('utf-8'))
        mod_id = mod_json.get('id', mod.path)
        mod_injected = {}
        for target, ifaces in mod_json.get('custom', {}).get('loom:injected_interfaces', {}).items():
            mod_injected.setdefault(target, []).extend(i.split('<')[0] for i in ifaces)
        tweaker_name = mod_json.get('accessWidener') or mod_json.get('classTweaker')
        if tweaker_name:
            tweaker = mod.read(tweaker_name)
            if tweaker is not None:
                for target, ifaces in parse_class_tweaker(tweaker.decode('utf-8')).items():
                    mod_injected.setdefault(target, []).extend(ifaces)
        for target, ifaces in mod_injected.items():
            for iface in ifaces:
                origins.setdefault(iface, mod_id)
                if iface not in injected.get(target, []):
                    injected.setdefault(target, []).append(iface)
        for config_name in mixin_configs_of(mod_json):
            config_data = mod.read(config_name)
            if config_data is not None:
                add_mixins(mixin_classes_of(json.loads(config_data.decode('utf-8'))), [mod], mod_id, False)

    for iface in shim_ifaces:
        origins.setdefault(iface, 'shim')

    def parse(name):
        """The class with the methods and interfaces mixins add to it."""
        info = raw(name)
        if info is None:
            return None
        if name in mixin_methods or name in mixin_ifaces:
            info = dict(info)
            methods = dict(info['methods'])
            for key, (access, _) in mixin_methods.get(name, {}).items():
                methods.setdefault(key, access)
            info['methods'] = methods
            info['interfaces'] = info['interfaces'] + [i for i in mixin_ifaces.get(name, []) if i not in info['interfaces']]
        return info

    iface_cache = {}

    def interfaces_of(name):
        """All interfaces of a class or interface (including injected ones), transitively."""
        if name in iface_cache:
            return iface_cache[name]
        iface_cache[name] = set()
        info = parse(name)
        result = set()
        direct = list(injected.get(name, []))
        if info is not None:
            direct = info['interfaces'] + direct
            if info['super']:
                result |= interfaces_of(info['super'])
        for iface in direct:
            result.add(iface)
            result |= interfaces_of(iface)
        iface_cache[name] = result
        return result

    def class_methods(name):
        """Instance methods declared in the class or its superclasses (most specific first), excluding private ones (never selected)."""
        methods = {}
        while name:
            info = parse(name)
            if info is None:
                break
            for key, access in info['methods'].items():
                if not access & (ACC_STATIC | ACC_PRIVATE) and not key[0].startswith('<'):
                    methods.setdefault(key, (access, name))
            name = info['super']
        return methods

    def label(iface):
        return f'{iface} ({origins.get(iface, "vanilla")})'

    problems, notes = [], set()
    for target in sorted(set(mc.class_names())):
        info = parse(target)
        if info is None or info['access'] & ACC_INTERFACE:
            continue  # interfaces don't need implementations
        all_ifaces = interfaces_of(target)
        if not all_ifaces & shim_ifaces:
            continue
        declared = {}
        for iface in all_ifaces:
            iface_info = parse(iface)
            if iface_info is None:
                continue
            for key, access in iface_info['methods'].items():
                if access & (ACC_STATIC | ACC_PRIVATE) or key[0].startswith('<'):
                    continue
                declared.setdefault(key, []).append((iface, not access & ACC_ABSTRACT))
        implemented = class_methods(target)
        for key, owners in declared.items():
            defaults = [o for o, is_default in owners if is_default]
            if not any(o in shim_ifaces for o in defaults):
                continue
            if key in implemented:
                access, owner = implemented[key]
                shim_defaults = sorted(o for o in defaults if o in shim_ifaces)
                if access & ACC_ABSTRACT:
                    continue  # subclasses have to implement it
                if not access & ACC_PUBLIC:
                    problems.append(f'{target}: {key[0]}{key[1]} from {", ".join(map(label, shim_defaults))} is selected as the non-public '
                                    f'{owner}#{key[0]} (IllegalAccessError)')
                elif raw(owner) is not None and key in raw(owner)['methods']:
                    notes.add(f'{owner}: {key[0]}{key[1]} from {", ".join(map(label, shim_defaults))} is implemented by vanilla')
                continue
            # The most specific declarations are those not overridden by a subinterface that also declares the method
            owner_names = [o for o, _ in owners]
            maximal = {o for o in owner_names if not any(other != o and o in interfaces_of(other) for other in owner_names)}
            maximal_defaults = sorted(o for o in maximal if o in defaults)
            if len(maximal_defaults) > 1:
                problems.append(f'{target}: {key[0]}{key[1]} from {", ".join(map(label, maximal_defaults))} (IncompatibleClassChangeError)')
            elif not maximal_defaults:
                problems.append(f'{target}: {key[0]}{key[1]} default of {", ".join(map(label, sorted(o for o in defaults if o in shim_ifaces)))} '
                                f'is redeclared abstract by {", ".join(map(label, sorted(maximal)))} (AbstractMethodError)')
    for problem in problems:
        print(problem)
    if args.verbose:
        for note in sorted(notes):
            print(f'note: {note}')
    print(f'{len(problems)} conflicting default methods ({len(injected)} classes with injected interfaces, {len(mods)} mod jars, '
          f'{len(notes)} shim defaults already implemented by vanilla)')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
