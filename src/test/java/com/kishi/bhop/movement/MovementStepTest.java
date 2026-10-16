package com.kishi.bhop.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kishi.bhop.physics.PhysicsSettings;

import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Runs {@link MovementStep} tick by tick with the shipped physics and checks where the speed ends
 * up, the way it feels in game.
 */
class MovementStepTest {

	/** {@link VanillaModifiers#speedFactor} while walking with no effects: 0.1 / 0.13. */
	private static final double WALKING = 0.1D / 0.13D;

	private static final double SPRINTING = 1.0D;

	/** Vanilla's input scales: sneaking, and using an item such as food or a shield. */
	private static final double SNEAK = 0.3D;
	private static final double USE_ITEM = 0.2D;

	/** Ordinary blocks; see {@link VanillaModifiers#surfaceFriction}. */
	private static final double SOLID_GROUND = 1.0D;

	private final PhysicsSettings physics = new PhysicsSettings();

	@ParameterizedTest(name = "{0}")
	@CsvSource({
			"sprinting,       1.0,                1.0",
			"walking,         0.7692307692307692, 1.0",
			"sneaking,        0.7692307692307692, 0.3",
			"using an item,   0.7692307692307692, 0.2",
			"sneaking and eating, 0.7692307692307692, 0.06"
	})
	void groundSpeedSettlesAtTheWishSpeed(final String state, final double speedFactor, final double inputScale) {
		Vec3 velocity = Vec3.ZERO;
		for (int tick = 0; tick < 60; tick++) {
			velocity = MovementStep.ground(velocity, forward(inputScale), 0.0F, speedFactor, SOLID_GROUND, false, this.physics);
		}

		final double wishSpeed = this.physics.maxGroundSpeed * speedFactor * inputScale;
		assertEquals(wishSpeed, SourcePhysics.horizontalSpeed(velocity), wishSpeed * 0.01D, state);
	}

	@Test
	void sneakingMatchesVanillaSneakSpeed() {
		Vec3 velocity = Vec3.ZERO;
		for (int tick = 0; tick < 60; tick++) {
			velocity = MovementStep.ground(velocity, forward(SNEAK), 0.0F, WALKING, SOLID_GROUND, false, this.physics);
		}

		// Vanilla sneaks at about 1.31 blocks per second.
		assertEquals(1.31D, SourcePhysics.horizontalSpeed(velocity), 0.05D);
	}

	@Test
	void releasingTheKeysStopsQuickly() {
		Vec3 velocity = new Vec3(0.0D, 0.0D, this.physics.maxGroundSpeed);
		for (int tick = 0; tick < 10; tick++) {
			velocity = MovementStep.ground(velocity, MoveInput.NONE, 0.0F, SPRINTING, SOLID_GROUND, false, this.physics);
		}

		assertEquals(0.0D, SourcePhysics.horizontalSpeed(velocity));
	}

	@Test
	void jumpingTickKeepsSpeedAboveTheGroundCap() {
		final Vec3 fast = new Vec3(0.0D, 0.0D, 9.0D);
		final Vec3 velocity = MovementStep.ground(fast, forward(1.0D), 0.0F, SPRINTING, SOLID_GROUND, true, this.physics);

		assertEquals(9.0D, SourcePhysics.horizontalSpeed(velocity), 1.0E-9D);
	}

	@Test
	void landingWithoutJumpingLosesSpeed() {
		final Vec3 fast = new Vec3(0.0D, 0.0D, 9.0D);
		final Vec3 velocity = MovementStep.ground(fast, forward(1.0D), 0.0F, SPRINTING, SOLID_GROUND, false, this.physics);

		assertTrue(SourcePhysics.horizontalSpeed(velocity) < 9.0D);
	}

	@Test
	void airStrafingGainsSpeedAndTurns() {
		final Vec3 velocity = strafeLeft(1.0D);

		assertTrue(SourcePhysics.horizontalSpeed(velocity) > 7.0D, "speed gained");
		// Started heading along +z; turning left swings the velocity towards +x.
		assertTrue(velocity.x > 1.0D, "velocity turned");
	}

	@ParameterizedTest(name = "input scale {0}")
	@CsvSource({ "0.3", "0.2", "0.06" })
	void slowedInputStrafesLikeFullInput(final double inputScale) {
		final Vec3 full = strafeLeft(1.0D);
		final Vec3 slowed = strafeLeft(inputScale);

		assertEquals(full.x, slowed.x, 1.0E-9D);
		assertEquals(full.z, slowed.z, 1.0E-9D);
	}

	@Test
	void noInputInTheAirKeepsVelocity() {
		final Vec3 velocity = new Vec3(3.0D, 0.0D, 4.0D);

		assertEquals(velocity, MovementStep.air(velocity, MoveInput.NONE, yawPath(0.0F, -10.0F), SPRINTING, this.physics));
	}

	/**
	 * One second of air strafing to the left: heading along +z at 7 blocks per second, holding only
	 * the left key and turning the view left at 90 degrees per second.
	 */
	private Vec3 strafeLeft(final double inputScale) {
		final MoveInput left = MoveInput.of(new Vec3(0.98D * inputScale, 0.0D, 0.0D));
		Vec3 velocity = new Vec3(0.0D, 0.0D, 7.0D);
		float yaw = 0.0F;
		for (int tick = 0; tick < 20; tick++) {
			// Yaw grows as the view turns right, so turning left lowers it.
			final float next = yaw - 4.5F;
			velocity = MovementStep.air(velocity, left, yawPath(yaw, next), WALKING, this.physics);
			yaw = next;
		}

		return velocity;
	}

	/** Holding forward, slowed to {@code scale}, as vanilla hands the input to travel. */
	private static MoveInput forward(final double scale) {
		return MoveInput.of(new Vec3(0.0D, 0.0D, 0.98D * scale));
	}

	/** A view turning evenly from {@code from} to {@code to} over the tick's sub-steps. */
	private float[] yawPath(final float from, final float to) {
		final int steps = this.physics.airSubsteps;
		final float[] yaws = new float[steps + 1];
		for (int k = 0; k <= steps; k++) {
			yaws[k] = from + (to - from) * k / steps;
		}

		return yaws;
	}
}
