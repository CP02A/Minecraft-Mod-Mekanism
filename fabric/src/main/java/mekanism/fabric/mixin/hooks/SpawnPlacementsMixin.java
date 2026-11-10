package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fires NeoForge's {@link MobSpawnEvent.SpawnPlacementCheck} with the result of the spawn placement predicate (not for entities that can't spawn in peaceful,
 * like NeoForge).
 */
@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {

    @ModifyReturnValue(method = "checkSpawnRules", at = @At("RETURN"))
    private static boolean mekanism$spawnPlacementCheck(boolean result, EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason spawnReason,
          BlockPos pos, RandomSource random) {
        if (!type.isAllowedInPeaceful() && level.getDifficulty() == Difficulty.PEACEFUL) {
            return result;
        }
        return NeoForge.EVENT_BUS.post(new MobSpawnEvent.SpawnPlacementCheck(type, level, spawnReason, pos, random, result)).getPlacementCheckResult();
    }
}
