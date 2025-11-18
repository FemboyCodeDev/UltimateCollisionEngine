package org.spifftech.ultimatecollisionengine.entity;

import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;

public class CustomCollisionEntityAttributes {

    public static DefaultAttributeContainer.Builder createCustomAttributes() {
        return MobEntity.createMobAttributes()
                // Ensure you add all essential LivingEntity attributes
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1) // Example: 20 health (10 hearts)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3) // Example: standard movement speed
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0); // Example: attack power
    }
}