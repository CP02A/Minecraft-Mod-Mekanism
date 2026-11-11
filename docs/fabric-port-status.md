# Fabric port status (branch `multiloader-fabric`)

## Architecture
- `shim/`: Fabric-only implementation of the NeoForge APIs Mekanism uses (`net.neoforged.*`, copied from NeoForge, LGPL-2.1).
  Loader specifics go through ServiceLoader backends in `net.neoforged.neoforge.shim` (`ShimServices`).
- `common/src`: the shared sources and resources (and the generated data), compiled by both `:neoforge` and `:fabric` (see docs/multiloader.md).
  Loader specific classes have the same name in `neoforge/src` and `fabric/src` (`VirtualRecipeBookComponent`, `BlockPlasticFenceGate`).
- `fabric/`: compiles `common/src/{api,main}/java` (and the modules, see Working) against the shim.
  - `mekanism/fabric/shim/`: backends (transactions, registries, network, attachments, data maps, biome modifiers),
    `ShimModLoader` (constructs mods from the `neoforge-shim` / `neoforge-shim-client` entrypoints and fires the mod bus events),
    `GameEventBridge` (Fabric events -> NeoForge game events), `ServerHooks` (server side hooks called from mixins), `RecipeContentSync`
    (`OnDatapackSyncEvent#sendRecipes` / `RecipesReceivedEvent`), `AdvancedMenuOpening` (menu extra data from
    `IMenuProviderExtension#writeClientSideData` passed to `IContainerFactory`), `transfer/` (NeoForge item/fluid/energy block and item
    capabilities <-> Fabric Transfer API and Team Reborn Energy, `ItemAccess` <-> `ContainerItemContext`).
  - `mekanism/fabric/shim/client/`: `ClientShimLoader` (client registration events -> Fabric client APIs), `FabricClientBackend`,
    `BlockStateModelBridge` (NeoForge custom blockstate models), `ModelPropertiesBridge` (root transform, part visibility; passed to geometries
    by `bakeTopGeometry` callers), NeoForge's `neoforge:fluid_container` (with its unlit render pipelines and `item_unlit` shader) and
    `neoforge:trimmed_armor` item models, `ClientGameEventBridge` (client tick, entity join, recipe sync, HUD layer, custom geometry and menu
    screen events), `BlockOutlineBridge` (custom block outline renderers, tracked by identity as vanilla's `BlockOutlineRenderState` can't hold
    them), `LevelAwareBlockStateModel` (Fabric's `emitQuads` uses NeoForge's level aware `collectParts`, i.e. model data), NeoForge's
    `ModelDataManager` (per client level, snapshotted per meshing region by `RenderRegionCacheMixin`), `ItemLayerFaceData` (item layer
    `neoforge_data` light emission).
  - `mekanism/fabric/mixin/`: `data/` (NeoForge ingredient/holder set/conditions formats), `hooks/` (server and common hooks: block entity
    lifecycle, capability invalidation, ticks, damage, fall, jump, attributes, break speed, crafting, menus, recipe book settings, chunk tickets,
    datapack registry callbacks and default keys, `ItemUseHooks` and the extension hooks in the table below), `client/` (level/login events,
    input, fog, arm, sound, block outlines, NeoForge's model `loader`/`transform`/`visibility`/`neoforge_data` json keys, model baking/atlas/fluid
    model/particle/tint events, recipe book search, water tint, fluid overlay), and generated `shim/` (interface injection + enum extensions).
- `fabric/gen_fabric_glue.py <merged MC jar>`: regenerates the access widener, injected interfaces and shim mixins. Re-run after changing ATs
  (including `fabric/fabric-accesstransformer.cfg`), `shim-injected-interfaces.json` or `enumextensions.json`.
- `fabric/check_injected_interfaces.py` (`./gradlew :fabric:checkInjectedInterfaces`, part of `check`): finds injected default methods that
  conflict at runtime (IncompatibleClassChangeError/IllegalAccessError) between the shim's interfaces and those of Fabric API, the other mods on
  the compile classpath (`loom:injected_interfaces` and class tweaker `inject-interface`s, incl. nested jars) or vanilla, taking the mixins into
  account. Conflicts are resolved by a mixin implementing the method on the class (`TooltipFlagDefaultMixin`, `ItemStackDefaultMixin`).

