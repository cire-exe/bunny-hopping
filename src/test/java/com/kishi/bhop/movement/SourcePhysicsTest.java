package com.kishi.bhop.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;

/** The Source maths on its own, one call at a time. */
class SourcePhysicsTest {

	private static final double DT = 0.05D;

	private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
	private static final Vec3 NORTH = new Vec3(0.0D, 0.0D, -1.0D);

	@Test
	void frictionScalesWithSpeedAboveStopSpeed() {
		final Vec3 velocity = SourcePhysics.applyFriction(new Vec3(10.0D, 0.0D, 0.0D), 4.0D, 2.0D, DT);

		// drop = speed * friction * dt = 10 * 4 * 0.05
		assertEquals(8.0D, velocity.x, 1.0E-9D);
	}

	@Test
	void frictionUsesTheStopSpeedFloorBelowIt() {
		final Vec3 velocity = SourcePhysics.applyFriction(new Vec3(1.0D, 0.0D, 0.0D), 4.0D, 2.0D, DT);

		// drop = stopSpeed * friction * dt = 2 * 4 * 0.05
		assertEquals(0.6D, velocity.x, 1.0E-9D);
	}

	@Test
	void frictionNeverReversesDirection() {
		final Vec3 velocity = SourcePhysics.applyFriction(new Vec3(0.1D, 0.0D, 0.0D), 4.0D, 2.0D, DT);

		assertEquals(0.0D, velocity.x);
	}

	@Test
	void frictionLeavesVerticalAlone() {
		final Vec3 velocity = SourcePhysics.applyFriction(new Vec3(10.0D, 0.42D, 0.0D), 4.0D, 2.0D, DT);

		assertEquals(0.42D, velocity.y);
	}

	@Test
	void accelerateStopsAtTheWishSpeed() {
		Vec3 velocity = Vec3.ZERO;
		for (int i = 0; i < 100; i++) {
			velocity = SourcePhysics.accelerate(velocity, EAST, 5.0D, 10.0D, DT);
		}

		assertEquals(5.0D, velocity.x, 1.0E-9D);
	}

	@Test
	void accelerateNeverSlowsAFasterPlayer() {
		final Vec3 fast = new Vec3(9.0D, 0.0D, 0.0D);

		assertEquals(fast, SourcePhysics.accelerate(fast, EAST, 5.0D, 10.0D, DT));
	}

	@Test
	void airAccelerateAlongVelocityIsCapped() {
		final Vec3 velocity = SourcePhysics.airAccelerate(new Vec3(7.0D, 0.0D, 0.0D), EAST, 5.0D, 12.0D, 0.75D, DT);

		// Already past the cap along the wish direction, so nothing is added.
		assertEquals(7.0D, velocity.x);
	}

	@Test
	void airAcceleratePerpendicularGainsSpeed() {
		final Vec3 before = new Vec3(7.0D, 0.0D, 0.0D);
		final Vec3 after = SourcePhysics.airAccelerate(before, NORTH, 5.0D, 12.0D, 0.75D, DT);

		assertEquals(-0.75D, after.z, 1.0E-9D);
		assertTrue(SourcePhysics.horizontalSpeed(after) > SourcePhysics.horizontalSpeed(before));
	}

	@Test
	void clampKeepsDirection() {
		final Vec3 velocity = SourcePhysics.clampHorizontalSpeed(new Vec3(6.0D, 1.0D, 8.0D), 5.0D);

		assertEquals(3.0D, velocity.x, 1.0E-9D);
		assertEquals(1.0D, velocity.y);
		assertEquals(4.0D, velocity.z, 1.0E-9D);
	}
}
