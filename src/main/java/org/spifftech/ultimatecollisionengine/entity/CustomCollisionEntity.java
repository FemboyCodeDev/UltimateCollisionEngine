package org.spifftech.ultimatecollisionengine.entity;


import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
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



}
