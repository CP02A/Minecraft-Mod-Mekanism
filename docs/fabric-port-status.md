# Fabric port status (branch `multiloader-fabric`)

## Architecture
- `shim/`: Fabric-only implementation of the NeoForge APIs Mekanism uses (`net.neoforged.*`, copied from NeoForge, LGPL-2.1).
  Loader specifics go through ServiceLoader backends in `net.neoforged.neoforge.shim` (`ShimServices`).
- `fabric/`: compiles `neoforge/src/{api,main}/java` against the shim (Sync task, with per-file overrides listed in `fabricOverrides`).
  - `mekanism/fabric/shim/`: backends (transactions, registries, network, attachments, data maps, biome modifiers),
    `ShimModLoader` (constructs mods from the `neoforge-shim` / `neoforge-shim-client` entrypoints and fires the mod bus events),
    `GameEventBridge` (Fabric events -> NeoForge game events), `transfer/` (NeoForge capabilities <-> Fabric Transfer API).
  - `mekanism/fabric/shim/client/`: `ClientShimLoader` (client registration events -> Fabric client APIs), `FabricClientBackend`,
    `BlockStateModelBridge` (NeoForge custom blockstate models), `FluidContainerItemModel` (simplified `neoforge:fluid_container`).
  - `mekanism/fabric/mixin/`: `data/` (NeoForge ingredient/holder set/conditions formats), `hooks/` (BlockEntity#onLoad, tick, damage,
    fall, jump, invulnerability, attributes, break speed, crafting events), `client/`, and generated `shim/` (interface injection + enum extensions).
- `fabric/gen_fabric_glue.py <merged MC jar>`: regenerates the access widener, injected interfaces and shim mixins. Re-run after changing ATs
  (including `fabric/fabric-accesstransformer.cfg`), `shim-injected-interfaces.json` or `enumextensions.json`.
- `fabric/check_injected_interfaces.py <merged MC jar> shim/build/classes/java/main`: finds conflicting injected default methods.

## Verification
- `./gradlew :neoforge:build :neoforge:runGameTestServer` (72 tests)
- `./gradlew :fabric:runGameTestServer` (Fabric game tests in `fabric/src/gametest`: transfer bridge, machine processing, ore gen)
- `./gradlew :fabric:runGameTestClient` (opens a client, loads a world, screenshots in `fabric/runs/gametestclient/screenshots`)

## Working
Server boots with all Mekanism content; recipes/tags/conditions/data maps/biome modifiers load; machines process recipes; item/fluid
interop with Fabric Transfer API; client starts, joins a world and renders Mekanism blocks (incl. custom energy cube model).

## Not done yet (rough priority)
1. Client: GUIs/screens untested; client game events (ClientTickEvent, ClientPlayerNetworkEvent, RenderGuiLayerEvent, input/mouse,
   RenderLevelStage/SubmitCustomGeometry, fog, sound, tooltips, RecipesReceivedEvent), ModelEvent.BakingCompleted/ModifyBakingResult,
   ModelEvent.RegisterLoaders (Fabric UnbakedModelDeserializer), RegisterFluidModelsEvent, TextureAtlasStitchedEvent, render pipelines,
   recipe book search categories, SoundEngineLoadEvent, model data in Fabric's emitQuads, slot backgrounds, renderSlotContents.
2. Real fluid rendering in `neoforge:fluid_container` item models (needs NeoForge's unlit render types).
3. Energy interop with other Fabric mods (Team Reborn Energy); item/fluid *item* capabilities (ContainerItemContext bridge).
4. Forced chunk persistence (ForcedChunkManager hooks), ChunkTicketLevelUpdatedEvent, spawn placements, entity tick for passengers/client.
5. Secondary modules (generators, additions, tools, defense) and integrations; datagen stays NeoForge-only.
6. Move shared sources from `neoforge/src` into `common/`; update docs/multiloader.md.
