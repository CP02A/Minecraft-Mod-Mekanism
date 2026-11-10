# Fabric port status (branch `multiloader-fabric`)

## Architecture
- `shim/`: Fabric-only implementation of the NeoForge APIs Mekanism uses (`net.neoforged.*`, copied from NeoForge, LGPL-2.1).
  Loader specifics go through ServiceLoader backends in `net.neoforged.neoforge.shim` (`ShimServices`).
- `fabric/`: compiles `neoforge/src/{api,main}/java` against the shim (Sync task, with per-file overrides listed in `fabricOverrides`).
  - `mekanism/fabric/shim/`: backends (transactions, registries, network, attachments, data maps, biome modifiers),
    `ShimModLoader` (constructs mods from the `neoforge-shim` / `neoforge-shim-client` entrypoints and fires the mod bus events),
    `GameEventBridge` (Fabric events -> NeoForge game events), `ServerHooks` (server side hooks called from mixins), `transfer/` (NeoForge
    item/fluid/energy block and item capabilities <-> Fabric Transfer API and Team Reborn Energy, `ItemAccess` <-> `ContainerItemContext`).
  - `mekanism/fabric/shim/client/`: `ClientShimLoader` (client registration events -> Fabric client APIs), `FabricClientBackend`,
    `BlockStateModelBridge` (NeoForge custom blockstate models), `FluidContainerItemModel` (simplified `neoforge:fluid_container`),
    `ClientGameEventBridge` (client tick, entity join, recipe sync, HUD layer, custom geometry and menu screen events), `BlockOutlineBridge`
    (custom block outline renderers, tracked by identity as vanilla's `BlockOutlineRenderState` can't hold them).
  - `RecipeContentSync` (NeoForge's `OnDatapackSyncEvent#sendRecipes` / `RecipesReceivedEvent`), `AdvancedMenuOpening` (menu extra data from
    `IMenuProviderExtension#writeClientSideData` passed to `IContainerFactory`).
  - `mekanism/fabric/mixin/`: `data/` (NeoForge ingredient/holder set/conditions formats), `hooks/` (BlockEntity#onLoad, tick, damage,
    fall, jump, invulnerability, attributes, break speed, crafting events, menu opening, modded recipe book type settings, entity/passenger tick,
    forced chunk persistence, chunk ticket level events), `client/` (level load/unload,
    login/logout/clone, mouse scroll, fog, first person arm, sound engine/play sound, block outline extraction), and generated `shim/`
    (interface injection + enum extensions).
- `fabric/gen_fabric_glue.py <merged MC jar>`: regenerates the access widener, injected interfaces and shim mixins. Re-run after changing ATs
  (including `fabric/fabric-accesstransformer.cfg`), `shim-injected-interfaces.json` or `enumextensions.json`.
- `fabric/check_injected_interfaces.py <merged MC jar> shim/build/classes/java/main`: finds conflicting injected default methods.

## Verification
- `./gradlew :neoforge:build :neoforge:runGameTestServer` (72 tests)
- `./gradlew :fabric:runGameTestServer` (Fabric game tests in `fabric/src/gametest`: transfer bridge, machine processing, ore gen, Team Reborn
  energy and item capability interop, forced chunk persistence, ticket level/passenger tick events)
- `./gradlew :fabric:runGameTestClient` (opens a client, loads a world, checks recipes reached the client, opens a machine GUI; screenshots in
  `fabric/runs/gametestclient/screenshots`)
- `./gradlew :fabric:runServer` (dedicated server; `fabric/runs/server/eula.txt` is accepted locally)

## Working
Server boots with all Mekanism content; recipes/tags/conditions/data maps/biome modifiers load; machines process recipes; item/fluid
interop with Fabric Transfer API (blocks and items, both directions); energy interop with Team Reborn Energy (blocks and items, both directions,
1 NeoForge energy unit = 1 TR energy unit, bundled jar-in-jar); forced chunks of ticket controllers (Mekanism's chunk loaders) are persisted and
validated on load; ChunkTicketLevelUpdatedEvent, RegisterSpawnPlacementsEvent and server/passenger EntityTickEvents fire; client starts, joins a world and renders Mekanism blocks (incl. custom energy cube model); machine GUIs open
(with NeoForge menu extra data) and render; recipes are synced to the client; the dedicated server boots.

## Not done yet (rough priority)
1. Client: GUI interaction beyond opening (side config, recipe book, JEI/EMI absent), RenderLevelStage, ScreenEvent/tooltip events,
   ModelEvent.BakingCompleted/ModifyBakingResult, ModelEvent.RegisterLoaders (Fabric UnbakedModelDeserializer), RegisterFluidModelsEvent,
   TextureAtlasStitchedEvent, render pipelines, recipe book search categories, model data in Fabric's emitQuads, slot backgrounds,
   renderSlotContents. Modded recipe book type settings are not saved/synced (vanilla codecs). Respawn (Clone) and fog/arm events are wired but
   not exercised by tests.
2. Real fluid rendering in `neoforge:fluid_container` item models (needs NeoForge's unlit render types).
3. Interop gaps: Mekanism chemicals have no Fabric equivalent (not exposed); `ItemAccess` can't distinguish a container item's main slot from
   overflow, so Fabric `ContainerItemContext` main slot insertions of a matching item may overflow; energy amounts are saturated to `int` per
   operation (NeoForge's `EnergyHandler`); MobSpawnEvent.SpawnPlacementCheck is not fired.
4. Client side EntityTickEvent for non-passengers (ClientLevel#tickNonPassenger). Forced chunk persistence is tested via a codec round trip, not
   via an actual server restart with a chunk loader.
5. Secondary modules (generators, additions, tools, defense) and integrations; datagen stays NeoForge-only.
6. Move shared sources from `neoforge/src` into `common/`; update docs/multiloader.md.
