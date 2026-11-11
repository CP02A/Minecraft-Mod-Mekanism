# Fabric port status (branch `multiloader-fabric`)

## Architecture
- `shim/`: Fabric-only implementation of the NeoForge APIs Mekanism uses (`net.neoforged.*`, copied from NeoForge, LGPL-2.1).
  Loader specifics go through ServiceLoader backends in `net.neoforged.neoforge.shim` (`ShimServices`).
- `common/src`: the shared sources and resources (and the generated data), compiled by both `:neoforge` and `:fabric` (see docs/multiloader.md).
  Loader specific classes have the same name in `neoforge/src` and `fabric/src` (`VirtualRecipeBookComponent`, `BlockPlasticFenceGate`).
- `fabric/`: compiles `common/src/{api,main}/java` against the shim (Sync task).
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
- `fabric/check_injected_interfaces.py` (run by `./gradlew :fabric:checkInjectedInterfaces`, part of `check`): finds injected default methods that
  conflict at runtime (IncompatibleClassChangeError/IllegalAccessError) between the shim's interfaces and those of Fabric API and the other mods on
  the compile classpath (`loom:injected_interfaces` and class tweaker `inject-interface`s, incl. nested jars) or vanilla, taking Mekanism's and the
  mods' mixins into account. Conflicts are resolved by a mixin implementing the method on the class (`TooltipFlagDefaultMixin`,
  `ItemStackDefaultMixin`), covered by `InjectedInterfaceTests`.

## Verification
- `./gradlew :neoforge:build :neoforge:runGameTestServer` (72 tests)
- `./gradlew :fabric:build` passes fully, including Loom's `validateAccessWidener` (`gen_fabric_glue.py` expands AT wildcards into the members of
  the class, normalizes dotted descriptors and skips members that don't exist in vanilla, such as signatures changed by NeoForge's patches)
- `./gradlew :fabric:checkInjectedInterfaces` (0 conflicting default methods; needs Python 3 as `python` or `$PYTHON`)
- `./gradlew :fabric:runGameTestServer` (Fabric game tests in `fabric/src/gametest`: transfer bridge, machine processing, ore gen, Team Reborn
  energy and item capability interop, universal cables/logistical transporters/mechanical pipes connecting to cubes, machines, tanks and vanilla
  chests placed after them (and disconnecting on removal), energy flowing through cables, forced chunk persistence, ticket level/passenger tick events,
  baby mobs in biome and structure spawn lists, FinalizeSpawnEvent spawn cancellation, SpawnPlacementCheck, plastic beacon beam colors, adding to
  existing creative tabs, tooltip appenders, grindstone merging; `GameplayTests`: dynamic tank, induction matrix charge/discharge, thermal
  evaporation, boiler and SPS forming, chemicals through pressurized tubes, digital miner mining, QIO importer into drives, teleporter, cardboard
  box, configurator wrench/rotate, breaking bounding blocks, block entity save/load and drop/place persistence, configs loaded; `InjectedInterfaceTests`: formerly conflicting injected defaults; `ExtensionHookTests`: explosion resistance of bounding
  blocks and the personal chest, pick block on bounding blocks, fluid tank fluid light, ore experience, piglin currency, free runners on powder snow,
  electric bow flame, robit damage using energy)
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

## Extension hooks Mekanism overrides
Default methods of the shim's extension interfaces that Mekanism or its modules override, and where Fabric calls them (mixins in `mixin/hooks`
unless noted; the call sites are the ones of NeoForge's patches).

