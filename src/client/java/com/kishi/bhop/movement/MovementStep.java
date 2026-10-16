package com.kishi.bhop.movement;

import com.kishi.bhop.physics.PhysicsSettings;

import net.minecraft.SharedConstants;
import net.minecraft.world.phys.Vec3;

/**
 * One tick of horizontal Source movement, on the ground or in the air.
 *
 * <p>Split out of {@link SourceMovementController} so it reads nothing from the game: every input,
 * from the player's speed factor to the block's friction, is passed in. That keeps it testable
 * without a running client. The controller still owns the jump, the speed clamp and the move.
 *
 * <p>Speeds are horizontal, in blocks per second; the vertical component passes through untouched.
 */
final class MovementStep {

	/** One Minecraft tick, in seconds. */
	static final double TICK_SECONDS = 1.0D / SharedConstants.TICKS_PER_SECOND;

	private MovementStep() {
	}

	/**
	 * Friction then acceleration, as Source's {@code WalkMove} does them.
	 *
	 * @param yaw         where the player faces
	 * @param speedFactor the movement speed attribute's scale, from {@link VanillaModifiers#speedFactor}
	 * @param surface     the block's friction scale, from {@link VanillaModifiers#surfaceFriction}
	 * @param jumping     whether the player leaves the ground this tick
	 */
	static Vec3 ground(
			Vec3 velocity,
			final MoveInput input,
			final float yaw,
			final double speedFactor,
			final double surface,
			final boolean jumping,
			final PhysicsSettings physics) {
		// 1. Friction, but never on a tick the player leaves the ground. Skipping it is the whole
		// trick: land and jump on the same tick and none of the speed is scrubbed off.
		if (!jumping) {
			velocity = SourcePhysics.applyFriction(velocity, physics.friction * surface, stopSpeed(physics, input), TICK_SECONDS);
		}

		// 2. Acceleration towards the wish direction.
		if (!input.isEmpty()) {
			final double wishSpeed = physics.maxGroundSpeed * speedFactor * input.scale();
			velocity = SourcePhysics.accelerate(velocity, input.direction(yaw), wishSpeed, physics.groundAccelerate * surface, TICK_SECONDS);
		}

		return velocity;
	}

	/**
	 * Air acceleration across the tick's sub-steps, each towards where the view faced at the end of
	 * that sub-step.
	 *
	 * @param yaws        the sub-step path from {@link ViewSampler#consume}
	 * @param speedFactor the movement speed attribute's scale, from {@link VanillaModifiers#speedFactor}
	 */
	static Vec3 air(Vec3 velocity, final MoveInput input, final float[] yaws, final double speedFactor, final PhysicsSettings physics) {
		if (input.isEmpty()) {
			return velocity;
		}

		final double wishSpeed = airWishSpeed(physics, speedFactor);
		final int steps = yaws.length - 1;
		final double dt = TICK_SECONDS / steps;
		for (int k = 1; k <= steps; k++) {
			velocity = SourcePhysics.airAccelerate(
					velocity, input.direction(yaws[k]), wishSpeed, physics.airAccelerate, airWishSpeedCap(physics, velocity), dt);
		}

		return velocity;
	}

	/**
	 * Wish speed for air strafing, which ignores input slowdowns such as sneaking, eating or
	 * blocking with a shield.
	 *
	 * <p>Air acceleration per step scales with the wish speed. Using an item cuts the input to a
	 * fifth, which leaves far too little acceleration to turn the velocity, so the player could only
	 * fly straight. The slowdowns still apply on the ground.
	 */
	private static double airWishSpeed(final PhysicsSettings physics, final double speedFactor) {
		return physics.maxGroundSpeed * speedFactor;
	}

	/**
	 * Friction's stop speed floor, scaled down with slowed input such as sneaking or using an item.
	 *
	 * <p>Below the floor friction removes a flat amount per tick. At full stop speed that is more
	 * than ground acceleration can add towards a sneaking wish speed, so the player would crawl at a
	 * fraction of vanilla's sneak speed. Scaling it with the input keeps the floor in proportion to
	 * the wish speed. With no input the full floor applies, so stopping is unchanged.
	 */
	private static double stopSpeed(final PhysicsSettings physics, final MoveInput input) {
		return input.isEmpty() ? physics.stopSpeed : physics.stopSpeed * input.scale();
	}

	/**
	 * Air wish speed cap for this sub-step, boosted while still below the ramp-up speed.
	 *
	 * <p>Scaling the cap is the only thing that actually speeds up the climb. Source computes the
	 * per-step gain as the smaller of (airAccelerate * wishSpeed * dt) and the cap; with the
	 * shipped numbers the first term is above the cap, so the cap is the binding constraint and
	 * raising airAccelerate on its own would have no visible effect.
	 */
	private static double airWishSpeedCap(final PhysicsSettings physics, final Vec3 velocity) {
		if (!physics.rampUp || SourcePhysics.horizontalSpeed(velocity) >= physics.rampUpSpeed) {
			return physics.maxAirWishSpeed;
		}

		return physics.maxAirWishSpeed * physics.rampUpMultiplier;
	}
}
