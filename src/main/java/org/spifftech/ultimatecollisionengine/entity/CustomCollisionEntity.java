package org.spifftech.ultimatecollisionengine.entity;


import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.spifftech.ultimatecollisionengine.boxcollision.SatCollisionHelper;

import java.util.List;

import static org.spifftech.ultimatecollisionengine.Ultimatecollisionengine.CollisionPushDistance;

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
        // Step 1: Calculate the world-space center
        Vec3d center = this.getPos();

        // Step 2: Get the entity's current rotation (Yaw is the rotation around the Y-axis)
        // We use the entity's current yaw, which is updated in the tick() method below.
        float yawDegrees = this.getYaw();

        // Convert the yaw from degrees to a quaternion for rotation
        // RotationAxis.POSITIVE_Y defines the rotation axis for yaw.
        org.joml.Quaternionf rotation = RotationAxis.POSITIVE_Y.rotationDegrees(yawDegrees);

        // Step 3: Rotate the OBB's *local* axes into *world* axes
        Vec3d[] worldAxes = new Vec3d[3];

        // Rotate Local X-axis (U1)
        worldAxes[0] = rotateVector(CustomCollisionBox.axes[0], rotation);
        // Rotate Local Y-axis (U2)
        worldAxes[1] = rotateVector(CustomCollisionBox.axes[1], rotation);
        // Rotate Local Z-axis (U3)
        worldAxes[2] = rotateVector(CustomCollisionBox.axes[2], rotation);

        // Step 4: Create the final world-space OBB
        return new SatCollisionHelper.OBB(
                center,
                worldAxes, // **Using the newly rotated axes!**
                CustomCollisionBox.halfExtents
        );
    }

    private Vec3d rotateVector(Vec3d vector, org.joml.Quaternionf rotation) {
        // Convert Minecraft Vec3d to JOML Vector3f
        org.joml.Vector3f vec = new org.joml.Vector3f((float) vector.getX(), (float) vector.getY(), (float) vector.getZ());

        // Apply rotation
        vec.rotate(rotation);

        // Convert back to Minecraft Vec3d
        return new Vec3d(vec.x(), vec.y(), vec.z());
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

    public void setEntityPosition(Entity other,Vec3d pos){

        // Check if the entity is a player
        if (other instanceof net.minecraft.server.network.ServerPlayerEntity player) {

            // 🌟 CRITICAL FIX: Use the specific player teleport method.
            // This method sends a packet that tells the client to stop its movement
            // prediction and accept the new position immediately.
            // The last three arguments (yaw, pitch, flags) are important for full sync.
            player.teleport(
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );

        } else {
            // For non-player entities (Mobs, Items, etc.):
            // Use setPos and then let the entity tracker handle the periodic sync.
            // Since you are in tick(), the tracker should pick up the change soon.
            other.setPosition(pos);
        }


    }
    // --- 4. Custom Collision Logic ---

    // 🌟 Implement the custom collision logic in the tick loop.
    @Override
    public void tick() {
        //System.out.println("Ticking Custom Collision Entity");
        super.tick();

        if (!this.getWorld().isClient) {



            // ⭐ NEW: Calculate and set a continuous rotation based on age (ticks)
            // This is what makes the entity spin!
            float spinSpeed = 5.0f; // Spin 5 degrees per tick
            float newYaw = (this.age * spinSpeed) % 360.0f; // Get angle and wrap around 360
            this.setYaw(newYaw);

            // Since we only rotate around Y, pitch and roll remain 0
            this.setPitch(0.0f);

            // Ensure the vanilla rotation fields are updated for the next tick
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();


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

                //other.setPosition(other.getPos().add(new Vec3d(0,1,0)));
                //other.updatePosition(0,0,0);
                System.out.println("Position");
                System.out.println(other.getPos());
                System.out.println(other.getType());

                //setEntityPosition(other, new Vec3d(0,0,0));






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

                    double pushFactor = 0.5; // Controls the strength of the push
                    pushFactor = (double) 1.0 / (double) getWorld().getGameRules().getInt(CollisionPushDistance);


                    double pushX = centerVector.getX() > 0 ? overlapX + 0.001 : -(overlapX + 0.001);
                    double pushZ = centerVector.getZ() > 0 ? overlapZ + 0.001 : -(overlapZ + 0.001);
                    other.addVelocity(pushX * pushFactor, 10.0, pushZ * pushFactor);
                    System.out.println(other.getPos().add(centerVector));
                    setEntityPosition(other,other.getPos().add(centerVector.multiply(pushFactor)));

                    // Determine MTV axis and apply repulsion
                    if (overlapX < overlapZ) {
                        // Push along X axis (axis with the least overlap)
                        // Add a small epsilon (0.001) to ensure separation
                        //double pushX = centerVector.getX() > 0 ? overlapX + 0.001 : -(overlapX + 0.001);
                        other.addVelocity(pushX * pushFactor, 0.0, 0.0);

                    } else {
                        // Push along Z axis
                       // double pushZ = centerVector.getZ() > 0 ? overlapZ + 0.001 : -(overlapZ + 0.001);
                        other.addVelocity(0.0, 0.0, pushZ * pushFactor);
                    }

                    //other.velocityDirty = true;
                }
            }
        }
    }
}