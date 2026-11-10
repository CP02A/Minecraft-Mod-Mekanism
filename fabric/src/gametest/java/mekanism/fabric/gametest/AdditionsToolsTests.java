package mekanism.fabric.gametest;

import java.util.concurrent.atomic.AtomicBoolean;
import mekanism.additions.common.entity.baby.BabyType;
import mekanism.additions.common.registries.AdditionsBlocks;
import mekanism.additions.common.registries.AdditionsEntityTypes;
import mekanism.api.text.EnumColor;
import mekanism.tools.common.registries.ToolsItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Tests for Mekanism: Additions and Mekanism: Tools on Fabric.
 */
public class AdditionsToolsTests {

    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final AtomicBoolean FINALIZE_SPAWN_FIRED = new AtomicBoolean();

    static {
        NeoForge.EVENT_BUS.addListener(FinalizeSpawnEvent.class, event -> {
            if (event.getEntity().getType() == EntityTypes.ZOMBIE) {
                FINALIZE_SPAWN_FIRED.set(true);
            }
        });
    }

    @GameTest
    public void babySkeletonSpawns(GameTestHelper helper) {
        Monster baby = helper.spawn(AdditionsEntityTypes.<Monster>getBaby(BabyType.SKELETON).get(), POS, EntitySpawnReason.SPAWN_ITEM_USE);
        helper.assertTrue(baby.isBaby(), Component.literal("The baby skeleton should be a baby"));
        helper.assertTrue(baby.getMaxHealth() > 0, Component.literal("The baby skeleton should have its attributes"));
        //Let it tick a bit to make sure its AI works
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(baby.isAlive(), Component.literal("The baby skeleton should still be alive"));
            helper.succeed();
        });
    }

    @GameTest
    public void plasticFenceGateOpens(GameTestHelper helper) {
        helper.setBlock(POS, AdditionsBlocks.PLASTIC_FENCE_GATES.pick(EnumColor.RED).defaultState());
        helper.useBlock(POS, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertBlockProperty(POS, FenceGateBlock.OPEN, true);
        helper.succeed();
    }

    @GameTest
    public void paxelMakesPaths(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.GRASS_BLOCK);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike) ToolsItems.STONE_PAXEL));
        BlockPos absolute = helper.absolutePos(POS);
        //Grass can't be stripped, so the paxel falls back to its shovel behavior (BlockTransformer#transformBlock with the shovel's transformer)
        helper.useBlock(POS, player, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        helper.assertBlockPresent(Blocks.DIRT_PATH, POS);
        helper.succeed();
    }

    @GameTest
    public void finalizeSpawnEventFires(GameTestHelper helper) {
        FINALIZE_SPAWN_FIRED.set(false);
        Mob zombie = helper.spawn(EntityTypes.ZOMBIE, POS);
        zombie.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.SPAWN_ITEM_USE, null);
        helper.assertTrue(FINALIZE_SPAWN_FIRED.get(), Component.literal("FinalizeSpawnEvent should fire when a mob's spawn is finalized"));
        helper.succeed();
    }
}
