package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mekanism.fabric.shim.AdvancedMenuOpening;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sends the additional client side menu data of NeoForge's {@link net.neoforged.neoforge.common.extensions.IMenuProviderExtension} when opening a menu.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @WrapOperation(method = "openMenu", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void mekanism$sendOpenScreen(ServerGamePacketListenerImpl connection, Packet<?> packet, Operation<Void> original, MenuProvider provider,
          @Local AbstractContainerMenu menu) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        byte[] extraData = FriendlyByteBufUtil.writeCustomData(buffer -> provider.writeClientSideData(self, menu, buffer), self.registryAccess());
        if (extraData.length == 0) {
            original.call(connection, packet);
        } else {
            original.call(connection, ServerPlayNetworking.createClientboundPacket(new AdvancedMenuOpening.Payload(menu.containerId, menu.getType(),
                  provider.getDisplayName(), extraData)));
        }
    }
}