## Verification
- `./gradlew :neoforge:build :neoforge:runGameTestServer`: 72 tests pass.
- `./gradlew :fabric:build` passes fully, including Loom's `validateAccessWidener` (`gen_fabric_glue.py` expands AT wildcards, normalizes dotted
  descriptors and skips members that don't exist in vanilla, such as signatures changed by NeoForge's patches) and `checkInjectedInterfaces`
  (0 conflicting default methods; needs Python 3 as `python` or `$PYTHON`).
- `./gradlew :fabric:runGameTestServer`: 79 tests pass (`fabric/src/gametest`):
  - `TransferBridgeTests`, `InteropTests`: Fabric Transfer API and Team Reborn Energy interop (blocks and items, both directions), machine
    processing, ore gen.
  - `TransmitterTests`: cables, transporters and pipes connecting to cubes, machines, tanks and chests placed after them; energy flow.
  - `ServerHookTests`: forced chunk persistence, ticket level/passenger tick events, FinalizeSpawnEvent cancellation, SpawnPlacementCheck,
    existing creative tabs, tooltip appenders, grindstone merging.
  - `GameplayTests`: dynamic tank, induction matrix, thermal evaporation, boiler, SPS, chemicals through tubes, digital miner, QIO, teleporter,
    cardboard box, configurator, bounding blocks, block entity/item persistence, configs.
  - `ExtensionHookTests`, `HookCallSiteTests`: the extension hooks in the table below (explosion resistance, pick block, fluid light, ore
    experience, piglin currency, powder snow, electric bow flame and enchantment support, robit damage and removal, obsidian TNT ignition,
    destroyed item drops, MekaSuit glide energy, robit skin registry default).
  - `GeneratorsTests`, `AdditionsToolsTests`: generators produce energy, fusion/fission/turbine form, path type of formed casings, meltdown;
    baby mobs (biome and structure spawns), plastic fence gate and beacon colors, paxel, FinalizeSpawnEvent.
  - `InjectedInterfaceTests`: formerly conflicting injected defaults.
  - `LookingAtTests` (8): the server data providers of Jade (its block data request handler), The One Probe (its probe info builder, with a
    probe) and WTHIT for energy cubes, fluid and chemical tanks and a formed dynamic tank (multiblock contents from a casing), checking the
    synced energy/fluid/chemical elements after a network round trip.
- `./gradlew :fabric:runGameTestClient` (`MekanismClientTests`, `GameplayClientTests`, `GeneratorsClientTests`): loads a world, checks
  model/atlas/fluid model events, recipe book settings and recipe sync, opens machine and generator GUIs and changes side config (checked on the
  server), opens Mekanism's recipe book, renders fluid tanks and buckets, emissive item layers and connected transmitters (model data checked
  against the server and the ModelDataManager), composite and trimmed armor models, the armor layer, tooltips, chunk update tags (fluid tank
  scale) and a portable teleporter frequency. `JeiClientTests` (JEI is only on the client runs' classpath, via `fabric.addMods`): Mekanism's and
  Generators' plugins, the chemical ingredient type, recipe categories and recipes (fluid slots), catalysts, item subtypes, exclusion areas,
  ghost ingredient and recipe transfer handlers, `ScreenEvent.Opening` marking a GUI as switching to JEI, and screenshots of JEI's recipe view.
  `LookingAtClientTests`: the Jade, WTHIT and TOP client registrations (tooltip renderers, configs, bounding block redirection, element
  factories), and an energy cube's server synced energy in Jade's and WTHIT's overlays (screenshots, TOP's overlay shows as well).
  Screenshots in `fabric/runs/gametestclient/screenshots`.
- `./gradlew :fabric:runServer`: the dedicated server boots (`fabric/runs/server/eula.txt` is gitignored and must be created locally).

## Working
- Server boots with all Mekanism content. Generators, Additions and Tools are separate Fabric mods (`generators`/`additions`/`tools` source sets
  in `:fabric`, compiled from `common/src/<module>` with per module overrides in `fabric/src/<module>`, own jars via `<module>Jar`). No defense
  module exists in this repo; datagen stays NeoForge-only.
