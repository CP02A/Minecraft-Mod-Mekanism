package mekanism.fabric.shim.client;

import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import java.util.List;
import mekanism.fabric.shim.AdvancedMenuOpening;
import mekanism.fabric.shim.RecipeContentSync;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;

/**
 * Fires NeoForge's client game events from the equivalent Fabric API events. Events without a Fabric equivalent are fired by the mixins in
 * {@code mekanism.fabric.mixin.client}.
 */
public final class ClientGameEventBridge {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientGameEventBridge() {
    }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientHooks.fireClientTickPre());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientHooks.fireClientTickPost());
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, level)));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> entity.onRemovedFromLevel());
        //NeoForge's ModelDataManager drops the model data of unloaded chunks
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            var modelDataManager = level.getModelDataManager();
            if (modelDataManager != null) {
                modelDataManager.onChunkUnload(chunk.getPos());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(RecipeContentSync.Payload.TYPE, (payload, context) ->
              NeoForge.EVENT_BUS.post(new RecipesReceivedEvent(payload.recipeTypes(), RecipeContentSync.createClientMap(payload.recipes()))));

        ClientPlayNetworking.registerGlobalReceiver(AdvancedMenuOpening.Payload.TYPE, (payload, context) -> openMenuScreen(payload, context.client()));

        LevelRenderEvents.BEFORE_GIZMOS.register(context -> NeoForge.EVENT_BUS.post(new SubmitCustomGeometryEvent(context.levelState(),
              context.submitNodeCollector(), context.poseStack(), List.of())));
        BlockOutlineBridge.register();

        //Fire the HUD layer events for the vanilla layers
        FabricClientBackend.forEachVanillaLayer((neoId, fabricId) -> HudElementRegistry.replaceElement(fabricId, element -> wrapLayer(neoId, element::extractRenderState)));
    }

    /**
     * Opens a menu screen with additional data from the server (NeoForge's {@code ClientPayloadHandler#handle(AdvancedOpenScreenPayload)}).
     */
    private static void openMenuScreen(AdvancedMenuOpening.Payload payload, Minecraft client) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.additionalData()), client.player.registryAccess());
        try {
            createMenuScreen(client, payload.name(), payload.menuType(), payload.windowId(), buf);
        } catch (Throwable t) {
            LOGGER.error("Failed to handle advanced open screen from server.", t);
            client.getConnection().getConnection().disconnect(Component.translatable("neoforge.network.advanced_open_screen.failed", t.toString()));
        } finally {
            buf.release();
        }
    }

    private static <T extends AbstractContainerMenu> void createMenuScreen(Minecraft client, Component name, MenuType<T> menuType, int windowId,
          RegistryFriendlyByteBuf buf) {
        @SuppressWarnings("unchecked")
        MenuScreens.ScreenConstructor<T, ?> factory = (MenuScreens.ScreenConstructor<T, ?>) MenuScreens.SCREENS.get(menuType);
        if (factory == null) {
            LOGGER.warn("Failed to create screen for menu type: {}", BuiltInRegistries.MENU.getKey(menuType));
            return;
        }
        Inventory inventory = client.player.getInventory();
        Screen screen = factory.create(AdvancedMenuOpening.create(menuType, windowId, inventory, buf), inventory, name);
        client.player.containerMenu = ((MenuAccess<?>) screen).getMenu();
        client.gui.setScreen(screen);
    }

    /**
     * Wraps a HUD layer to fire NeoForge's {@link RenderGuiLayerEvent.Pre} and {@link RenderGuiLayerEvent.Post} around it.
     */
    public static HudElement wrapLayer(Identifier name, GuiLayer layer) {
        return (graphics, deltaTracker) -> {
            if (!NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(graphics, deltaTracker, name, layer)).isCanceled()) {
                layer.render(graphics, deltaTracker);
                NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Post(graphics, deltaTracker, name, layer));
            }
        };
    }
}
