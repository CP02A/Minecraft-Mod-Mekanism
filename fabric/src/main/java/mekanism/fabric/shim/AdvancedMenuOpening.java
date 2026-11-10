package mekanism.fabric.shim;

import mekanism.fabric.mixin.hooks.MenuTypeAccessor;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import org.jspecify.annotations.Nullable;

/**
 * NeoForge's support for sending additional data (written by {@link net.neoforged.neoforge.common.extensions.IMenuProviderExtension#writeClientSideData}) to
 * the client when opening a menu, which is passed to {@link IContainerFactory#create(int, Inventory, RegistryFriendlyByteBuf)}.
 */
public final class AdvancedMenuOpening {

    private AdvancedMenuOpening() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(Payload.TYPE, Payload.STREAM_CODEC, 8 * 1024 * 1024);
    }

    /**
     * NeoForge's {@code IMenuTypeExtension#create(int, Inventory, RegistryFriendlyByteBuf)}.
     */
    @Nullable
    public static <T extends AbstractContainerMenu> T create(MenuType<T> type, int windowId, Inventory inventory, RegistryFriendlyByteBuf data) {
        @SuppressWarnings("unchecked")
        MenuType.MenuSupplier<T> constructor = ((MenuTypeAccessor<T>) type).mekanism$getConstructor();
        if (constructor instanceof IContainerFactory<T> factory) {
            return factory.create(windowId, inventory, data);
        }
        return type.create(windowId, inventory);
    }

    public record Payload(int windowId, MenuType<?> menuType, Component name, byte[] additionalData) implements CustomPacketPayload {

        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("neoforge", "advanced_open_screen"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
              ByteBufCodecs.VAR_INT, Payload::windowId,
              ByteBufCodecs.registry(Registries.MENU), Payload::menuType,
              ComponentSerialization.STREAM_CODEC, Payload::name,
              ByteBufCodecs.BYTE_ARRAY, Payload::additionalData,
              Payload::new
        );

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }
}
