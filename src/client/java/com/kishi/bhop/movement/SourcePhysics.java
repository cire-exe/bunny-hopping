package com.kishi.bhop.movement;

import net.minecraft.world.phys.Vec3;

/**
 * A direct port of the classic FPS engine's horizontal movement maths.
 *
 * <p>Every method is pure: it takes a velocity and returns a new one, leaving the vertical
 * component untouched. Keeping the maths free of Minecraft state makes the behaviour easy to reason
 * about and to unit test, and lets {@link SourceMovementController} own all of the game-side
 * concerns.
 *
 * <p>All speeds are in blocks per second and {@code dt} is in seconds.
 */
public final class SourcePhysics {

	/** Speeds below this are treated as a dead stop, mirroring Source's own epsilon. */
	private static final double SPEED_EPSILON = 1.0E-4D;

	private SourcePhysics() {
	}

	/** Horizontal length of a velocity vector. */
	public static double horizontalSpeed(final Vec3 velocity) {
		return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
	}

	/**
	 * Source's {@code CBasePlayer::Friction}.
	 *
	 * <p>Scales horizontal velocity down by a drop proportional to the current speed, with a floor
	 * of {@code stopSpeed} so that slow movement still comes to a halt promptly. This only ever
	 * runs while standing on the ground, which is precisely why bunny hopping preserves speed: a
	 * player who leaves the ground on the same tick they land never has friction applied.
	 */
	public static Vec3 applyFriction(final Vec3 velocity, final double friction, final double stopSpeed, final double dt) {
		final double speed = horizontalSpeed(velocity);
		if (speed < SPEED_EPSILON) {
			return new Vec3(0.0D, velocity.y, 0.0D);
		}

		final double control = Math.max(speed, stopSpeed);
		final double drop = control * friction * dt;
		final double newSpeed = Math.max(0.0D, speed - drop);
		if (newSpeed == speed) {
			return velocity;
		}

		final double scale = newSpeed / speed;
		return new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
	}

	/**
	 * Source's {@code CBasePlayer::Accelerate}, used while on the ground.
	 *
	 * <p>Only ever adds speed along the wish direction, and never past {@code wishSpeed}, so ground
	 * movement settles at a fixed top speed.
	 */
	public static Vec3 accelerate(
			final Vec3 velocity,
			final Vec3 wishDir,
			final double wishSpeed,
			final double accel,
			final double dt) {
		return push(velocity, wishDir, wishSpeed, accel * wishSpeed * dt);
	}

	/**
	 * Source's {@code CBasePlayer::AirAccelerate} - the heart of air strafing and bunny hopping.
	 *
	 * <p>The difference from {@link #accelerate} is a single clamp: the <em>target</em> speed is
	 * capped at {@code airWishSpeedCap} (30 u/s in the original), while the amount of
	 * acceleration available per tick still scales with the uncapped wish speed. The cap therefore
	 * limits how much velocity can be added <em>along</em> the wish direction, but places no limit
	 * on the total speed.
	 *
	 * <p>Holding a strafe key and turning the mouse the same way keeps the wish direction nearly
	 * perpendicular to the current velocity. The dot product stays near zero, so the cap never bites
	 * and a little speed is added perpendicular on every tick, which rotates the velocity vector and
	 * lengthens it. Repeating that across hops is what accelerates the player.
	 */
	public static Vec3 airAccelerate(
			final Vec3 velocity,
			final Vec3 wishDir,
			final double wishSpeed,
			final double accel,
			final double airWishSpeedCap,
			final double dt) {
		// The target is capped, but the available acceleration deliberately uses the uncapped wish
		// speed, exactly as Source does it.
		return push(velocity, wishDir, Math.min(wishSpeed, airWishSpeedCap), accel * wishSpeed * dt);
	}

	/**
	 * The step shared by both accelerate functions: add speed along {@code wishDir} until the
	 * velocity's component in that direction reaches {@code targetSpeed}, gaining at most
	 * {@code maxGain} this tick.
	 */
	private static Vec3 push(final Vec3 velocity, final Vec3 wishDir, final double targetSpeed, final double maxGain) {
		final double currentSpeed = velocity.x * wishDir.x + velocity.z * wishDir.z;
		final double addSpeed = targetSpeed - currentSpeed;
		if (addSpeed <= 0.0D) {
			return velocity;
		}

		final double gain = Math.min(maxGain, addSpeed);
		return velocity.add(wishDir.x * gain, 0.0D, wishDir.z * gain);
	}

	/** Clamps horizontal speed, leaving the vertical component alone. */
	public static Vec3 clampHorizontalSpeed(final Vec3 velocity, final double maxSpeed) {
		final double speed = horizontalSpeed(velocity);
		if (speed <= maxSpeed || speed < SPEED_EPSILON) {
			return velocity;
		}

		final double scale = maxSpeed / speed;
		return new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
	}
}
