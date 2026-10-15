package com.kishi.bhop.movement;

/**
 * Records where the player looked on every rendered frame, so air strafing can follow the mouse
 * between ticks.
 *
 * <p>Minecraft simulates movement 20 times a second, but the mouse turns the view every frame. Read
 * only at the tick, a strafe is a handful of straight segments and gains speed about three times
 * slower than on a 64-tick shooter server. Instead, the yaw after each frame's mouse turn is
 * recorded here, and the tick replays air acceleration in fixed sub-steps along that path.
 *
 * <p>The sub-steps are fixed in number, not one per frame, because the speed gained per step does
 * not scale with its length: one step per frame would make strafing faster on a faster computer.
 * Frames are taken as evenly spaced in time, which holds closely enough over one tick.
 */
public final class ViewSampler {

	/** Room for one tick of frames at 5,000 fps. Beyond that the newest frame replaces the last. */
	private static final int CAPACITY = 256;

	private static final float[] frames = new float[CAPACITY];
	private static int frameCount;

	/** The yaw at the end of the previous tick, where this tick's path starts. */
	private static float tickStartYaw;
	private static boolean started;

	private ViewSampler() {
	}

	/** Records the view after this frame's mouse turn. Called once per rendered frame. */
	public static void recordFrame(final float yaw) {
		if (frameCount < CAPACITY) {
			frames[frameCount++] = yaw;
		} else {
			frames[CAPACITY - 1] = yaw;
		}
	}

	/**
	 * The yaw at each sub-step boundary of the tick that just ended, then starts the next tick.
	 *
	 * @param currentYaw the yaw now, which always ends the path
	 * @param steps      number of sub-steps
	 * @return {@code steps + 1} yaws: entry 0 is the start of the tick and entry {@code k} the end of
	 *         sub-step {@code k}
	 */
	static float[] consume(final float currentYaw, final int steps) {
		final float start = started ? tickStartYaw : currentYaw;

		// Path points: the start, every recorded frame, then the current yaw if anything moved the
		// view since the last frame (a teleport, or a mod turning the player). Always at least two.
		final int points = 1 + frameCount + (frameCount == 0 || frames[frameCount - 1] != currentYaw ? 1 : 0);
		final float[] yaws = new float[steps + 1];
		for (int k = 0; k <= steps; k++) {
			final float position = (float) k / steps * (points - 1);
			final int index = Math.min((int) position, points - 2);
			yaws[k] = lerp(point(index, start, currentYaw), point(index + 1, start, currentYaw), position - index);
		}

		tickStartYaw = currentYaw;
		started = true;
		frameCount = 0;
		return yaws;
	}

	/** Forgets the path, for when Source movement stops driving the player. */
	static void reset() {
		started = false;
		frameCount = 0;
	}

	private static float point(final int index, final float start, final float end) {
		if (index == 0) {
			return start;
		}

		return index <= frameCount ? frames[index - 1] : end;
	}

	private static float lerp(final float from, final float to, final float t) {
		return from + (to - from) * t;
	}
}