- Recipes, tags, conditions, data maps, biome modifiers (all but climate/special effects; `BiomeModifierBridge`, server configs load before
  Fabric's biome modification) and structure modifiers (`StructureMixin`) load. Datapack registries get their `RegistryBuilder` bake callbacks
  and default key (`ShimBackend.addDatapackRegistry` -> `RegistryLoadTaskMixin` creates a `DefaultedMappedRegistry`; robit skins).
- Item/fluid interop with the Fabric Transfer API and energy interop with Team Reborn Energy (1:1, bundled jar-in-jar), blocks and items, both
  directions.
- Forced chunks of Mekanism's chunk loaders persist and are validated on load; ChunkTicketLevelUpdatedEvent, RegisterSpawnPlacementsEvent,
  server/passenger EntityTickEvents, FinalizeSpawnEvent cancellation, SpawnPlacementCheck, BuildCreativeModeTabContentsEvent and NeoForge's
  tooltip appenders (`ItemStackTooltipMixin`, after vanilla's component tooltips) work. Block friction, beacon colors, `isDamageable`,
  `shouldCauseReequipAnimation` and `shouldCauseBlockBreakReset` are honored.
- Equipment attribute modifiers from ItemAttributeModifierEvent apply (`ItemStackMixin` now targets both `forEachModifier` overloads, it used
  to target only the first), so the MekaSuit's and free runners' conditional attributes work.
- Main module multiblocks, chemicals in tubes, digital miner, QIO, teleporters, cardboard box, configurator, bounding block drops, persistence,
  configs, item-held GUIs (portable teleporter) and their packets.
- Client: joins a world and renders Mekanism blocks and items (custom energy cube model, NeoForge composite/OBJ loaders, model data and part
  visibility, fluid containers, emissive layers); model baking/atlas/fluid model/particle/tint/render pipeline events; machine GUIs with NeoForge
  menu extra data and side config; Mekanism's recipe book with saved/synced settings; synced recipes; EntityRenderersEvent.AddLayers.
- JEI (31.17.0.109): the integration's sources are shared in `common/src/integration/jei/<module>` (also compiled by `:neoforge`) and compiled
  into the `jei`/`<module>Jei` source sets with the Fabric `JeiFluidHelper` (`src/jei`; converts Mekanism's fluid stacks to JEI's Fabric fluid
  ingredients, and resolves NeoForge's fluid slot displays itself as JEI's Fabric fluid helper can't), packaged into the module jars and
  registered through `jei_mod_plugin` entrypoints (`MekanismFabricJeiPlugin` registers the screen hooks, using the shim's `ScreenEvent.Opening`).
  All categories, catalysts, transfer handlers, GUI handlers, subtypes and aliases. Mekanism's recipe book window opening mostly off-screen at
  small resolutions is shared code (`GuiRecipeBookWindowTab` places it below the GUI, unclamped), so not Fabric specific.
- Jade (26.3.5), The One Probe (26.3-16.0.2) and WTHIT (21.0.0): the integrations' sources are shared in `common/src/integration/{jade,
  the_one_probe,wthit}` (also compiled by `:neoforge`; TOP's IMC registration `MekanismTOPIntegration` stays in `neoforge/src`), compiled into
  the `integration{Jade,TheOneProbe,Wthit}` source sets and packaged into the main jar. Registered through the `jade` entrypoint, the
  `theoneprobe` entrypoint (`MekanismTOPPlugin` in `fabric/src/integration/the_one_probe`) and WTHIT's `wthit_plugins.json`; all three are
  `suggests` and in the dev runs/game tests (Modrinth maven). Energy, fluid and chemical elements, multiblock contents, robit energy, builtin
  energy/fluid line removal and bounding block redirection, unchanged from NeoForge. WTHIT refuses to start when `net.neoforged.fml.ModList`
  exists (which the shim provides), so `compat.WailaPlatformCheckMixin` (`@Pseudo`, only applies with WTHIT) hides the shim from that check.

## Extension hooks Mekanism overrides
Default methods of the shim's extension interfaces that Mekanism or its modules override, and where Fabric calls them (mixins in `mixin/hooks`
unless noted; the call sites are the ones of NeoForge's patches).

| Hook | Mekanism override(s) | Fabric status |
|---|---|---|
| Block `getExplosionResistance` | `BlockBase` (custom resistance), `BlockBounding` (main block's) | `ExplosionDamageCalculatorMixin`, tested |
| Block `onBlockExploded` | `BlockMekanism` (meltdowns) | `BlockBehaviourExplosionMixin` (`onExplosionHit`), tested (`GeneratorsTests` meltdown) |
| Block `getLightEmission` | `BlockFluidTank` (fluid light) | `BlockLightEngineMixin`, `LevelChunkLightMixin`, `LightCoordsUtilMixin` (render light), tested via render light |
| Block `getCloneItemStack` | `BlockMekanism` (block data), `BlockBounding` (main block) | `ServerGamePacketListenerImplMixin`, tested |
| Block `getExpDrop` | `BlockOre` | `BlockExperienceMixin` (`Block#dropResources`), tested |
| Block `onDestroyedByPlayer` | `BlockBounding` | `ServerPlayerGameModeDestroyMixin`/`MultiPlayerGameModeDestroyMixin`, tested |
| Block `getFriction`, `getBeaconColorMultiplier` | additions plastic blocks (beacon) | `EntityFrictionMixin` etc., `BeaconBlockEntityMixin`, tested |
| Block `getRelocability` | `BlockMekanism`, `BlockBounding` | NeoForge only API, nothing to call |
| Block `getPistonPushReaction` | `BlockMekanism` (immovable block entities) | not called; vanilla never pushes block entities |
| Block `rotate` (level aware) | `BlockMekanism` | NeoForge 26.3 has no call site either, nothing to call |
| Block `getBlockPathType` | `BlockBase` | `WalkNodeEvaluatorMixin`, tested |
| Block `shouldDisplayFluidOverlay` | `BlockTileGlass`, generators' laser focus matrix | `client.FluidRendererOverlayMixin` (OR-ed into the `LeavesBlock` check; Fabric API replaces the `HalfTransparentBlock` one), untested |
| Block `onCaughtFire` | additions' obsidian TNT | `TntBlockMixin`, `FireBlockIgnitionMixin`, `FlintAndSteelDispenseMixin`, tested |
| BlockEntity `handleUpdateTag`, `onDataPacket` | `TileEntityUpdateable` and most tiles (reduced update tag) | `LevelChunkUpdateTagMixin`, `client.ClientPacketListenerBlockEntityMixin`; chunk update tags tested by the client test (fluid tank scale), `onDataPacket` only exercised (Mekanism uses its own `PacketUpdateTile` at runtime) |
| BlockEntity `onLoad`, `onChunkUnloaded`, `getModelData` | various | already called |
| Item `onItemUseFirst`, `doesSneakBypassUse` | cardboard box, configurator, turbine blade | `ServerPlayerGameModeMixin`/`MultiPlayerGameModeMixin`, tested |
| Item `getEnchantmentLevel`, `getAllEnchantments` | Meka-Tool, MekaSuit (module enchantments), electric bow (flame) | `EnchantmentHelperMixin`, `EnchantmentsPredicateMixin` (server registries only), tested |
| Item `isPiglinCurrency` | refined glowstone ingot | `PiglinAiMixin`, tested |
| Item `canWalkOnPowderedSnow` | free runners, MekaSuit boots | `PowderSnowBlockMixin`, tested |
| Item `onDestroyed` (damage source) | QIO dashboard, robit, Meka-Tool, MekaSuit, block items | `ItemEntityMixin`, tested |
| Item `onGlideDamage` | MekaSuit | `LivingEntityGlideMixin`, tested; its `canGlideUsing` also accepts items whose slot modifiers give `GLIDING_FLIGHT` |
| Item `shouldCauseReequipAnimation`, `shouldCauseBlockBreakReset`, `isDamageable` | various | already called |
| Item `supportsEnchantment` | armor, tools, bow, flamethrower | `SupportsEnchantmentMixin` (enchanting table, `isPrimaryItemFor`), `AnvilMenuMixin`, `EnchantCommandMixin`, `EnchantRandomlyFunctionMixin`, tested |
| Item `canPerformAction` | configurator, Meka-Tool | still not called (only where the shim calls it); vanilla's tool behaviour is item class/tag based |
| Item `isNotReplaceableByPickAction` | Meka-Tool, MekaSuit | NeoForge 26.3 has no call site either, nothing to call |
| Entity `onRemovedFromLevel` | robit (chunk ticket while following) | Fabric's `ENTITY_UNLOAD` events (`GameEventBridge`), tested |
| LivingEntity `onDamageTaken` | robit (energy) | `LivingEntityMixin` (only when the damage was applied), tested |

## Not done yet
1. Integrations: JEI, Jade, The One Probe and WTHIT are ported (see Working). The other integration source sets (`neoforge/src/integration/*`:
   computer_craft, crafttweaker, curios, emi, framedblocks, nerb, open_computers, projecte, recipe_stages) aren't compiled for Fabric. Main code only reaches them through optional
   services (null on Fabric) behind `MekanismHooks` mod loaded checks, and their plugins are discovered through NeoForge-only means, so nothing
   loads or crashes with or without the mods. Fabric 26.3 builds exist (Modrinth, 2026-11-11) for JEI, Jade, WTHIT, CC: Tweaked and The One
   Probe; not for EMI, Curios, CraftTweaker, FramedBlocks, ProjectE (or Trinkets/Accessories).
2. Interop: Mekanism chemicals have no Fabric equivalent (not exposed); `ItemAccess` can't distinguish a container item's main slot from
   overflow, so Fabric `ContainerItemContext` main slot insertions of a matching item may overflow; energy amounts are saturated to `int` per
   operation (NeoForge's `EnergyHandler`).
3. Server side:
   - NeoForge's server side canGlide checks only the entity's `GLIDING_FLIGHT` attribute. Fabric keeps vanilla's per item check, so the elytra
     unit's shift-to-disengage modifier doesn't apply.
   - Uncalled hooks: `canPerformAction`, `getPistonPushReaction` (see the table). The server's light engine runs off thread, where
     `Level#getBlockEntity` returns null (also on NeoForge), so a fluid tank's fluid light only spreads on the client.
   - MobSpawnEvent.PositionCheck is not fired (unused by Mekanism); FinalizeSpawnEvent has no spawner and fires inside `Mob#finalizeSpawn`;
     spawn cancellation is stored in the event (no `Mob#setSpawnCancelled`).
   - Biome modifier climate/special effect changes and attribute removals aren't applied (warned once); direct (unregistered) features/carvers
     can't be added through Fabric's API. Datapack registry add/clear callbacks aren't supported (Mekanism uses none).
   - Vanilla's attribute tooltips are used instead of NeoForge's `AttributeUtil` ones.
   - Not supported by the shim: tool modification events, enchantment level event, block drops experience, custom permission handlers,
     explosion events, BooleanAttribute/PercentageAttribute tooltip formatting.
4. Client side: RenderLevelStageEvent, ScreenEvent and tooltip events (RenderTooltipEvent, ItemTooltipEvent, ...) are not fired (Mekanism
   doesn't subscribe to them). Vanilla's `AbstractContainerScreen#extractSlot` doesn't call `renderSlotContents` (Mekanism's screens call it
   themselves). The `neoforge:conditional` model loader isn't ported. Only the light emission of NeoForge's `ExtraFaceData` is applied (vanilla's
   face baking has no colour/per face light/ambient occlusion support, `BakedQuad.MaterialInfo` has no AO flag). Client side EntityTickEvent for
   non-passengers isn't fired.
5. Test gaps: forced chunk persistence is tested via a codec round trip, not an actual server restart; `shouldDisplayFluidOverlay`,
   `onDataPacket`, respawn (Clone) and fog/arm events are wired but not asserted.

## Next steps
1. Port the remaining integration that has a Fabric 26.3 build (CC: Tweaked) like JEI and the overlay mods: an extra `:fabric` source set
   compiled from the integration sources with CC's peripheral lookup, gated by `fabric.mod.json` `suggests` and the existing mod loaded checks.
2. Expose chemicals to Fabric (e.g. a Fabric API lookup for Mekanism's chemical handlers).
3. Remove the `int` energy cap of the energy bridge.
4. Server side canGlide parity for the elytra unit; the remaining uncalled hooks (`canPerformAction`).
5. Client models: `ExtraFaceData` colour/AO and the `neoforge:conditional` model loader.
6. RenderLevelStage, Screen and tooltip events (unused by Mekanism) if other shim users need them.
7. Close the test gaps above.
