package mekanism.fabric.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's client level load event and model data manager.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Unique
    private final ModelDataManager mekanism$modelDataManager = new ModelDataManager((ClientLevel) (Object) this);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mekanism$load(CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Load((ClientLevel) (Object) this));
    }

    /**
     * Overrides {@code ILevelExtension#getModelDataManager()}.
     */
    public ModelDataManager getModelDataManager() {
        return mekanism$modelDataManager;
    }

    /**
     * Overrides {@code IBlockGetterExtension#getModelData(BlockPos)}.
     */
    public ModelData getModelData(BlockPos pos) {
        return mekanism$modelDataManager.getAt(pos);
    }
}
