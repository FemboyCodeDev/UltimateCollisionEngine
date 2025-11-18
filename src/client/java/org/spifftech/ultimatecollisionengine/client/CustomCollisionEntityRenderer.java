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

        // **IMPORTANT:** Translate to center the block.
        // Entities are usually rendered at the origin (0,0,0) of the matrix stack,
        // but blocks are often rendered from their corner.
        matrices.translate(-0.5, -0.5, -0.5);
        matrices.translate(0, 0.5, 0);


        // --- 🧱 Start Block Rendering Logic ---

        net.minecraft.block.BlockState blockStateToRender = Blocks.STONE.getDefaultState();

        // Get the specific VertexConsumer for the block's texture layer
        net.minecraft.client.render.VertexConsumer blockConsumer =
                vertexConsumers.getBuffer(RenderLayers.getMovingBlockLayer(blockStateToRender));

        // Call the REQUIRED method signature:
        this.blockRenderManager.renderBlock(
                // 1. net.minecraft.block.BlockState state
                blockStateToRender,
                // 2. net.minecraft.util.math.BlockPos pos
                entity.getBlockPos(), // Use the entity's current block position
                // 3. net.minecraft.world.BlockRenderView world
                entity.getWorld(), // Use the entity's world, which implements BlockRenderView
                // 4. net.minecraft.client.util.math.MatrixStack matrices
                matrices,
                // 5. net.minecraft.client.render.VertexConsumer vertexConsumer
                blockConsumer, // The consumer we retrieved above
                // 6. boolean cull
                false, // We generally don't want to cull faces when rendering a block on an entity
                // 7. net.minecraft.util.math.random.Random random
                entity.getRandom() // Use the entity's random instance for consistency
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