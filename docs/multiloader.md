# Multiloader layout

Mekanism is split into Gradle subprojects following the
[MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) approach:

| Project     | Compiles against                          | Contents                                                                |
|-------------|-------------------------------------------|-------------------------------------------------------------------------|
| `common`    | Vanilla Minecraft only (NeoForm)          | Loader agnostic code and resources                                      |
| `neoforge`  | NeoForge (ModDevGradle)                   | Everything that has not been migrated yet (all modules, datagen, tests) |
| `fabric`    | Fabric Loader + Fabric API (Loom)         | Fabric entrypoints and Fabric implementations of the platform services  |

`common` is not shipped as its own jar. Its sources and resources are compiled directly into the
`neoforge` main module and into the `fabric` jar.

## Platform services

Code in `common` must not reference NeoForge or Fabric classes. Instead, it accesses loader specific
functionality through the interfaces in `mekanism.common.platform.services`, which are obtained via
`mekanism.common.platform.Services`. Each loader project provides an implementation, registered in
`META-INF/services/<interface name>`:

- NeoForge: `neoforge/src/main/java/mekanism/common/platform/neoforge`
- Fabric: `fabric/src/main/java/mekanism/common/platform/fabric`

To add a new service, add the interface to `common`, implement it in both loader projects, register the
implementations in each project's service file, and expose it as a constant in `Services`.

## Current status

Only the skeleton exists: `MekanismCommon.init()` runs on both loaders, and the Fabric jar contains
no content yet. Suggested migration order:

1. Registration (a common `DeferredRegister`-like abstraction backed by NeoForge registries / vanilla `Registry.register` on Fabric)
2. Transfer: items/fluids (NeoForge transfer API vs Fabric Transfer API), energy (NeoForge energy vs Team Reborn Energy), chemicals
3. Config, networking (payloads), data attachments
4. Client: rendering, models, GUIs, key bindings
5. Machines and multiblocks, then the additions/generators/tools modules
6. Integrations (each needs a Fabric build of the target mod, or stays NeoForge only)

## Common commands

- `./gradlew :neoforge:build` — the full NeoForge build, unchanged from before the split
- `./gradlew :fabric:build` — the Fabric jar
- `./gradlew :fabric:runClient` / `:fabric:runServer` — Fabric dev runs
- `./gradlew :neoforge:runClient`, `:neoforge:runData`, `:neoforge:runGameTestServer` — NeoForge runs
