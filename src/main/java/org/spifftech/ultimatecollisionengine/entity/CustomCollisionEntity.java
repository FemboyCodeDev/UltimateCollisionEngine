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


public class CustomCollisionEntity extends MobEntity {

    public CustomCollisionEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    private static final net.minecraft.entity.EntityDimensions CUSTOM_DIMENSIONS =
            net.minecraft.entity.EntityDimensions.changing(2.0F, 2.0F); // Width, Height



    // 2. Define the basic attributes (Health, Movement Speed)
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

    // Helper to create the OBB from the entity's AABB.
    private SatCollisionHelper.OBB createObbFromEntityBox() {
        Box box = this.getBoundingBox();
        Vec3d center = box.getCenter();

        // Half extents calculated from the Box
        // Using getXLength/2.0 is cleaner, but this ensures the calculation is clear.
        Vec3d halfExtents = new Vec3d(
                (box.maxX - box.minX) / (2.0*2.0),
                (box.maxY - box.minY) / (2.0*2.0),
                (box.maxZ - box.minZ) / (2.0*2.0)
        );

        // Non-rotated axes (identity matrix) - **Only valid if entity never rotates!**
        Vec3d[] axes = new Vec3d[] {
                new Vec3d(1.0, 0.0, 0.0), // U1 (Local X)
                new Vec3d(0.0, 1.0, 0.0), // U2 (Local Y)
                new Vec3d(0.0, 0.0, 1.0)  // U3 (Local Z)
        };

        return new SatCollisionHelper.OBB(center, axes, halfExtents);
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


    // 🌟 Implement the custom collision logic in the tick loop.
    @Override
    public void tick() {
        System.out.println("Ticking Custom Collision Entity");
        super.tick();
        System.out.println("Ticking Custom Collision Entity 1");

        if (!this.getWorld().isClient) {
            // Step 1: Define a search area around the entity
            // Expand the box slightly to catch entities just bumping the edge.
            Box searchBox = this.getBoundingBox().expand(0.1);

            // Get a list of potential entities in the search area
            List<Entity> nearbyEntities = this.getWorld().getOtherEntities(
                    this,
                    searchBox,
                    (entity) -> entity.isAlive()
                            && !(entity instanceof CustomCollisionEntity) // Don't check against self-type
            );

            SatCollisionHelper.OBB thisObb = this.createObbFromEntityBox();

            // Step 2 & 3: Check intersection and resolve collision for each
            for (Entity other : nearbyEntities) {
                Box otherAabb = other.getBoundingBox();

                if (SatCollisionHelper.obbAabbIntersects(thisObb, otherAabb)) {

                    System.out.println("Intersecting Custom Collision Entity");

                    // Collision found! Apply push logic to the 'other' entity.
                    Vec3d centerVector = other.getPos().subtract(this.getPos());

                    // Calculate penetration depth (Overlap) for X and Z axes
                    double overlapX = (thisObb.halfExtents.getX() + (otherAabb.getXLength() / 2.0)) - Math.abs(centerVector.getX());
                    double overlapZ = (thisObb.halfExtents.getZ() + (otherAabb.getZLength() / 2.0)) - Math.abs(centerVector.getZ());

                    double pushFactor = 0.5; // Controls the strength of the push
                    pushFactor = 0.0;

                    // Determine MTV axis and apply repulsion
                    if (overlapX < overlapZ) {
                        // Push along X axis (axis with the least overlap)
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