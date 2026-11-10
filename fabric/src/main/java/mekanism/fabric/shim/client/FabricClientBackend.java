package mekanism.fabric.shim.client;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedExtraModel;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader;
import net.neoforged.neoforge.client.model.standalone.UnbakedStandaloneModel;
import net.neoforged.neoforge.shim.ClientBackend;
import org.jspecify.annotations.Nullable;

/**
 * Fabric implementation of the shim's client services.
 */
public class FabricClientBackend implements ClientBackend {

    /**
     * Fabric keys of the standalone models registered during the last model load.
     */
    private static volatile Map<StandaloneModelKey<?>, ExtraModelKey<?>> standaloneKeys = Map.of();

    /**
     * Registers a model loading plugin that fires NeoForge's standalone model registration event and adds the models as Fabric extra models.
     */
    public static void registerModelLoadingPlugin() {
        ModelLoadingPlugin.register(context -> {
            Map<StandaloneModelKey<?>, UnbakedStandaloneModel<?>> models = new IdentityHashMap<>();
            ModLoader.postEvent(new ModelEvent.RegisterStandalone(models));
            Map<StandaloneModelKey<?>, ExtraModelKey<?>> keys = new IdentityHashMap<>();
            models.forEach((key, model) -> keys.put(key, addStandalone(context, key, model)));
            standaloneKeys = keys;
        });
    }

    private static <T> ExtraModelKey<T> addStandalone(ModelLoadingPlugin.Context context, StandaloneModelKey<?> key, UnbakedStandaloneModel<T> model) {
        ExtraModelKey<T> fabricKey = ExtraModelKey.create(key::getName);
        context.addModel(fabricKey, new UnbakedExtraModel<>() {
            @Override
            public T bake(ModelBaker baker) {
                return model.bake(baker, key::getName);
            }

            @Override
            public void resolveDependencies(ResolvableModel.Resolver resolver) {
                model.resolveDependencies(resolver);
            }
        });
        return fabricKey;
    }

    @Override
    public void registerLayerDefinition(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        ModelLayerRegistry.registerModelLayer(layer, definition::get);
    }

    /**
     * NeoForge's ids of the vanilla HUD layers, and the corresponding Fabric HUD elements.
     */
    private static final Map<Identifier, Identifier> VANILLA_LAYERS = Map.ofEntries(
          Map.entry(VanillaGuiLayers.CAMERA_OVERLAYS, VanillaHudElements.MISC_OVERLAYS),
          Map.entry(VanillaGuiLayers.CROSSHAIR, VanillaHudElements.CROSSHAIR),
          Map.entry(VanillaGuiLayers.HOTBAR, VanillaHudElements.HOTBAR),
          Map.entry(VanillaGuiLayers.PLAYER_HEALTH, VanillaHudElements.HEALTH_BAR),
          Map.entry(VanillaGuiLayers.ARMOR_LEVEL, VanillaHudElements.ARMOR_BAR),
          Map.entry(VanillaGuiLayers.FOOD_LEVEL, VanillaHudElements.FOOD_BAR),
          Map.entry(VanillaGuiLayers.VEHICLE_HEALTH, VanillaHudElements.MOUNT_HEALTH),
          Map.entry(VanillaGuiLayers.AIR_LEVEL, VanillaHudElements.AIR_BAR),
          Map.entry(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, VanillaHudElements.INFO_BAR),
          Map.entry(VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaHudElements.EXPERIENCE_LEVEL),
          Map.entry(VanillaGuiLayers.CONTEXTUAL_INFO_BAR, VanillaHudElements.INFO_BAR_FOREGROUND),
          Map.entry(VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaHudElements.HELD_ITEM_TOOLTIP),
          Map.entry(VanillaGuiLayers.SPECTATOR_TOOLTIP, VanillaHudElements.SPECTATOR_TOOLTIP),
          Map.entry(VanillaGuiLayers.EFFECTS, VanillaHudElements.MOB_EFFECTS),
          Map.entry(VanillaGuiLayers.BOSS_OVERLAY, VanillaHudElements.BOSS_BAR),
          Map.entry(VanillaGuiLayers.SLEEP_OVERLAY, VanillaHudElements.SLEEP),
          Map.entry(VanillaGuiLayers.DEMO_OVERLAY, VanillaHudElements.DEMO_TIMER),
          Map.entry(VanillaGuiLayers.SCOREBOARD_SIDEBAR, VanillaHudElements.SCOREBOARD),
          Map.entry(VanillaGuiLayers.OVERLAY_MESSAGE, VanillaHudElements.OVERLAY_MESSAGE),
          Map.entry(VanillaGuiLayers.TITLE, VanillaHudElements.TITLE_AND_SUBTITLE),
          Map.entry(VanillaGuiLayers.CHAT, VanillaHudElements.CHAT),
          Map.entry(VanillaGuiLayers.TAB_LIST, VanillaHudElements.PLAYER_LIST),
          Map.entry(VanillaGuiLayers.SUBTITLE_OVERLAY, VanillaHudElements.SUBTITLES)
    );

    /**
     * Calls the consumer with NeoForge's id and the corresponding Fabric HUD element id of each vanilla HUD layer.
     */
    static void forEachVanillaLayer(BiConsumer<Identifier, Identifier> consumer) {
        VANILLA_LAYERS.forEach(consumer);
    }

    @Override
    public void registerGuiLayer(Identifier id, GuiLayer layer, @Nullable Identifier relativeTo, boolean above) {
        HudElement element = ClientGameEventBridge.wrapLayer(id, layer);
        if (relativeTo != null) {
            relativeTo = VANILLA_LAYERS.getOrDefault(relativeTo, relativeTo);
        }
        if (relativeTo == null) {
            if (above) {
                HudElementRegistry.addLast(id, element);
            } else {
                HudElementRegistry.addFirst(id, element);
            }
        } else if (above) {
            HudElementRegistry.attachElementAfter(relativeTo, id, element);
        } else {
            HudElementRegistry.attachElementBefore(relativeTo, id, element);
        }
    }

    @Override
    public StandaloneModelLoader.BakedModels getStandaloneModels() {
        Map<StandaloneModelKey<?>, Object> baked = new IdentityHashMap<>();
        var modelManager = Minecraft.getInstance().getModelManager();
        standaloneKeys.forEach((key, fabricKey) -> {
            Object model = modelManager.getModel(fabricKey);
            if (model != null) {
                baked.put(key, model);
            }
        });
        return new StandaloneModelLoader.BakedModels(baked);
    }
}
