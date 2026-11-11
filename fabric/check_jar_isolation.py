"""Checks that the Fabric jars keep Mekanism isolated from other mods.

Other mods can bundle their own NeoForge shim (or run through Sinytra Connector), or bundle the same libraries as we do. Fabric Loader puts all mods on
one class path and keeps only one copy of a nested jar per mod id, so any class, mod id, entrypoint, service, mixin config or datapack folder of ours
that isn't Mekanism-specific can be shadowed by (or shadow) another mod's. This script inspects the built jars, recursively including the nested jars
under META-INF/jars/, and reports (exit code 1):
- class:       a package (or root class) of a Mekanism-owned jar outside OWNED_CLASS_PREFIXES
- nested-jar:  a nested jar that isn't Mekanism-owned and isn't in THIRD_PARTY_MOD_IDS (third-party jars are not inspected further)
- mod-id:      a nested jar without fabric.mod.json
- data:        a datapack folder of a Mekanism-owned jar under a loader named folder (data/<namespace>/neoforge/...), which belongs under the
               mekanism_shim namespace instead
- mixin:       a mixin config, refmap, access widener or class tweaker of a Mekanism-owned jar without a Mekanism-specific name, or a mixin config
               whose package is outside Mekanism's packages
- service:     a META-INF/services file of a Mekanism-owned jar for an interface outside Mekanism's packages
- entrypoint:  a fabric.mod.json entrypoint key of a Mekanism-owned jar that is neither Mekanism-specific nor in FOREIGN_ENTRYPOINTS
A jar is Mekanism-owned if the id in its fabric.mod.json starts with "mekanism" (mekanism, mekanism_all, mekanism_shim, mekanismgenerators...).

Usage: python fabric/check_jar_isolation.py <jar>...   (./gradlew :fabric:checkJarIsolation, part of check, runs it with the built jars)
"""
import io
import json
import sys
import zipfile

# ----------------------------------------------------------------------------------------------------------------------------------------------------
# Allowlists. Edit these.

# Mod id prefix of the jars that are ours (and are inspected in full)
OWNED_MOD_ID_PREFIX = 'mekanism'
# Packages (as class file path prefixes) that Mekanism-owned jars may contain classes in
OWNED_CLASS_PREFIXES = [
    'mekanism/',  # Mekanism, its modules, the integrations, the Fabric glue, and the shim with its shaded libraries (mekanism/shim/)
]
# Name prefix of mixin configs, refmaps, access wideners and class tweakers (file name) and mixin packages (dotted) in Mekanism-owned jars
OWNED_FILE_PREFIX = 'mekanism'
# Third-party mods/libraries that may be nested in our jars, by fabric.mod.json id. They are not inspected further
THIRD_PARTY_MOD_IDS = {
    # Forge Config API Port: NeoForge's config API on Fabric (net.neoforged.neoforge.common.ModConfigSpec...). It is a real Fabric mod with its own
    # id, so Fabric Loader dedupes it against other mods' copies (newest wins) instead of loading two copies. Nests night-config itself
    'forgeconfigapiport',
    # Team Reborn Energy: the de facto Fabric energy API, used for energy interop with other mods. A real Fabric mod, deduped like the above
    'team_reborn_energy',
}
# Entrypoint keys defined by Fabric or other mods that Mekanism-owned jars may use (keys starting with OWNED_MOD_ID_PREFIX are always allowed)
FOREIGN_ENTRYPOINTS = {
    'main', 'client', 'server', 'preLaunch',  # Fabric Loader
    'fabric-datagen', 'fabric-gametest', 'fabric-client-gametest',  # Fabric API
    'jei_mod_plugin',  # JEI
    'jade',  # Jade
    'theoneprobe',  # The One Probe
    'emi',  # EMI
    'modmenu',  # Mod Menu
}
# Loader named datapack folders (data/<namespace>/<folder>/) that Mekanism-owned jars must not contain: the shim's datapack registries live under
# the mekanism_shim namespace
FOREIGN_DATA_FOLDERS = ['neoforge']
# ----------------------------------------------------------------------------------------------------------------------------------------------------

KINDS = ['class', 'nested-jar', 'mod-id', 'data', 'mixin', 'service', 'entrypoint']


