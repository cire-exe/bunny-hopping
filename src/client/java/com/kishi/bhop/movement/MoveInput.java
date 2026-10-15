package com.kishi.bhop.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The movement keys for one tick, relative to where the player is looking.
 *
 * <p>Built from the input vanilla hands to {@code travel}, which already carries its slowdowns:
 * sneaking, crawling and using an item such as food or a bow all scale it down. Source movement
 * keeps the direction and turns the length into {@link #scale}.
 *
 * @param left    sideways component of the unit direction, positive to the left
 * @param forward forward component of the unit direction
 * @param scale   0 to 1, how much of the wish speed the input asks for
 */
record MoveInput(double left, double forward, double scale) {

	static final MoveInput NONE = new MoveInput(0.0D, 0.0D, 0.0D);

	/**
	 * The length vanilla gives plain, unslowed input: it trims every move vector to 98% before
	 * applying the other slowdowns. Dividing by it maps that back to full speed.
	 */
	private static final double VANILLA_FULL_INPUT = 0.98D;

	private static final double EPSILON = 1.0E-6D;

	/** From vanilla's travel input, where x is sideways ({@code xxa}) and z is forward ({@code zza}). */
	static MoveInput of(final Vec3 travelInput) {
		final double length = Math.sqrt(travelInput.x * travelInput.x + travelInput.z * travelInput.z);
		if (length < EPSILON) {
			return NONE;
		}

		return new MoveInput(travelInput.x / length, travelInput.z / length, Math.min(1.0D, length / VANILLA_FULL_INPUT));
	}

	boolean isEmpty() {
		return this.scale <= 0.0D;
	}

	/** The wish direction in world space, as a unit vector, for a player facing {@code yawDegrees}. */
	Vec3 direction(final float yawDegrees) {
		final float yawRadians = yawDegrees * Mth.DEG_TO_RAD;
		final double sin = Mth.sin(yawRadians);
		final double cos = Mth.cos(yawRadians);

		// Matches Entity#moveRelative: forward is (-sin, cos) and left is (cos, sin).
		return new Vec3(this.left * cos - this.forward * sin, 0.0D, this.forward * cos + this.left * sin);
	}
}
