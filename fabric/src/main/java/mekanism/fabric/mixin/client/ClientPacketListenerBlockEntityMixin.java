package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IBlockEntityExtension#onDataPacket}: block entity data packets are handled by the block entity (Mekanism reads its reduced update
 * tag there) instead of being loaded like saved data, like NeoForge's patch of {@link ClientPacketListener#handleBlockEntityData}.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerBlockEntityMixin {

    @WrapOperation(method = "lambda$handleBlockEntityData$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private void mekanism$onDataPacket(BlockEntity blockEntity, ValueInput input, Operation<Void> original) {
        blockEntity.onDataPacket(((ClientPacketListener) (Object) this).getConnection(), input);
    }
}
