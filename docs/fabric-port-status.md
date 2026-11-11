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
    (added to `BlockStateModel`: Fabric's `emitQuads` uses NeoForge's level aware `collectParts`, i.e. model data), NeoForge's `ModelDataManager`
    (shim; per client level, snapshotted per meshing region by `RenderRegionCacheMixin`), `ItemLayerFaceData` (item layer `neoforge_data` light
    emission for generated item models).
  - `RecipeContentSync` (NeoForge's `OnDatapackSyncEvent#sendRecipes` / `RecipesReceivedEvent`), `AdvancedMenuOpening` (menu extra data from
    `IMenuProviderExtension#writeClientSideData` passed to `IContainerFactory`).
  - `mekanism/fabric/mixin/`: `data/` (NeoForge ingredient/holder set/conditions formats), `hooks/` (BlockEntity#onLoad, capability
    invalidation and model data refresh on block entity placement/removal, tick, damage,
    fall, jump, invulnerability, attributes, break speed, crafting events, menu opening, modded recipe book type settings incl. saving/syncing,
    slot backgrounds, entity/passenger tick, forced chunk persistence, chunk ticket level events, datapack registry bake callbacks on freeze,
    value based `TypedInstance#is(Holder)`, `onItemUseFirst`/`doesSneakBypassUse` and `onDestroyedByPlayer` in `ServerPlayerGameMode` and
    `MultiPlayerGameMode` (`ItemUseHooks`)), `client/` (level load/unload, login/logout/clone,
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
  energy and item capability interop, universal cables/logistical transporters/mechanical pipes connecting to cubes, machines, tanks and vanilla
  chests placed after them (and disconnecting on removal), energy flowing through cables, forced chunk persistence, ticket level/passenger tick events,
  baby mobs in biome and structure spawn lists, FinalizeSpawnEvent spawn cancellation, SpawnPlacementCheck, plastic beacon beam colors, adding to
  existing creative tabs, tooltip appenders, grindstone merging; `GameplayTests`: dynamic tank, induction matrix charge/discharge, thermal
  evaporation, boiler and SPS forming, chemicals through pressurized tubes, digital miner mining, QIO importer into drives, teleporter, cardboard
  box, configurator wrench/rotate, breaking bounding blocks, block entity save/load and drop/place persistence, configs loaded)
- `./gradlew :fabric:runGameTestClient` (opens a client, loads a world, checks model/atlas/fluid model events, modded recipe book settings
  codecs and that recipes reached the client, opens a machine GUI, clicks its side config tab and a side button (checked on the server) and
  opens Mekanism's recipe book, renders filled fluid tanks and buckets in hand/hotbar, emissive item layers, connects transmitters to each other and
  to energy cubes (connections checked against the server, model data snapshot checked against the ModelDataManager), checks the wind generator's composite model and Tools' trimmed armor item models,
  Mekanism's armor layer (EntityRenderersEvent.AddLayers), the tooltip lines of a charged energy cube and a filled fluid tank and screenshots a hovered
  tooltip in the inventory, opens the portable teleporter's GUI and sets a frequency from it (checked on the server and the synced item);
  screenshots in `fabric/runs/gametestclient/screenshots`)
- `./gradlew :fabric:runServer` (dedicated server; `fabric/runs/server/eula.txt` is accepted locally)

