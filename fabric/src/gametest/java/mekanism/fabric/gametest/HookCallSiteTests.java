package mekanism.fabric.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import mekanism.additions.common.registries.AdditionsBlocks;
import mekanism.additions.common.registries.AdditionsEntityTypes;
import mekanism.api.MekanismRegistries;
import mekanism.api.gear.ModuleData;
import mekanism.common.content.gear.ModuleContainer;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.entity.EntityRobit;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismEntityTypes;
import mekanism.common.registries.MekanismItems;
import mekanism.common.registries.MekanismModules;
import mekanism.common.registries.MekanismRobitSkins;
import mekanism.common.registries.MekanismTicketTypes;
import mekanism.common.tile.base.TileEntityMekanism;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * NeoForge extension hooks that are called from vanilla code NeoForge patches (see the hook table in docs/fabric-port-status.md), called by the shim's mixins.
 */
public class HookCallSiteTests {

    private static ServerPlayer player(GameTestHelper helper, ItemStack held) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static void assertObsidianTntPrimed(GameTestHelper helper, BlockPos pos, String how) {
        helper.assertBlockNotPresent(AdditionsBlocks.OBSIDIAN_TNT.value(), pos);
        helper.assertTrue(!helper.getEntities(AdditionsEntityTypes.OBSIDIAN_TNT.value(), pos, 2).isEmpty(),
              Component.literal("Expected obsidian TNT " + how + " to spawn primed obsidian TNT"));
        helper.assertTrue(helper.getEntities(EntityTypes.TNT, pos, 2).isEmpty(),
              Component.literal("Expected obsidian TNT " + how + " to not spawn vanilla primed TNT"));
    }

