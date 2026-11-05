package com.nivek.themaskedmorte.client;

import com.nivek.themaskedmorte.ModItems;
import com.nivek.themaskedmorte.net.MorphPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

public class TheMaskedMorteClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(MaskArmorRenderer.LAYER, MaskArmorRenderer::createModel);
        ArmorRenderer.register(new MaskArmorRenderer(), ModItems.MASK);

        ClientPlayNetworking.registerGlobalReceiver(MorphPayload.ID,
                (payload, context) -> MorphClient.handle(payload));
        ClientTickEvents.END_CLIENT_TICK.register(MorphClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MorphClient.clear());
    }
}
