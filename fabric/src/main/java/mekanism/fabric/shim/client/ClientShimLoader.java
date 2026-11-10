package mekanism.fabric.shim.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import mekanism.fabric.shim.FabricModContainer;
import mekanism.fabric.shim.ShimModLoader;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.AtlasRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ExtractItemDecorationsCallback;
import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterFeatureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.ClientExtensionsManager;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import java.util.function.Function;

/**
 * Fires NeoForge's client mod loading events on Fabric, at client initialization, and maps them onto Fabric's client APIs.
 * <p>
 * Events that need to be fired at a specific point of the client's lifecycle (such as during resource reloads) are fired by mixins instead.
 */
public final class ClientShimLoader {

    private ClientShimLoader() {
    }

    public static void init() {
        ClientExtensionsManager.init();

        List<KeyMapping.Category> categories = new ArrayList<>();
        RegisterKeyMappingsEvent keyMappingsEvent = new RegisterKeyMappingsEvent(KeyMappingHelper::registerKeyMapping);
        ModLoader.postEvent(keyMappingsEvent);
        keyMappingsEvent.sortAndStoreCategories(categories);
        for (KeyMapping.Category category : categories) {
            KeyMapping.Category.register(category.id());
        }

        Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> screens = new LinkedHashMap<>();
        ModLoader.postEvent(new RegisterMenuScreensEvent(screens));
        MenuScreens.SCREENS.putAll(screens);

        ModLoader.postEvent(new EntityRenderersEvent.RegisterLayerDefinitions());
        ModLoader.postEvent(new EntityRenderersEvent.RegisterRenderers());
        ModLoader.postEvent(new RegisterGuiLayersEvent());

        FluidContainerItemModel.register();
        BlockStateModelBridge.register();
        ModLoader.postEvent(new RegisterItemModelsEvent(ItemModels.ID_MAPPER));
        ModLoader.postEvent(new RegisterSpecialModelRendererEvent(SpecialModelRenderers.ID_MAPPER));
        ModLoader.postEvent(new RegisterConditionalItemModelPropertyEvent(ConditionalItemModelProperties.ID_MAPPER));
        ModLoader.postEvent(new RegisterSelectItemModelPropertyEvent(SelectItemModelProperties.ID_MAPPER));
        ModLoader.postEvent(new RegisterColorHandlersEvent.ItemTintSources(ItemTintSources.ID_MAPPER));

        Map<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> particleGroups = new LinkedHashMap<>();
        ModLoader.postEvent(new RegisterParticleGroupsEvent(particleGroups, new ArrayList<>()));
        particleGroups.forEach(ParticleGroupRegistry::register);

        List<PictureInPictureRendererRegistration<?>> pipRenderers = new ArrayList<>();
        ModLoader.postEvent(new RegisterPictureInPictureRenderersEvent(pipRenderers));
        for (PictureInPictureRendererRegistration<?> registration : pipRenderers) {
            PictureInPictureRendererRegistry.register(context -> registration.factory().get());
        }

        //Note: Fired on the game bus by NeoForge. The map is only used to collect the renderers, which are then registered with Fabric
        NeoForge.EVENT_BUS.post(new RegisterFeatureRenderersEvent(new FeatureRendererMap() {
            @Override
            public <S extends SubmitNode> void put(FeatureRendererType<S> type, FeatureRenderer<S> renderer) {
                super.put(type, renderer);
                FeatureRendererRegistry.register(type, () -> renderer);
            }
        }));

        SequencedMap<Identifier, AtlasManager.AtlasConfig> atlases = new LinkedHashMap<>();
        ModLoader.postEvent(new RegisterTextureAtlasesEvent(atlases));
        atlases.values().forEach(AtlasRegistry::register);

        Map<Item, List<IItemDecorator>> decorators = new IdentityHashMap<>();
        ModLoader.postEvent(new RegisterItemDecorationsEvent(decorators));
        if (!decorators.isEmpty()) {
            ExtractItemDecorationsCallback.EVENT.register((graphics, font, stack, x, y) -> {
                List<IItemDecorator> itemDecorators = decorators.get(stack.getItem());
                if (itemDecorators != null) {
                    for (IItemDecorator decorator : itemDecorators) {
                        decorator.render(graphics, font, stack, x, y);
                    }
                }
            });
        }

        ModLoader.postEvent(new RegisterRenderStateModifiersEvent());

        AddClientReloadListenersEvent reloadListenersEvent = new AddClientReloadListenersEvent();
        ModLoader.postEvent(reloadListenersEvent);
        reloadListenersEvent.getListeners().forEach((id, listener) -> ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id, listener));

        FabricClientBackend.registerModelLoadingPlugin();

        for (FabricModContainer container : ShimModLoader.getContainers()) {
            container.getEventBus().post(new FMLClientSetupEvent(container));
        }
        ShimModLoader.finishClientLoading();
    }
}
