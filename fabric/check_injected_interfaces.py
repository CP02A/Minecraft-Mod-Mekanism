"""Checks the shim's injected interfaces for default methods that conflict at runtime.

Interface injection makes vanilla classes implement the shim's extension interfaces. If a class ends up implementing two unrelated injected interfaces
(directly or through its superclasses and interfaces) that both declare a default method with the same name and descriptor, and the class doesn't
override the method itself, the JVM throws an IncompatibleClassChangeError when the method is invoked. The compiler doesn't catch this, as the vanilla
classes are not compiled against the injected interfaces.

Usage: python fabric/check_injected_interfaces.py <merged Minecraft jar (Mojang names)> <shim classes directory or jar>
"""
import json
import os
import struct
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INJECTED = os.path.join(REPO, 'shim', 'src', 'main', 'resources', 'META-INF', 'shim-injected-interfaces.json')

mc_jar = zipfile.ZipFile(sys.argv[1])
shim_path = sys.argv[2]
shim_jar = zipfile.ZipFile(shim_path) if shim_path.endswith('.jar') else None


def read_bytes(name):
    try:
        return mc_jar.read(name + '.class')
    except KeyError:
        pass
    if shim_jar is not None:
        try:
            return shim_jar.read(name + '.class')
        except KeyError:
            return None
    path = os.path.join(shim_path, name + '.class')
    if os.path.exists(path):
        return open(path, 'rb').read()
    return None


_cache = {}


def parse(name):
    """Returns (access, super name, [interfaces], {(method name, descriptor): access}) or None."""
    if name in _cache:
        return _cache[name]
    data = read_bytes(name)
    if data is None:
        _cache[name] = None
        return None
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
    for _member_kind in ('fields', 'methods'):
        member_count = struct.unpack_from('>H', data, pos)[0]
        pos += 2
        members = {}
        for _ in range(member_count):
            m_access, name_idx, desc_idx, attr_count = struct.unpack_from('>HHHH', data, pos)
            pos += 8
            members[(utf8[name_idx], utf8[desc_idx])] = m_access
            for _ in range(attr_count):
                length = struct.unpack_from('>I', data, pos + 2)[0]
                pos += 6 + length
        if _member_kind == 'methods':
            methods = members
    _cache[name] = (access, super_name, interfaces, methods)
    return _cache[name]


injected = {target: [i.split('<')[0] for i in ifaces] for target, ifaces in json.load(open(INJECTED)).items()}


_iface_cache = {}


def interfaces_of(name):
    """All interfaces of a class or interface (including injected ones), transitively."""
    if name in _iface_cache:
        return _iface_cache[name]
    _iface_cache[name] = set()
    info = parse(name)
    result = set()
    direct = list(injected.get(name, []))
    if info is not None:
        direct = info[2] + direct
        if info[1]:
            result |= interfaces_of(info[1])
    for iface in direct:
        result.add(iface)
        result |= interfaces_of(iface)
    _iface_cache[name] = result
    return result


def concrete_methods(name):
    """Methods declared (non-abstract) in the class or its superclasses."""
    methods = set()
    while name:
        info = parse(name)
        if info is None:
            break
        for key, access in info[3].items():
            if not access & 0x0400:
                methods.add(key)
        name = info[1]
    return methods


def is_subinterface(sub, sup):
    return sup in interfaces_of(sub)


shim_ifaces = {i for ifaces in injected.values() for i in ifaces}
problems = []
#Check every class of the game (and the shim), as classes inherit the injected interfaces of their supertypes
candidates = [n[:-6] for n in mc_jar.namelist() if n.endswith('.class')]
for target in sorted(candidates):
    info = parse(target)
    if info is None:
        continue
    if info[0] & 0x0200:
        continue  # interfaces don't need implementations
    all_ifaces = interfaces_of(target)
    if len(all_ifaces & shim_ifaces) < 2:
        continue
    defaults = {}
    for iface in all_ifaces:
        iface_info = parse(iface)
        if iface_info is None:
            continue
        for key, access in iface_info[3].items():
            if access & 0x0400 or access & 0x0008 or key[0].startswith('<'):
                continue  # abstract, static or initializer
            defaults.setdefault(key, []).append(iface)
    implemented = concrete_methods(target)
    for key, owners in defaults.items():
        if len(owners) < 2 or key in implemented:
            continue
        # The most specific defaults are those not overridden by a subinterface that also has the default
        maximal = [o for o in owners if not any(other != o and is_subinterface(other, o) for other in owners)]
        if len(maximal) > 1 and any(o in shim_ifaces for o in maximal):
            problems.append(f'{target}: {key[0]}{key[1]} from {", ".join(sorted(set(maximal)))}')
for problem in problems:
    print(problem)
print(f'{len(problems)} conflicting default methods')
sys.exit(1 if problems else 0)
