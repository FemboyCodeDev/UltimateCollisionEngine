package org.spifftech.ultimatecollisionengine.entity;


import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.spifftech.ultimatecollisionengine.boxcollision.SatCollisionHelper;

import java.util.List;

/**
 * An entity designed to act as a custom collision boundary (e.g., a wall or prop).
 * It uses a fixed, internal OBB that is NOT derived from its vanilla AABB/Dimensions.
 * This entity is configured to prevent all vanilla movement and collision resolution.
 */
public class CustomCollisionEntity extends MobEntity {

    // --- 1. Custom OBB Definition (Independent of vanilla EntityDimensions) ---

    // Define the custom collision box. This entity will be treated as this box.
    // NOTE: The center of the OBB is relative to the entity's position, so halfExtents are fixed.
    private static final SatCollisionHelper.OBB CustomCollisionBox;

    static {
        // Define the half extents (half-width, half-height, half-depth)
        Vec3d halfExtents = new Vec3d(1.0, 1.0, 1.0); // Creates a 2x2x2 collision box

        // Non-rotated axes (identity matrix) - **Only valid if entity never rotates!**
        // These represent the local X, Y, Z axes of the OBB.
        Vec3d[] axes = new Vec3d[] {
                new Vec3d(1.0, 0.0, 0.0), // U1 (Local X)
                new Vec3d(0.0, 1.0, 0.0), // U2 (Local Y)
                new Vec3d(0.0, 0.0, 1.0)  // U3 (Local Z)
        };

        // The center is set to (0, 0, 0) initially, as it's relative to the entity's position.
        CustomCollisionBox = new SatCollisionHelper.OBB(Vec3d.ZERO, axes, halfExtents);
    }

    // Vanilla dimensions are set small, as they only affect world loading and entity hitbox/selection.
    private static final net.minecraft.entity.EntityDimensions VANILLA_DIMENSIONS =
            net.minecraft.entity.EntityDimensions.changing(0.1F, 0.1F); // Minimized Width/Height

    // Override the getter to use the minimal dimensions
    @Override
    public net.minecraft.entity.EntityDimensions getDimensions(net.minecraft.entity.EntityPose pose) {
        return VANILLA_DIMENSIONS;
    }

    // --- 2. Constructor and Attributes ---

    public CustomCollisionEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    // Define the basic attributes (Health, Movement Speed)
    public static DefaultAttributeContainer.Builder createCustomCollisionEntityAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0); // Entity should not move
    }

    @Override
    protected void initGoals() {
        // CRITICAL: Leave empty! No goals means no vanilla movement/push attempts.
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
    }

    // --- 3. OBB Creation and Collision Overrides ---

    /**
     * Helper to create the OBB from the entity's *fixed* internal OBB definition.
     * The OBB is translated to the entity's current world position.
     */
    private SatCollisionHelper.OBB getCurrentObb() {
        // The OBB center is the entity's current position (getPos())
        Vec3d center = this.getPos();

        // Create a *new* OBB translated to the current world position
        return new SatCollisionHelper.OBB(
                center,
                CustomCollisionBox.axes,
                CustomCollisionBox.halfExtents
        );
    }


    // 🌟 Override the method that determines if THIS entity can be pushed.
    @Override
    public boolean isPushable() {
        return false; // Prevents THIS entity from being moved by others.
    }

    // 🌟 This is the KEY change: prevents vanilla collision resolution.
    @Override
    public boolean isCollidable() {
        return false; // Ignores the default AABB check for *collision*.
    }

    // 🌟 Empty pushAwayFrom: ensures no vanilla push logic is executed.
    @Override
    public void pushAwayFrom(Entity other) {
        // CRITICAL: Do nothing here. All collision is handled in tick().
    }


    // --- 4. Custom Collision Logic ---

    // 🌟 Implement the custom collision logic in the tick loop.
    @Override
    public void tick() {
        //System.out.println("Ticking Custom Collision Entity");
        super.tick();

        if (!this.getWorld().isClient) {
            // Step 1: Define a search area around the entity
            // Base the search box on the custom OBB's size for efficiency.
            // OBB max dimension = Math.max(x,y,z) * 2. Add a small buffer (0.1).
            double maxHalfExtent = Math.max(CustomCollisionBox.halfExtents.getX(),
                    Math.max(CustomCollisionBox.halfExtents.getY(),
                            CustomCollisionBox.halfExtents.getZ()));

            Box searchBox = this.getBoundingBox().expand(maxHalfExtent + 0.1);

            // Get a list of potential entities in the search area
            List<Entity> nearbyEntities = this.getWorld().getOtherEntities(
                    this,
                    searchBox,
                    (entity) -> entity.isAlive()
                            && !(entity instanceof CustomCollisionEntity) // Don't check against self-type
            );

            // Get the world-space OBB for the current tick
            SatCollisionHelper.OBB thisObb = this.getCurrentObb();

            // Step 2 & 3: Check intersection and resolve collision for each
            for (Entity other : nearbyEntities) {
                Box otherAabb = other.getBoundingBox();

                // Check for OBB-AABB collision
                if (SatCollisionHelper.obbAabbIntersects(thisObb, otherAabb)) {

                    System.out.println("Collision");

                    // Collision found! Apply push logic to the 'other' entity.
                    Vec3d centerVector = other.getPos().subtract(this.getPos());

                    // --- Collision Resolution using simple AABB overlap logic (for demonstration) ---
                    // This simple resolution is based on *AABB* overlap, not true OBB-AABB Minimum Translation Vector (MTV).
                    // For true OBB-AABB MTV, the SatCollisionHelper should calculate and return the MTV vector.

                    // Calculate penetration depth (Overlap) for X and Z axes
                    double overlapX = (thisObb.halfExtents.getX() + (otherAabb.getXLength() / 2.0)) - Math.abs(centerVector.getX());
                    double overlapZ = (thisObb.halfExtents.getZ() + (otherAabb.getZLength() / 2.0)) - Math.abs(centerVector.getZ());

                    double pushFactor = 5.0; // Controls the strength of the push

                    // Determine MTV axis and apply repulsion
                    if (overlapX < overlapZ) {
                        // Push along X axis (axis with the least overlap)
                        // Add a small epsilon (0.001) to ensure separation
                        double pushX = centerVector.getX() > 0 ? overlapX + 0.001 : -(overlapX + 0.001);
                        other.addVelocity(pushX * pushFactor, 0.0, 0.0);
                    } else {
                        // Push along Z axis
                        double pushZ = centerVector.getZ() > 0 ? overlapZ + 0.001 : -(overlapZ + 0.001);
                        other.addVelocity(0.0, 0.0, pushZ * pushFactor);
                    }

                    other.velocityDirty = true;
                }
            }
        }
    }
}