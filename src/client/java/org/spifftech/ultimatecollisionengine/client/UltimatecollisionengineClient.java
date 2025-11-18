package org.spifftech.ultimatecollisionengine.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.spifftech.ultimatecollisionengine.Ultimatecollisionengine;

public class UltimatecollisionengineClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1. Link the custom entity to its renderer
        EntityRendererRegistry.register(
                Ultimatecollisionengine.CUSTOM_COLLISION_ENTITY, // <-- This is the custom entity type that was summoned
                (context) -> {
                    // 2. Return an instance of the class that handles the rendering logic
                    return new CustomCollisionEntityRenderer(context);
                }
        );
    }
}