| Hook | Mekanism override(s) | Fabric status |
|---|---|---|
| Block `getExplosionResistance` | `BlockBase` (custom resistance), `BlockBounding` (main block's) | `ExplosionDamageCalculatorMixin`, tested |
| Block `onBlockExploded` | `BlockMekanism` (meltdowns) | `BlockBehaviourExplosionMixin` (`onExplosionHit`), untested |
| Block `getLightEmission` | `BlockFluidTank` (fluid light) | `BlockLightEngineMixin`, `LevelChunkLightMixin`, `LightCoordsUtilMixin` (render light), tested via render light |
| Block `getCloneItemStack` | `BlockMekanism` (block data), `BlockBounding` (main block) | `ServerGamePacketListenerImplMixin`, tested |
| Block `getExpDrop` | `BlockOre` | `BlockExperienceMixin` (`Block#dropResources`), tested |
| Block `onDestroyedByPlayer` | `BlockBounding` | `ServerPlayerGameModeDestroyMixin`/`MultiPlayerGameModeDestroyMixin`, tested |
| Block `getFriction`, `getBeaconColorMultiplier` | additions plastic blocks (beacon) | `EntityFrictionMixin` etc., `BeaconBlockEntityMixin`, tested |
| Block `getRelocability` | `BlockMekanism`, `BlockBounding` | NeoForge only API, nothing to call |
| Block `getPistonPushReaction` | `BlockMekanism` (immovable block entities) | not called; vanilla never pushes block entities |
| Block `rotate` (level aware) | `BlockMekanism` | not called (structure rotation uses vanilla `rotate`) |
| Block `getBlockPathType` | `BlockBase` | not called |
| Block `shouldDisplayFluidOverlay` | `BlockTileGlass`, generators' laser focus matrix | not called (client fluid rendering) |
| Block `onCaughtFire` | additions' obsidian TNT | not called |
| BlockEntity `handleUpdateTag`, `onDataPacket` | `TileEntityUpdateable` and most tiles (reduced update tag) | `LevelChunkUpdateTagMixin`, `client.ClientPacketListenerBlockEntityMixin`; exercised (not asserted) by the client test |
| BlockEntity `onLoad`, `onChunkUnloaded`, `getModelData` | various | already called |
| Item `onItemUseFirst`, `doesSneakBypassUse` | cardboard box, configurator, turbine blade | `ServerPlayerGameModeMixin`/`MultiPlayerGameModeMixin`, tested |
| Item `getEnchantmentLevel`, `getAllEnchantments` | Meka-Tool, MekaSuit (module enchantments), electric bow (flame) | `EnchantmentHelperMixin`, `EnchantmentsPredicateMixin` (server registries only), tested |
| Item `isPiglinCurrency` | refined glowstone ingot | `PiglinAiMixin`, tested |
| Item `canWalkOnPowderedSnow` | free runners, MekaSuit boots | `PowderSnowBlockMixin`, tested |
| Item `onDestroyed` (damage source) | QIO dashboard, robit, Meka-Tool, MekaSuit, block items | `ItemEntityMixin`, untested |
| Item `onGlideDamage` | MekaSuit | `LivingEntityGlideMixin`, untested |
| Item `shouldCauseReequipAnimation`, `shouldCauseBlockBreakReset`, `isDamageable` | various | already called |
| Item `supportsEnchantment` | armor, tools, bow, flamethrower | not called (vanilla's enchantment `supportedItems` are used) |
| Item `canPerformAction` | configurator, Meka-Tool | only where the shim calls it; vanilla's tool behaviour is item class/tag based |
| Item `isNotReplaceableByPickAction` | Meka-Tool, MekaSuit | not called |
| Entity `onRemovedFromLevel` | robit (chunk ticket while following) | Fabric's `ENTITY_UNLOAD` events (`GameEventBridge`), untested |
| LivingEntity `onDamageTaken` | robit (energy) | `LivingEntityMixin` (only when the damage was applied), tested |

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
   compiled from `common/src/<module>` with Fabric entrypoints, `fabric.mod.json` and per module overrides in `fabric/src/<module>`, own jars via
   `<module>Jar`), loaded in all runs and covered by game tests (generators produce energy, fusion/fission/turbine multiblocks form, baby mobs,
   plastic fence gate, paxel, FinalizeSpawnEvent, baby mob spawn modifiers, plastic beacon colors; client test renders generators and opens a
   generator GUI). No defense module exists in this repo; datagen stays NeoForge-only.
6. Remaining server side gaps (events/extension hooks Mekanism uses that the shim doesn't fire/call):
   - MobSpawnEvent.PositionCheck is not fired (Mekanism doesn't use it); FinalizeSpawnEvent has no spawner and fires inside `Mob#finalizeSpawn`;
     spawn cancellation is stored in the event (no `Mob#setSpawnCancelled`).
   - Vanilla's attribute tooltips are used instead of NeoForge's `AttributeUtil` ones.
   - Biome modifier climate/special effect changes and attribute removals aren't applied (warned once); direct (unregistered) features/carvers
     can't be added through Fabric's API.
   - Extension hooks Mekanism overrides that still have no Fabric call site (see the hook table below): `supportsEnchantment` (anvil/enchant
     command), `canPerformAction` (NeoForge only item abilities), `isNotReplaceableByPickAction`, `getBlockPathType`, level aware `rotate`,
     `shouldDisplayFluidOverlay` (client fluid rendering), `onCaughtFire` (obsidian TNT), `getPistonPushReaction` (vanilla already refuses to push
     block entities). `onBlockExploded` (meltdowns) is called but not covered by a test. The server's light engine runs off thread, where
     `Level#getBlockEntity` returns null (also on NeoForge), so a fluid tank's fluid light only spreads on the client.
   - Datapack registries only get the bake callbacks of their `RegistryBuilder` configurator: the robit skin registry's default key isn't applied
     (Mekanism's robit falls back to the base skin itself); Mekanism uses no add/clear callbacks.
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
