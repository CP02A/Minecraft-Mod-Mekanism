package mekanism.fabric.gametest;

import mekanism.common.entity.EntityRobit;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.registries.MekanismEntityTypes;
import mekanism.common.registries.MekanismItems;
import mekanism.common.resource.ore.OreType;
import mekanism.common.tile.TileEntityFluidTank;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Level aware NeoForge extension hooks (IBlockExtension, IItemExtension, IEntityExtension) that vanilla has no call site for, called by the shim's mixins.
 */
public class ExtensionHookTests {

    private static ServerPlayer player(GameTestHelper helper, GameType gameType, ItemStack held) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static BlockPos placeMiner(GameTestHelper helper, BlockPos minerPos) {
        ServerPlayer player = player(helper, GameType.SURVIVAL, new ItemStack(MekanismBlocks.DIGITAL_MINER));
        helper.placeAt(player, player.getMainHandItem(), minerPos.below(), Direction.UP);
        BlockPos boundingPos = minerPos.offset(1, 1, 0);
        helper.assertBlockPresent(MekanismBlocks.BOUNDING_BLOCK.value(), boundingPos);
        return boundingPos;
    }

    private static void explode(GameTestHelper helper, BlockPos pos, float power) {
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(pos));
        helper.getLevel().explode(null, center.x, center.y, center.z, power, Level.ExplosionInteraction.TNT);
    }

    /**
     * Bounding blocks use the explosion resistance of their main block (getExplosionResistance): an explosion that would destroy a block with the bounding
     * block's own resistance (4.8) doesn't destroy the digital miner's (16).
     */
    @GameTest(maxTicks = 100)
    public void boundingBlocksUseTheMainBlocksExplosionResistance(GameTestHelper helper) {
        BlockPos minerPos = new BlockPos(3, 2, 3);
        BlockPos boundingPos = placeMiner(helper, minerPos);
        explode(helper, boundingPos, 2);
        helper.assertBlockPresent(MekanismBlocks.BOUNDING_BLOCK.value(), boundingPos);
        helper.assertBlockPresent(MekanismBlocks.DIGITAL_MINER.value(), minerPos);
        helper.succeed();
    }

    /**
     * The personal chest's custom resistance (-1) makes an explosion that couldn't destroy a normal machine (resistance 16) destroy it.
     */
    @GameTest(maxTicks = 100)
    public void personalChestUsesItsCustomExplosionResistance(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(3, 2, 3);
        helper.setBlock(chestPos, MekanismBlocks.PERSONAL_CHEST.defaultState());
        explode(helper, chestPos, 1);
        helper.assertBlockNotPresent(MekanismBlocks.PERSONAL_CHEST.value(), chestPos);
        helper.succeed();
    }

    /**
     * Pick block on a bounding block gives the main block (getCloneItemStack with a player).
     */
    @GameTest(maxTicks = 100)
    public void pickBlockOnBoundingBlockGivesTheMainBlock(GameTestHelper helper) {
        BlockPos boundingPos = placeMiner(helper, new BlockPos(3, 1, 3));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(boundingPos.north(2))));
        player.connection.handlePickItemFromBlock(new ServerboundPickItemFromBlockPacket(helper.absolutePos(boundingPos), false));
        helper.assertTrue(player.getMainHandItem().is(MekanismBlocks.DIGITAL_MINER.asItem()),
              Component.literal("Expected pick block on a bounding block to give a digital miner, got " + player.getMainHandItem()));
        helper.succeed();
    }

    /**
     * A fluid tank with a glowing fluid is rendered with its fluid's light (level aware getLightEmission). Note: the server's light engine runs off
     * thread, where block entities can't be looked up (the same on NeoForge), so the tank's light only spreads on the client.
     */
    @GameTest(maxTicks = 100)
    public void fluidTankWithGlowingFluidIsLit(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(1, 1, 1);
        helper.setBlock(tankPos, MekanismBlocks.BASIC_FLUID_TANK.defaultState());
        helper.getBlockEntity(tankPos, TileEntityFluidTank.class).applyComponentsFromItemStack(new ItemStack(MekanismBlocks.BASIC_FLUID_TANK));
        ResourceHandler<FluidResource> fluids = Capabilities.Fluid.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(tankPos), null, null, Direction.UP);
        helper.assertTrue(fluids != null, Component.literal("Expected the fluid tank to expose a fluid handler"));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(fluids.insert(FluidResource.of(Fluids.LAVA), 4_000, transaction), 4_000, "inserted lava");
            transaction.commit();
        }
        int blockLight = LightCoordsUtil.block(LightCoordsUtil.getLightCoords(helper.getLevel(), helper.absolutePos(tankPos)));
        helper.assertValueEqual(blockLight, 15, "block light the lava filled tank is rendered with");
        helper.succeed();
    }

    /**
     * Mekanism's ores drop experience (getExpDrop).
     */
    @GameTest(maxTicks = 100)
    public void oresDropExperience(GameTestHelper helper) {
        BlockPos orePos = new BlockPos(1, 1, 1);
        helper.setBlock(orePos, MekanismBlocks.ORES.get(OreType.FLUORITE).stone().defaultState());
        ServerPlayer player = player(helper, GameType.SURVIVAL, new ItemStack(Items.DIAMOND_PICKAXE));
        player.setPos(Vec3.atCenterOf(helper.absolutePos(orePos.above())));
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(orePos)), Component.literal("The ore should be destroyed"));
        helper.assertTrue(!helper.getEntities(EntityTypes.EXPERIENCE_ORB, orePos, 2).isEmpty(), Component.literal("Expected fluorite ore to drop experience"));
        helper.succeed();
    }

    /**
     * Piglins take refined glowstone ingots as currency (isPiglinCurrency).
     */
    @GameTest(maxTicks = 100)
    public void piglinsBarterForRefinedGlowstone(GameTestHelper helper) {
        Piglin piglin = helper.spawn(EntityTypes.PIGLIN, new BlockPos(1, 1, 1));
        piglin.setBaby(false);
        ServerPlayer player = player(helper, GameType.SURVIVAL, new ItemStack(MekanismItems.REFINED_GLOWSTONE_INGOT.asItem(), 2));
        piglin.interact(player, InteractionHand.MAIN_HAND, piglin.position());
        helper.assertTrue(piglin.getOffhandItem().is(MekanismItems.REFINED_GLOWSTONE_INGOT.asItem()),
              Component.literal("Expected the piglin to take the refined glowstone ingot, it holds " + piglin.getOffhandItem()));
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "remaining ingots");
        helper.succeed();
    }

    /**
     * Free runners let their wearer walk on powder snow (canWalkOnPowderedSnow).
     */
    @GameTest
    public void freeRunnersWalkOnPowderSnow(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        helper.assertTrue(!PowderSnowBlock.canEntityWalkOnPowderSnow(player), Component.literal("A player without boots shouldn't walk on powder snow"));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(MekanismItems.FREE_RUNNERS.asItem()));
        helper.assertTrue(PowderSnowBlock.canEntityWalkOnPowderSnow(player), Component.literal("Free runners should walk on powder snow"));
        helper.succeed();
    }

    /**
     * The electric bow's fire mode gives it flame (gameplay enchantments, getEnchantmentLevel/getAllEnchantments).
     */
    @GameTest
    public void electricBowFireModeGivesFlame(GameTestHelper helper) {
        var flame = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FLAME);
        ItemStack bow = new ItemStack(MekanismItems.ELECTRIC_BOW.asItem());
        bow.set(MekanismDataComponents.ELECTRIC_BOW_MODE.get(), true);
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(flame, bow), 1, "flame level in fire mode");
        helper.assertTrue(EnchantmentHelper.hasTag(bow, net.minecraft.tags.EnchantmentTags.NON_TREASURE),
              Component.literal("Expected the fire mode's flame to be found by enchantment tag checks"));
        bow.set(MekanismDataComponents.ELECTRIC_BOW_MODE.get(), false);
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(flame, bow), 0, "flame level without fire mode");
        helper.succeed();
    }

    /**
     * Damaging a robit uses its energy (ILivingEntityExtension#onDamageTaken).
     */
    @GameTest(maxTicks = 100)
    public void robitDamageUsesEnergy(GameTestHelper helper) {
        EntityRobit robit = helper.spawn(MekanismEntityTypes.ROBIT.value(), new BlockPos(1, 1, 1));
        robit.getEnergyContainer().setEnergy(100_000, null);
        robit.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2);
        long energy = robit.getEnergyContainer().getAmountAsLong();
        helper.assertTrue(energy < 100_000, Component.literal("Expected the robit to use energy when damaged, it has " + energy));
        helper.succeed();
    }
}
