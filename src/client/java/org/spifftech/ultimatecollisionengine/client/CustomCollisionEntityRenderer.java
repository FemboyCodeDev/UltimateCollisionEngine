package org.spifftech.ultimatecollisionengine.client;

import org.spifftech.ultimatecollisionengine.entity.CustomCollisionEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.RenderLayers; // NEW: Needed to get the block consumer

public class CustomCollisionEntityRenderer extends EntityRenderer<CustomCollisionEntity> {

    private final BlockRenderManager blockRenderManager;

    public CustomCollisionEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
    }

    @Override
    public void render(CustomCollisionEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // 1. Light calculation (still needed)
        int combinedLight = WorldRenderer.getLightmapCoordinates(entity.getWorld(), entity.getBlockPos());
        int overlay = 0;

        // 2. Scale it down a bit
        matrices.scale(1f, 1f, 1f);

        // Calculate an independent, time-based rotation angle
        // (entity.age is in ticks, add tickDelta for smooth interpolation between ticks)
        // Here, 5.0f controls the speed (5 degrees per tick)
        float spinAngle = (entity.age + tickDelta) * 5.0f;

        // **IMPORTANT:** Translate to center the block.
        // Entities render from the middle, blocks from a corner. We move the origin to the block's corner.
        //matrices.translate(0.5, 0.5, 0.5);
        matricesatrices.translate(0.0, 0.5, 0.0);

        // --- 🔄 APPLY INDEPENDENT ROTATION ---
        // Apply the time-based rotation around the Y-axis (up/down).
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(spinAngle));
        matrices.translate(-0.5, -0.5, -0.5);
        // --- 🔄 END ROTATION ---

        // --- 🧱 Start Block Rendering Logic ---

        net.minecraft.block.BlockState blockStateToRender = Blocks.STONE.getDefaultState();

        net.minecraft.client.render.VertexConsumer blockConsumer =
                vertexConsumers.getBuffer(RenderLayers.getMovingBlockLayer(blockStateToRender));

        this.blockRenderManager.renderBlock(
                blockStateToRender,
                entity.getBlockPos(),
                entity.getWorld(),
                matrices,
                blockConsumer,
                false,
                entity.getRandom()
        );

        // --- 🧱 End Block Rendering Logic ---

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(CustomCollisionEntity entity) {
        return new Identifier("minecraft", "textures/entity/stone.png");
    }
}