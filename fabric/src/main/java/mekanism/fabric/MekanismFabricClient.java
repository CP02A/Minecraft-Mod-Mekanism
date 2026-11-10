package mekanism.fabric;

import mekanism.fabric.shim.DataMapLoader;
import mekanism.fabric.shim.client.ClientShimLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class MekanismFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DataMapLoader.SyncPayload.TYPE, (payload, context) ->
              payload.apply(context.player().registryAccess(), context.client().hasSingleplayerServer()));
        ClientShimLoader.init();
    }
}
