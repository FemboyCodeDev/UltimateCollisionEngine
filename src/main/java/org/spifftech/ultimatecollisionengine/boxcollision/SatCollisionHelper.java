package org.spifftech.ultimatecollisionengine.boxcollision;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/**
 * Utility class for OBB-AABB collision detection using the Separating Axis Theorem (SAT).
 * Uses Minecraft's built-in Vec3d and Box classes.
 */
public class SatCollisionHelper {

    // --- 1. AABB Structure (Minecraft's Box) ---

    // The AABB is represented directly by net.minecraft.util.math.Box.
    // Its axes are the world axes: X, Y, Z.
    public static final Vec3d[] AABB_AXES = {
            new Vec3d(1.0, 0.0, 0.0), // X-axis
            new Vec3d(0.0, 1.0, 0.0), // Y-axis
            new Vec3d(0.0, 0.0, 1.0)  // Z-axis
    };

    /**
     * Helper class to manage the OBB data.
     * In a mod, the OBB data would come from your entity's position and rotation.
     */
    public static class OBB {
        public final Vec3d center;
        public final Vec3d[] axes;         // U1, U2, U3 (must be normalized)
        public final Vec3d halfExtents;    // E1, E2, E3

        public OBB(Vec3d center, Vec3d[] axes, Vec3d halfExtents) {
            this.center = center;
            // Normalize axes using Vec3d.normalize()
            this.axes = new Vec3d[] {
                    axes[0].normalize(),
                    axes[1].normalize(),
                    axes[2].normalize()
            };
            this.halfExtents = halfExtents;
        }
    }


    // --- 2. Separating Axis Theorem (SAT) Logic ---

    /**
     * Calculates the projected radius (R) of an OBB onto a given axis (L).
     * R = sum(|Ei * (Ui . L)|)
     */
    private static double projectObbRadius(OBB obb, Vec3d axis) {
        double radius = 0.0;
        // OBB axes U1, U2, U3 and half-extents E1, E2, E3
        // Uses Vec3d.dotProduct()
        radius += Math.abs(obb.halfExtents.getX() * obb.axes[0].dotProduct(axis));
        radius += Math.abs(obb.halfExtents.getY() * obb.axes[1].dotProduct(axis));
        radius += Math.abs(obb.halfExtents.getZ() * obb.axes[2].dotProduct(axis));
        return radius;
    }

    /**
     * Calculates the projected radius (R) of an AABB (Box) onto a given axis (L).
     * This is a simplified version since the AABB axes are the world axes (X, Y, Z).
     */
    private static double projectAabbRadius(Box aabb, Vec3d axis) {
        double halfEx = (aabb.maxX - aabb.minX) / 2.0;
        double halfEy = (aabb.maxY - aabb.minY) / 2.0;
        double halfEz = (aabb.maxZ - aabb.minZ) / 2.0;

        double radius = 0.0;
        // Since AABB axes align with world axes, dot product with X, Y, Z simplifies to components
        radius += Math.abs(halfEx * axis.getX());
        radius += Math.abs(halfEy * axis.getY());
        radius += Math.abs(halfEz * axis.getZ());
        return radius;
    }


    /**
     * Checks if a given axis is a separating axis in 3D space.
     * Returns true if separated (a gap is found), false if overlapping.
     */
    private static boolean isSeparatingAxis(OBB obb, Box aabb, Vec3d axis) {
        // 1. Check for a near-zero axis length (handles cross-products of parallel axes)
        // Uses Vec3d.lengthSquared()
        if (axis.lengthSquared() < 1e-6) {
            return false;
        }

        // 2. Get the radius (projected half-length) of each box onto the axis
        double rObb = projectObbRadius(obb, axis);
        double rAabb = projectAabbRadius(aabb, axis);

        // 3. Calculate the distance between the projected centers
        // Box.getCenter() is a built-in method
        Vec3d cAabb = aabb.getCenter();
        Vec3d cObbToCAabb = cAabb.subtract(obb.center); // Vec3d.subtract()

        // Projected center distance: | (C_aabb - C_obb) . axis |
        // Uses Vec3d.dotProduct()
        double distanceBetweenCentersProj = Math.abs(cObbToCAabb.dotProduct(axis));

        // 4. Check for separation (if distance > sum of radii)
        return distanceBetweenCentersProj > (rObb + rAabb);
    }

    /**
     * Checks for intersection between an OBB and a Minecraft Box (AABB) using SAT.
     * Tests 15 potential separating axes.
     * * @param obb The Oriented Bounding Box (e.g., your custom entity).
     * @param aabb The Axis-Aligned Bounding Box (e.g., a block or vanilla entity's Box).
     * @return true if the boxes intersect, false otherwise (a separating axis was found).
     */
    public static boolean obbAabbIntersects(OBB obb, Box aabb) {
        // Test 1: 3 OBB axes (U1, U2, U3)
        for (Vec3d axis : obb.axes) {
            if (isSeparatingAxis(obb, aabb, axis)) {
                return false;
            }
        }

        // Test 2: 3 AABB axes (World axes X, Y, Z)
        for (Vec3d axis : AABB_AXES) {
            if (isSeparatingAxis(obb, aabb, axis)) {
                return false;
            }
        }

        // Test 3: 9 Cross-products (Ui x Xj)
        for (Vec3d obbAxis : obb.axes) {
            for (Vec3d aabbAxis : AABB_AXES) {
                // Vec3d.crossProduct() is the built-in method
                Vec3d crossAxis = obbAxis.crossProduct(aabbAxis);

                // Only test axes with a non-zero length
                if (crossAxis.lengthSquared() > 1e-6) {
                    Vec3d normalizedCrossAxis = crossAxis.normalize(); // Vec3d.normalize()
                    if (isSeparatingAxis(obb, aabb, normalizedCrossAxis)) {
                        return false;
                    }
                }
            }
        }

        // If no separating axis was found, they must intersect.
        return true;
    }
}