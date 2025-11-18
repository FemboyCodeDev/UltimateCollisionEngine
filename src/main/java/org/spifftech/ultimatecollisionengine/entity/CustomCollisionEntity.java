package org.spifftech.ultimatecollisionengine.entity;


import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.spifftech.ultimatecollisionengine.boxcollision.SatCollisionHelper;


public class CustomCollisionEntity extends MobEntity {

    public CustomCollisionEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    // 2. Define the basic attributes (Health, Movement Speed)
    public static DefaultAttributeContainer.Builder createCustomCollisionEntityAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initDataTracker() {

        super.initDataTracker();
        // Add any of your *own* custom tracked data here if you have any.
        // e.g., this.dataTracker.startTracking(MY_CUSTOM_FIELD, defaultValue);

    }
    // 3. Define simple AI goals (wandering and looking around)
    @Override
    protected void initGoals() {
        //this.goalSelector.add(1, new WanderAroundFarGoal(this, (double)1.0));
        //this.goalSelector.add(2, new LookAroundGoal(this));
    }

    private SatCollisionHelper.OBB createObbFromEntityBox() {
        Box box = this.getBoundingBox();
        Vec3d center = box.getCenter();

        // Half extents calculated from the Box
        Vec3d halfExtents = new Vec3d(
                (box.maxX - box.minX) / (2.0*2.0),
                (box.maxY - box.minY) / (2.0*2.0),
                (box.maxZ - box.minZ) / (2.0*2.0)
        );

        // Non-rotated axes (identity matrix)
        Vec3d[] axes = new Vec3d[] {
                new Vec3d(1.0, 0.0, 0.0), // U1 (Local X)
                new Vec3d(0.0, 1.0, 0.0), // U2 (Local Y)
                new Vec3d(0.0, 0.0, 1.0)  // U3 (Local Z)
        };

        return new SatCollisionHelper.OBB(center, axes, halfExtents);
    }


    // 🌟 Override the method that determines if THIS entity can be pushed.
    // Returning false makes it impossible for other entities (like players)
    // to apply forces that would make it move.
    @Override
    public boolean isPushable() {
        return true;
        //return false;
    }
    @Override
    public boolean isCollidable() {
        //return false;
        return true; // Ensures the game sees a solid boundary
    }

    // 🌟 Override the pushAwayFrom method.
    // This is called when another entity (other) bumps into THIS entity.
    @Override
    public void pushAwayFrom(Entity other) {


        System.out.println("Collision 1");
        if (!this.getWorld().isClient) {
        // Output debug


        // --- 1. Define the OBB and AABB ---
        SatCollisionHelper.OBB thisObb = this.createObbFromEntityBox(); // The rigid object
        Box otherAabb = other.getBoundingBox();       // The colliding object

        // --- 2. Check for intersection and find the MTV (Separating Axis Theorem) ---

        // This is the ideal place to call an advanced SAT function from SatCollisionHelper
        // that returns the Minimum Translation Vector (MTV) instead of just a boolean.
        // For demonstration, we'll assume a helper function exists:

        // Vec3d mtv = SatCollisionHelper.findObbAabbMtv(thisObb, otherAabb);

        // Since that function doesn't exist in the provided SAT code,
        // we'll revert to a simpler method to find the push direction/depth
        // based on the center overlap, but only if an intersection actually occurs.

        if (SatCollisionHelper.obbAabbIntersects(thisObb, otherAabb)) {
            System.out.println("Collision 2");
            // --- 3. Simple Repulsion based on center overlap ---

            // The vector from the center of THIS (the OBB) to the center of OTHER (the AABB)
            Vec3d centerVector = other.getPos().subtract(this.getPos());

            // Find the penetration depth on the X and Z axes (Y is often handled by jump/fall)
            double overlapX = (thisObb.halfExtents.getX() + (otherAabb.getXLength() / 2.0)) - Math.abs(centerVector.getX());
            double overlapZ = (thisObb.halfExtents.getZ() + (otherAabb.getZLength() / 2.0)) - Math.abs(centerVector.getZ());

            double pushFactor = 0.5; // Controls the strength of the push
            //pushFactor = 0.0;

            // Determine which axis has the LEAST overlap (this is the MTV direction)
            if (overlapX < overlapZ) {
                // Collision is shallower in X, so push along X
                double pushX = centerVector.getX() > 0 ? overlapX + 0.001 : -(overlapX + 0.001);

                // Set the velocity or movement directly on the other entity
                other.addVelocity(pushX * pushFactor, 0.0, 0.0);

            } else {
                // Collision is shallower in Z, so push along Z
                double pushZ = centerVector.getZ() > 0 ? overlapZ + 0.001 : -(overlapZ + 0.001);

                // Set the velocity or movement directly on the other entity
                other.addVelocity(0.0, 0.0, pushZ * pushFactor);
            }

            // To prevent the player/mob from constantly trying to walk into the object:
            other.velocityDirty = true;
        }

        // IMPORTANT: Do NOT call super.pushAwayFrom(other) to avoid default Minecraft physics.
        // Also avoid other.pushAwayFrom(this).
    }}




}