## Working
Server boots with all Mekanism content; recipes/tags/conditions/data maps/biome modifiers load; machines process recipes; item/fluid
interop with Fabric Transfer API (blocks and items, both directions); energy interop with Team Reborn Energy (blocks and items, both directions,
1 NeoForge energy unit = 1 TR energy unit, bundled jar-in-jar); forced chunks of ticket controllers (Mekanism's chunk loaders) are persisted and
validated on load; ChunkTicketLevelUpdatedEvent, RegisterSpawnPlacementsEvent and server/passenger EntityTickEvents fire; all NeoForge biome
modifier changes except climate/special effects (spawns, spawn costs, features, carvers, attributes; `BiomeModifierBridge`, server configs are loaded
before Fabric's biome modification) and structure modifiers (`StructureMixin`) apply; FinalizeSpawnEvent spawn cancellation, SpawnPlacementCheck,
block friction (incl. leashed entity angular friction), beacon colors, `isDamageable` (repair recipe and grindstone),
`shouldCauseReequipAnimation` (via `FabricItem` when the item stays the same, `FirstPersonHandsAndItemsMixin` when it changes),
`shouldCauseBlockBreakReset` and BuildCreativeModeTabContentsEvent are honored; NeoForge's tooltip appenders (RegisterTooltipAppendersEvent, i.e.
Mekanism's data component tooltips) run in `ItemStack#addDetailsToTooltip` (shim `ItemTooltipHandler` + `ItemStackTooltipMixin`; vanilla's own
component tooltips stay in place and the modded ones run after the vanilla component NeoForge's sort puts them behind); EntityRenderersEvent.AddLayers
fires; client starts, joins a
world and renders Mekanism blocks (incl. custom energy cube model, NeoForge composite/OBJ model loaders, model data and part visibility, i.e.
connected transmitters); fluid tank and bucket items show their fluid; model baking, atlas
stitched, fluid model, particle provider, block tint and render pipeline registration events; machine GUIs open (with NeoForge menu extra data),
render and are interactive (side config changes reach the server); Mekanism's recipe book with its search category, modded recipe book settings
saved/synced; recipes are synced to the client; the dedicated server boots. Gameplay covered by `GameplayTests`: main module multiblocks form and
work, chemicals flow through tubes (chemical registry bake callback), digital miner, QIO, teleporters (`TypedInstance#is(Holder)` with registry
objects), cardboard box and configurator (`onItemUseFirst`/`doesSneakBypassUse`), multi-block machines drop when a bounding block is broken
(`onDestroyedByPlayer`), block entity and item persistence, configs; an item-held GUI (portable teleporter) and its frequency packet.

## Not done yet (rough priority)
1. Client: RenderLevelStageEvent, ScreenEvent and tooltip events (RenderTooltipEvent, ItemTooltipEvent, ...) are not fired (Mekanism doesn't
   subscribe to them). Vanilla's `AbstractContainerScreen#extractSlot` doesn't call `renderSlotContents` (Mekanism's screens call it themselves).
   `neoforge:conditional` model loader not ported. Respawn (Clone) and fog/arm events are wired but not checked by tests; JEI/EMI absent.
2. Model leftovers: only the light emission of NeoForge's `ExtraFaceData` is applied (item layers and `UnbakedElementsHelper`; vanilla's face baking
   has no color/per face light/ambient occlusion support); vanilla's `BakedQuad.MaterialInfo` has no ambient occlusion flag.
3. Interop gaps: Mekanism chemicals have no Fabric equivalent (not exposed); `ItemAccess` can't distinguish a container item's main slot from
   overflow, so Fabric `ContainerItemContext` main slot insertions of a matching item may overflow; energy amounts are saturated to `int` per
   operation (NeoForge's `EnergyHandler`).
4. Client side EntityTickEvent for non-passengers (ClientLevel#tickNonPassenger). Forced chunk persistence is tested via a codec round trip, not
   via an actual server restart with a chunk loader.
5. Secondary modules: Generators, Additions and Tools are separate Fabric mods (`generators`/`additions`/`tools` source sets in `:fabric`,
   compiled from `neoforge/src/<module>` with Fabric entrypoints, `fabric.mod.json` and per module overrides in `fabric/src/<module>`, own jars via
   `<module>Jar`), loaded in all runs and covered by game tests (generators produce energy, fusion/fission/turbine multiblocks form, baby mobs,
   plastic fence gate, paxel, FinalizeSpawnEvent, baby mob spawn modifiers, plastic beacon colors; client test renders generators and opens a
   generator GUI). No defense module exists in this repo; datagen stays NeoForge-only.
6. Remaining server side gaps (events/extension hooks Mekanism uses that the shim doesn't fire/call):
   - MobSpawnEvent.PositionCheck is not fired (Mekanism doesn't use it); FinalizeSpawnEvent has no spawner and fires inside `Mob#finalizeSpawn`;
     spawn cancellation is stored in the event (no `Mob#setSpawnCancelled`).
   - Vanilla's attribute tooltips are used instead of NeoForge's `AttributeUtil` ones. Injected default methods that conflict with Fabric API's
     injected interfaces aren't found by `check_injected_interfaces.py` (`TooltipFlag#shouldDisplayAllInformation` is resolved by
     `TooltipFlagDefaultMixin`).
   - Biome modifier climate/special effect changes and attribute removals aren't applied (warned once); direct (unregistered) features/carvers
     can't be added through Fabric's API.
   - Level aware `IBlockExtension` hooks without a vanilla call site: `getExplosionResistance` (Mekanism's custom resistances),
     `getLightEmission` (fluid tanks with glowing fluids), `getCloneItemStack` (pick block on bounding blocks), `onBlockExploded`, `getExpDrop`.
     Datapack registries only get the bake callbacks of their `RegistryBuilder` configurator (no default key, add/clear callbacks).
   - Already listed as not supported in the shim: tool modification events, enchantment level event, block drops experience, custom permission
     handlers, explosion events, BooleanAttribute/PercentageAttribute tooltip formatting.
7. Integrations: the integration source sets (`neoforge/src/integration/*`: computer_craft, crafttweaker, curios, emi, framedblocks, jade, jei,
   nerb, open_computers, projecte, recipe_stages, the_one_probe, wthit) aren't compiled for Fabric. Main code only reaches them through optional
   services (`ICuriosHelper`, `IComputerCraftHelper`, `IOpenComputersHelper`, `IRecipeStagesHelper`, JEI/EMI helpers via `MekanismAPI.getOptionalService`,
   null on Fabric) behind `MekanismHooks` mod loaded checks, and recipe viewer/probe plugins are discovered through NeoForge-only means
   (`@JeiPlugin`, NeoForge entrypoints), so none load or crash when the mods are present or absent on Fabric (`computerCompatEnabled` only runs
   `FactoryRegistry`, which finds no registries). Fabric builds for 26.3 on Modrinth (2026-11-11): JEI, Jade, WTHIT, CC: Tweaked and The One Probe
   have them; EMI, Curios, CraftTweaker, FramedBlocks, ProjectE (and Trinkets/Accessories as a Curios alternative) don't. Plan: port in that order
   as extra `:fabric` source sets compiled from the integration sources with Fabric entrypoints (`jei_mod_plugin`, `jade`, `waila_plugins.json`,
   CC's Fabric peripheral lookup), each gated by its own `fabric.mod.json` `suggests` and the existing mod loaded checks.
8. Move shared sources from `neoforge/src` into `common/`; update docs/multiloader.md.
