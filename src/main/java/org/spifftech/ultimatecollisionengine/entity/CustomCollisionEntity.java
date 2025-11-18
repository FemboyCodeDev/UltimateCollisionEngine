package org.spifftech.ultimatecollisionengine.entity;


import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;


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


    // 🌟 Override the method that determines if THIS entity can be pushed.
    // Returning false makes it impossible for other entities (like players)
    // to apply forces that would make it move.
    @Override
    public boolean isPushable() {
        return false;
    }
    @Override
    public boolean isCollidable() {
        return true; // Ensures the game sees a solid boundary
    }

    // 🌟 Override the pushAwayFrom method.
    // This is called when another entity (other) bumps into THIS entity.
    @Override

    public void pushAwayFrom(Entity other) {
        if (!this.getWorld().isClient) {

            // 1. Calculate the vector pointing *from* the colliding entity *to* us.
            // This vector gives us the direction of the push.
            Vec3d collisionVector = this.getPos().subtract(other.getPos());

            // 2. Normalize the vector (make its length 1) to get only the direction.
            // Then, multiply it by a high factor (e.g., -0.7) to apply a strong
            // and immediate push *away* from the static entity.
            // Note the negative sign to push AWAY from the collision.
            Vec3d repulsionForce = collisionVector.normalize().multiply(-0.7);

            // 3. Apply the velocity to the *other* entity (the player/mob).
            other.addVelocity(repulsionForce);

            // 4. Reset the other entity's velocity immediately in the axis
            // of collision to prevent sliding *into* the entity.

            // This combination ensures the other entity is instantly shoved out
            // of the collision box, removing the "mushy" feeling.
        }

        // IMPORTANT: Still do *not* call super.pushAwayFrom(other)
        // or other.pushAwayFrom(this); to prevent default slow-push logic.
    }




}