    /**
     * Obsidian TNT spawns its own primed TNT when powered or lit with flint and steel (IBlockExtension#onCaughtFire); vanilla's TntBlock#prime would spawn
     * vanilla TNT.
     */
    @GameTest(maxTicks = 100)
    public void obsidianTntIsIgnitedThroughOnCaughtFire(GameTestHelper helper) {
        BlockPos powered = new BlockPos(1, 1, 1);
        helper.setBlock(powered, AdditionsBlocks.OBSIDIAN_TNT.defaultState());
        helper.setBlock(powered.east(), Blocks.REDSTONE_BLOCK);
        assertObsidianTntPrimed(helper, powered, "powered by redstone");

        BlockPos lit = new BlockPos(5, 1, 5);
        helper.setBlock(lit, AdditionsBlocks.OBSIDIAN_TNT.defaultState());
        ServerPlayer player = player(helper, new ItemStack(Items.FLINT_AND_STEEL));
        BlockPos absolute = helper.absolutePos(lit);
        player.setPos(Vec3.atCenterOf(absolute.north(2)));
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
              new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false));
        assertObsidianTntPrimed(helper, lit, "lit with flint and steel");
        helper.succeed();
    }

    private static ItemStack enchantedBook(Holder<Enchantment> enchantment) {
        return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, 1));
    }

    private static ItemStack anvilResult(GameTestHelper helper, ItemStack input, ItemStack addition) {
        ServerPlayer player = player(helper, ItemStack.EMPTY);
        AnvilMenu anvil = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
        anvil.getSlot(0).set(input);
        anvil.getSlot(1).set(addition);
        anvil.createResult();
        return anvil.getSlot(2).getItem();
    }

    /**
     * The electric bow can't get flame (it has its own fire mode) from an enchanted book in the anvil nor from the enchanting table
     * (IItemExtension#supportsEnchantment), but can get power.
     */
    @GameTest
    public void electricBowDoesNotSupportFlame(GameTestHelper helper) {
        Registry<Enchantment> enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> flame = enchantments.getOrThrow(Enchantments.FLAME);
        Holder<Enchantment> power = enchantments.getOrThrow(Enchantments.POWER);
        ItemStack bow = new ItemStack(MekanismItems.ELECTRIC_BOW.asItem());
        helper.assertTrue(!anvilResult(helper, bow.copy(), enchantedBook(power)).isEmpty(), Component.literal("Expected the anvil to apply power to the electric bow"));
        ItemStack flameResult = anvilResult(helper, bow.copy(), enchantedBook(flame));
        helper.assertTrue(flameResult.isEmpty(), Component.literal("Expected the anvil to not apply flame to the electric bow, got " + flameResult));

        List<EnchantmentInstance> available = EnchantmentHelper.getAvailableEnchantmentResults(30, bow, enchantments.listElements().map(holder -> holder));
        helper.assertTrue(available.stream().anyMatch(instance -> instance.enchantment().is(Enchantments.POWER)),
              Component.literal("Expected the enchanting table to offer power for the electric bow"));
        helper.assertTrue(available.stream().noneMatch(instance -> instance.enchantment().is(Enchantments.FLAME)),
              Component.literal("Expected the enchanting table to not offer flame for the electric bow"));
        helper.succeed();
    }

    /**
     * A destroyed item entity of a machine drops the machine's inventory (IItemExtension#onDestroyed with the damage source).
     */
    @GameTest(maxTicks = 100)
    public void destroyedMachineItemDropsItsContents(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MekanismBlocks.ENRICHMENT_CHAMBER.defaultState());
        TileEntityMekanism chamber = helper.getBlockEntity(pos, TileEntityMekanism.class);
        chamber.applyComponentsFromItemStack(new ItemStack(MekanismBlocks.ENRICHMENT_CHAMBER));
        chamber.getInventorySlots().stream()
              .filter(slot -> slot.isValid(ItemResource.of(Items.IRON_ORE)))
              .findFirst()
              .orElseThrow(() -> helper.assertionException(pos, "Expected an input slot for iron ore"))
              .setContents(ItemResource.of(Items.IRON_ORE), 8, null);
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM, pos, 2);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(MekanismBlocks.ENRICHMENT_CHAMBER.asItem()),
              Component.literal("Expected the enrichment chamber to drop itself, got " + drops));
        ItemEntity machine = drops.getFirst();
        machine.hurtServer(helper.getLevel(), helper.getLevel().damageSources().cactus(), 10);
        helper.assertTrue(machine.isRemoved(), Component.literal("Expected the machine's item entity to be destroyed"));
        helper.assertTrue(helper.getEntities(EntityTypes.ITEM, pos, 3).stream().anyMatch(item -> item.getItem().is(Items.IRON_ORE) && item.getItem().getCount() == 8),
              Component.literal("Expected the destroyed machine item to drop its iron ore"));
        helper.succeed();
    }

    /**
     * A MekaSuit with an elytra unit can glide (gliding flight attribute from its attribute modifiers) and uses energy for it instead of durability
     * (IItemExtension#onGlideDamage).
     */
    @GameTest(maxTicks = 100)
    public void mekaSuitElytraUsesEnergyWhenGliding(GameTestHelper helper) throws ReflectiveOperationException {
        ItemStack chestplate = new ItemStack(MekanismItems.MEKASUIT_BODYARMOR.asItem());
        ItemAccess access = ItemAccess.forStack(chestplate);
        ModuleContainer container = ModuleHelper.get().getModuleContainer(chestplate);
        helper.assertTrue(container != null, Component.literal("Expected the MekaSuit to have a module container"));
        Holder<ModuleData<?>> elytraUnit = MekanismRegistries.MODULES.wrapAsHolder(MekanismModules.ELYTRA_UNIT.get());
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(container.addModule(helper.getLevel().registryAccess(), access, elytraUnit, 1, transaction), 1, "installed elytra units");
            transaction.commit();
        }
        EnergyHandler energy = access.getCapability(Capabilities.Energy.ITEM);
        helper.assertTrue(energy != null, Component.literal("Expected the MekaSuit to have an energy handler"));
        try (Transaction transaction = Transaction.openRoot()) {
            energy.insert(1_000_000, transaction);
            transaction.commit();
        }
        long before = energy.getAmountAsLong();
        helper.assertTrue(before > 0, Component.literal("Expected the MekaSuit to be charged"));

        //Note: Creative players glide for free
        ServerPlayer player = player(helper, ItemStack.EMPTY);
        helper.assertValueEqual(player.gameMode(), GameType.SURVIVAL, "game mode");
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 5, 2))));
        player.setOnGround(false);
        helper.assertTrue(LivingEntity.canGlideUsing(player.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST),
              Component.literal("Expected the MekaSuit's elytra unit to let its wearer glide"));
        player.startFallFlying();
        //Glide damage is applied every 20 ticks of gliding
        Field fallFlyTicks = LivingEntity.class.getDeclaredField("fallFlyTicks");
        fallFlyTicks.setAccessible(true);
        fallFlyTicks.setInt(player, 19);
        Method updateFallFlying = LivingEntity.class.getDeclaredMethod("updateFallFlying");
        updateFallFlying.setAccessible(true);
        updateFallFlying.invoke(player);
        helper.assertTrue(player.isFallFlying(), Component.literal("Expected the player to still be gliding"));
        long after = ItemAccess.forStack(player.getItemBySlot(EquipmentSlot.CHEST)).getCapability(Capabilities.Energy.ITEM).getAmountAsLong();
        helper.assertTrue(after < before, Component.literal("Expected gliding to use the MekaSuit's energy, it has " + after + " of " + before));
        helper.succeed();
    }

    /**
     * A following robit that is removed from the level (e.g. by its chunk unloading) loads its chunk for a moment, so it can teleport to its owner
     * (IEntityExtension#onRemovedFromLevel).
     */
    @GameTest(maxTicks = 100)
    public void followingRobitKeepsItsChunkLoadedWhenRemoved(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        EntityRobit robit = helper.spawn(MekanismEntityTypes.ROBIT.value(), new BlockPos(1, 1, 1));
        robit.setOwnerUUID(owner.getUUID(), null);
        robit.setFollowing(true);
        ChunkPos chunk = ChunkPos.containing(robit.blockPosition());
        TicketStorage tickets = helper.getLevel().getDataStorage().get(TicketStorage.TYPE);
        helper.assertTrue(tickets != null, Component.literal("Expected the level to have a ticket storage"));
        long packed = ChunkPos.pack(chunk.x(), chunk.z());
        helper.assertTrue(tickets.getTickets(packed).stream().noneMatch(ticket -> ticket.getType() == MekanismTicketTypes.ROBIT_CHUNK_UNLOAD.value()),
              Component.literal("Expected no robit ticket before the robit is removed"));
        robit.discard();
        helper.assertTrue(tickets.getTickets(packed).stream().anyMatch(ticket -> ticket.getType() == MekanismTicketTypes.ROBIT_CHUNK_UNLOAD.value()),
              Component.literal("Expected the removed following robit to load its chunk"));
        helper.succeed();
    }

    /**
     * The robit skin registry is a defaulted registry with the base skin as its default (NeoForge's RegistryBuilder#defaultKey of a datapack registry).
     */
    @GameTest
    public void robitSkinRegistryDefaultsToTheBaseSkin(GameTestHelper helper) {
        Registry<?> skins = helper.getLevel().registryAccess().lookupOrThrow(MekanismRegistries.Keys.ROBIT_SKINS);
        helper.assertTrue(skins instanceof DefaultedRegistry<?>, Component.literal("Expected the robit skin registry to be a defaulted registry, it is a " + skins.getClass()));
        ResourceKey<?> base = MekanismRobitSkins.BASE;
        helper.assertValueEqual(((DefaultedRegistry<?>) skins).getDefaultKey(), base.identifier(), "default robit skin");
        helper.assertTrue(skins.getValue(Identifier.fromNamespaceAndPath("mekanism_fabric_tests", "missing")) == skins.getValue(base.identifier()),
              Component.literal("Expected unknown robit skins to resolve to the base skin"));
        helper.succeed();
    }
}