class Checker:
    def __init__(self):
        self.offenders = {kind: [] for kind in KINDS}
        self.seen = set()

    def report(self, kind, location, entry):
        # The same jar can be reached through several paths (Mekanism-fabric.jar and its copy nested in the all jar): report it once
        key = (kind, location.rsplit(' > ', 1)[-1], entry)
        if key in self.seen:
            return
        self.seen.add(key)
        self.offenders[kind].append((location, entry))

    def check_jar(self, data, location):
        with zipfile.ZipFile(io.BytesIO(data)) as jar:
            names = jar.namelist()
            try:
                metadata = json.loads(jar.read('fabric.mod.json').decode('utf-8'))
            except KeyError:
                metadata = None
            mod_id = metadata.get('id', '') if metadata else ''
            if not mod_id.startswith(OWNED_MOD_ID_PREFIX):
                # Only reached for nested jars (the top level jars are checked to be ours in main)
                if metadata is None:
                    self.report('mod-id', location, 'nested jar without fabric.mod.json')
                elif mod_id not in THIRD_PARTY_MOD_IDS:
                    self.report('nested-jar', location, mod_id)
                return
            self.check_owned(jar, names, metadata, location)
            for name in names:
                if name.startswith('META-INF/jars/') and name.endswith('.jar'):
                    self.check_jar(jar.read(name), f'{location} > {name.rsplit("/", 1)[-1]}')

    def check_owned(self, jar, names, metadata, location):
        for name in names:
            if name.endswith('.class'):
                path = name
                if path.startswith('META-INF/versions/'):
                    # Multi-release classes: META-INF/versions/<n>/<class path>
                    path = path.split('/', 3)[-1]
                if not any(path.startswith(prefix) for prefix in OWNED_CLASS_PREFIXES):
                    # Report packages rather than every class
                    self.report('class', location, name.rsplit('/', 1)[0] + '/' if '/' in name else name)
            elif name.startswith('data/'):
                parts = name.split('/')
                if len(parts) > 3 and parts[3] and parts[2] in FOREIGN_DATA_FOLDERS:
                    # Report folders (data/<namespace>/neoforge/<registry>/) rather than every file
                    self.report('data', location, '/'.join(parts[:4]) + ('/' if len(parts) > 4 else ''))
            elif name.startswith('META-INF/services/') and not name.endswith('/'):
                if not name[len('META-INF/services/'):].startswith(OWNED_FILE_PREFIX + '.'):
                    self.report('service', location, name)
            elif '/' not in name and (name.endswith(('.mixins.json', '.accesswidener', '.classtweaker')) or 'refmap' in name):
                if not name.startswith(OWNED_FILE_PREFIX):
                    self.report('mixin', location, name)
        # Files referenced by fabric.mod.json (which may live in folders)
        mixins = []
        for mixin in metadata.get('mixins', []):
            mixins.append(mixin['config'] if isinstance(mixin, dict) else mixin)
        for key in ('accessWidener', 'classTweaker'):
            if metadata.get(key):
                self.check_owned_file_name(location, metadata[key])
        for config in mixins:
            self.check_owned_file_name(location, config)
            try:
                config_json = json.loads(jar.read(config).decode('utf-8'))
            except KeyError:
                self.report('mixin', location, f'{config} (missing)')
                continue
            package = config_json.get('package', '')
            if not package.startswith(OWNED_FILE_PREFIX + '.'):
                self.report('mixin', location, f'{config}: package {package}')
            if config_json.get('refmap'):
                self.check_owned_file_name(location, config_json['refmap'])
        for key in metadata.get('entrypoints', {}):
            if key not in FOREIGN_ENTRYPOINTS and not key.startswith(OWNED_MOD_ID_PREFIX):
                self.report('entrypoint', location, key)

    def check_owned_file_name(self, location, path):
        if not path.rsplit('/', 1)[-1].startswith(OWNED_FILE_PREFIX):
            self.report('mixin', location, path)


def main(paths):
    if not paths:
        print(__doc__)
        return 2
    checker = Checker()
    for path in paths:
        with open(path, 'rb') as file:
            data = file.read()
        location = path.replace('\\', '/').rsplit('/', 1)[-1]
        with zipfile.ZipFile(io.BytesIO(data)) as jar:
            try:
                mod_id = json.loads(jar.read('fabric.mod.json').decode('utf-8')).get('id', '')
            except KeyError:
                mod_id = ''
        if not mod_id.startswith(OWNED_MOD_ID_PREFIX):
            print(f'{location}: not a Mekanism jar (mod id "{mod_id}")')
            return 2
        checker.check_jar(data, location)

    total = sum(len(entries) for entries in checker.offenders.values())
    if total == 0:
        print('The Fabric jars are isolated: no foreign classes, nested jars, data folders, mixin names, services or entrypoints')
        return 0
    print(f'Found {total} entries that break the isolation of the Fabric jars from other mods (see {__file__}):')
    for kind in KINDS:
        entries = checker.offenders[kind]
        if entries:
            print(f'[{kind}] ({len(entries)})')
            for location, entry in entries:
                print(f'  {location}: {entry}')
    return 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
