package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import org.jspecify.annotations.Nullable;
import mekanism.fabric.shim.ServerHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * NeoForge's FinalizeSpawnEvent. NeoForge fires it before the call sites of {@link Mob#finalizeSpawn}, wrapping the method itself is equivalent (overrides
 * call the super method, which only happens once per spawn), and the spawn cancellation flag NeoForge patches into {@link Mob}.
 */
@Mixin(Mob.class)
public abstract class MobMixin implements ServerHooks.SpawnCancellable {

    @Unique
    private boolean mekanism$spawnCancelled;

    @Override
    public boolean mekanism$isSpawnCancelled() {
        return mekanism$spawnCancelled;
    }

    @WrapMethod(method = "finalizeSpawn")
    private @Nullable SpawnGroupData mekanism$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason,
          @Nullable SpawnGroupData groupData, Operation<SpawnGroupData> original) {
        Mob mob = (Mob) (Object) this;
        FinalizeSpawnEvent event = NeoForge.EVENT_BUS.post(new FinalizeSpawnEvent(mob, level, mob.getX(), mob.getY(), mob.getZ(), difficulty, spawnReason,
              groupData, null));
        if (event.isSpawnCancelled()) {
            mekanism$spawnCancelled = true;
        }
        if (event.isCanceled()) {
            return null;
        }
        return original.call(level, event.getDifficulty(), spawnReason, event.getSpawnData());
    }
}
