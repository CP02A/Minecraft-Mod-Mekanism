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
    `BlockStateModelBridge` (NeoForge custom blockstate models), `ModelPropertiesBridge` (NeoForge's additional model properties: root transform,
    part visibility; passed to geometries by `bakeTopGeometry` callers), NeoForge's `neoforge:fluid_container` (with its unlit render pipelines and
    `item_unlit` shader) and `neoforge:trimmed_armor` item models (registered by `ClientShimLoader`),
    `ClientGameEventBridge` (client tick, entity join, recipe sync, HUD layer, custom geometry and menu screen events), `BlockOutlineBridge`
    (custom block outline renderers, tracked by identity as vanilla's `BlockOutlineRenderState` can't hold them), `LevelAwareBlockStateModel`
    (added to `BlockStateModel`: Fabric's `emitQuads` uses NeoForge's level aware `collectParts`, i.e. model data).
  - `RecipeContentSync` (NeoForge's `OnDatapackSyncEvent#sendRecipes` / `RecipesReceivedEvent`), `AdvancedMenuOpening` (menu extra data from
    `IMenuProviderExtension#writeClientSideData` passed to `IContainerFactory`).
  - `mekanism/fabric/mixin/`: `data/` (NeoForge ingredient/holder set/conditions formats), `hooks/` (BlockEntity#onLoad, tick, damage,
    fall, jump, invulnerability, attributes, break speed, crafting events, menu opening, modded recipe book type settings incl. saving/syncing,
    slot backgrounds, entity/passenger tick, forced chunk persistence, chunk ticket level events), `client/` (level load/unload, login/logout/clone,
    mouse scroll, fog, first person arm, sound engine/play sound, block outline extraction, NeoForge's model `loader` key in Fabric's unbaked model
    deserializer, model dependency discovery, model baking/atlas stitched/fluid model/particle provider/block tint events, recipe book search
    categories and result indexing, json model `transform`/`visibility`/`neoforge_data` keys and Transformation Gson adapter, NeoForge's water tint
    source), and generated `shim/`
    (interface injection + enum extensions).
- `fabric/gen_fabric_glue.py <merged MC jar>`: regenerates the access widener, injected interfaces and shim mixins. Re-run after changing ATs
  (including `fabric/fabric-accesstransformer.cfg`), `shim-injected-interfaces.json` or `enumextensions.json`.
- `fabric/check_injected_interfaces.py <merged MC jar> shim/build/classes/java/main`: finds conflicting injected default methods.

## Verification
- `./gradlew :neoforge:build :neoforge:runGameTestServer` (72 tests)
- `./gradlew :fabric:runGameTestServer` (Fabric game tests in `fabric/src/gametest`: transfer bridge, machine processing, ore gen, Team Reborn
  energy and item capability interop, forced chunk persistence, ticket level/passenger tick events)
- `./gradlew :fabric:runGameTestClient` (opens a client, loads a world, checks model/atlas/fluid model events, modded recipe book settings
  codecs and that recipes reached the client, opens a machine GUI, clicks its side config tab and a side button (checked on the server) and
  opens Mekanism's recipe book, renders filled fluid tanks and buckets in hand/hotbar, connects transmitters (connections checked against the
  server), checks the wind generator's composite model and Tools' trimmed armor item models; screenshots in `fabric/runs/gametestclient/screenshots`)
- `./gradlew :fabric:runServer` (dedicated server; `fabric/runs/server/eula.txt` is accepted locally)

## Working
Server boots with all Mekanism content; recipes/tags/conditions/data maps/biome modifiers load; machines process recipes; item/fluid
interop with Fabric Transfer API (blocks and items, both directions); energy interop with Team Reborn Energy (blocks and items, both directions,
1 NeoForge energy unit = 1 TR energy unit, bundled jar-in-jar); forced chunks of ticket controllers (Mekanism's chunk loaders) are persisted and
validated on load; ChunkTicketLevelUpdatedEvent, RegisterSpawnPlacementsEvent and server/passenger EntityTickEvents fire; client starts, joins a
world and renders Mekanism blocks (incl. custom energy cube model, NeoForge composite/OBJ model loaders, model data and part visibility, i.e.
connected transmitters); fluid tank and bucket items show their fluid; model baking, atlas
stitched, fluid model, particle provider, block tint and render pipeline registration events; machine GUIs open (with NeoForge menu extra data),
render and are interactive (side config changes reach the server); Mekanism's recipe book with its search category, modded recipe book settings
saved/synced; recipes are synced to the client; the dedicated server boots.

## Not done yet (rough priority)
1. Client: RenderLevelStageEvent, ScreenEvent and tooltip events (RenderTooltipEvent, ItemTooltipEvent, ...) are not fired (Mekanism doesn't
   subscribe to them). Vanilla's `AbstractContainerScreen#extractSlot` doesn't call `renderSlotContents` (Mekanism's screens call it themselves).
   Model data is read from the block entity on the meshing thread (no `ModelDataManager` snapshot); `neoforge:conditional` model loader not
   ported. Respawn (Clone) and fog/arm events are wired but not checked by tests; JEI/EMI absent.
2. Model leftovers: item layer face data (`neoforge_data`) is parsed but vanilla's `ItemModelGenerator` doesn't apply it; vanilla's
   `BakedQuad.MaterialInfo` has no ambient occlusion flag; other vanilla `bakeTopGeometry` callers (missing model) don't pass properties.
3. Interop gaps: Mekanism chemicals have no Fabric equivalent (not exposed); `ItemAccess` can't distinguish a container item's main slot from
   overflow, so Fabric `ContainerItemContext` main slot insertions of a matching item may overflow; energy amounts are saturated to `int` per
   operation (NeoForge's `EnergyHandler`); MobSpawnEvent.SpawnPlacementCheck is not fired.
4. Client side EntityTickEvent for non-passengers (ClientLevel#tickNonPassenger). Forced chunk persistence is tested via a codec round trip, not
   via an actual server restart with a chunk loader.
5. Secondary modules: Generators, Additions and Tools are separate Fabric mods (`generators`/`additions`/`tools` source sets in `:fabric`,
   compiled from `neoforge/src/<module>` with Fabric entrypoints, `fabric.mod.json` and per module overrides in `fabric/src/<module>`, own jars via
   `<module>Jar`), loaded in all runs and covered by game tests (generators produce energy, fusion/fission/turbine multiblocks form, baby mobs,
   plastic fence gate, paxel, FinalizeSpawnEvent; client test renders generators and opens a generator GUI). Left: Additions' baby mob biome/structure
   spawn modifiers aren't applied (BiomeModifierBridge only supports NeoForge's feature modifiers); FinalizeSpawnEvent spawn cancellation; beacon
   colors, block friction and reequip animation extension hooks aren't called by vanilla. No defense module exists in this
   repo; integrations not started; datagen stays NeoForge-only.
6. Move shared sources from `neoforge/src` into `common/`; update docs/multiloader.md.
