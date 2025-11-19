package org.spifftech.ultimatecollisionengine;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;


import org.spifftech.ultimatecollisionengine.entity.CustomCollisionEntity;
import org.spifftech.ultimatecollisionengine.entity.CustomCollisionEntityAttributes;


public class Ultimatecollisionengine implements ModInitializer {

    public static final String MOD_ID = "ultimatecollisionengine";


    public static final EntityType<CustomCollisionEntity> CUSTOM_COLLISION_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "custom_collision_entity"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomCollisionEntity::new)
                    .dimensions(EntityDimensions.fixed(0.1f, 0.1f)) // Define width and height

                    .build()
    );

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(
                CUSTOM_COLLISION_ENTITY,
                CustomCollisionEntityAttributes.createCustomAttributes()
        );
    }

}
