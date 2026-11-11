# Multiloader layout

Mekanism is split into Gradle subprojects following the
[MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) approach, except that the shared code is
not a Gradle project of its own:

| Directory   | Compiles against                                     | Contents                                                                                          |
|-------------|------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| `common/`   | (not compiled on its own)                            | Shared sources and resources (`common/src`), including the generated data                         |
| `neoforge/` | NeoForge (ModDevGradle)                              | NeoForge specific code and resources, datagen, integrations, unit and game tests                  |
| `shim/`     | Vanilla Minecraft only (NeoForm)                     | Fabric implementation of the subset of NeoForge's API that the shared code uses                   |
| `fabric/`   | Fabric Loader + Fabric API (Loom) + `shim`           | Fabric entrypoints, mixins and Fabric implementations of the platform services and shim backends  |

`common/src` mirrors the source sets of the loader projects (`api`, `main`, `additions`, `generators`, `tools`, each with
`java` and `resources`, plus `datagen/generated`). It is compiled by each loader project:

- `:neoforge` adds the `common/src/<source set>` directories to its own source sets, so IDEs and the NeoForge build see
  them as regular sources. `:neoforge:runData` writes the generated data to `common/src/datagen/generated`.
- `:fabric` copies the shared sources into its build directory (`syncSharedSources`, `sync<Module>Sources`) and compiles
  them against the shim, and adds the shared resources and generated data in its `processResources` tasks. The sources
  are copied rather than added as source directories, as IntelliJ doesn't allow two modules to share a content root.

## Writing shared code

Shared code is written against NeoForge's API (`net.neoforged.*`). On Fabric, the shim provides the parts of it that
Mekanism uses, so shared code may only use NeoForge APIs that the shim implements; the Fabric compile fails otherwise.
NeoForge's patches to vanilla classes (added constructors, methods and interfaces) are only available where the shim
injects or reimplements them (see `docs/fabric-port-status.md`).

Anything else that differs between the loaders goes into one of:

- A platform service: an interface in `common/src/main/java/mekanism/common/platform/services` (or
  `mekanism.client.platform` for client only functionality), obtained via `mekanism.common.platform.Services` (or
  `ClientServices`). Each loader project provides an implementation, registered in `META-INF/services/<interface name>`:
  - NeoForge: `neoforge/src/main/java/mekanism/{common,client}/platform/neoforge`
  - Fabric: `fabric/src/main/java/mekanism/{common,client}/platform/fabric`

  To add a new service, add the interface to `common/src`, implement it in both loader projects, register the
  implementations in each project's service file, and expose it as a constant in `Services`.
- A loader specific class: a class with the same name in both `neoforge/src/<source set>/java` and
  `fabric/src/<source set>/java` (never in `common/src`), for example
  `mekanism.client.gui.recipe_book.VirtualRecipeBookComponent` and
  `mekanism.additions.common.block.plastic.BlockPlasticFenceGate`. Keep these small, as both versions need to be kept in
  sync by hand.

NeoForge only files stay in `neoforge/src`: `neoforge.mods.toml`, the access transformers (also the input of
`fabric/gen_fabric_glue.py`), `enumextensions.json`, the datagen API (`mekanism.api.datagen`) and data generators,
integrations and tests.

## Common commands

- `./gradlew :neoforge:build` — the full NeoForge build
- `./gradlew :fabric:build` — the Fabric jars (including `:fabric:checkInjectedInterfaces`)
- `./gradlew :fabric:runClient` / `:fabric:runServer` / `:fabric:runGameTestServer` / `:fabric:runGameTestClient` — Fabric dev runs and game tests
- `./gradlew :neoforge:runClient`, `:neoforge:runData`, `:neoforge:runGameTestServer` — NeoForge runs
