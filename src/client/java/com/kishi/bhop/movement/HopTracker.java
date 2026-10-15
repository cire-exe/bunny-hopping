package com.kishi.bhop.movement;

/**
 * Measures each hop, for the HUD's strafe feedback.
 *
 * <p>A hop is reported at takeoff and covers everything since the previous takeoff: the air strafe,
 * any ticks spent on the ground, and the jump itself. The first jump of a chain only starts the
 * measurement, as there is nothing to compare it with.
 */
public final class HopTracker {

	/**
	 * One hop.
	 *
	 * @param gain          horizontal speed at this takeoff minus the previous one, in blocks per
	 *                      second
	 * @param groundTicks   ticks spent on the ground before jumping; 0 is a perfect hop, with no
	 *                      friction at all
	 * @param perfectStreak perfect hops in a row, this one included; 0 if this one was not
	 * @param syncPercent   share of the mouse turning in the air that matched the strafe key, 0 to
	 *                      100, or -1 if the view did not turn
	 */
	public record Hop(double gain, int groundTicks, int perfectStreak, int syncPercent) {

		public boolean perfect() {
			return this.groundTicks == 0;
		}

		public boolean hasSync() {
			return this.syncPercent >= 0;
		}
	}

	/** Longer than this on the ground and the next jump starts a new chain. */
	private static final int CHAIN_BREAK_TICKS = 10;

	/** How long the last hop stays reported after it. */
	private static final int DISPLAY_TICKS = 60;

	/** Smallest yaw change in one sub-step that counts as turning, in degrees. */
	private static final float TURN_EPSILON = 1.0E-3F;

	private static Hop last;
	private static int ticksSinceHop;

	private static double lastTakeoffSpeed = Double.NaN;
	private static int groundTicks;
	private static int perfectStreak;
	private static int turningSteps;
	private static int syncedSteps;

	private HopTracker() {
	}

	/** The most recent hop, or {@code null} if there has not been one lately. */
	public static Hop lastHop() {
		return ticksSinceHop <= DISPLAY_TICKS ? last : null;
	}

	/**
	 * Scores the view's turning against the strafe key, one sub-step at a time.
	 *
	 * <p>A sub-step is in sync when the view turns the way the strafe key points: right with only
	 * the right key, left with only the left one. Turning without a strafe key, or against it, does
	 * not count.
	 *
	 * @param yaws the sub-step path from {@link ViewSampler#consume}
	 * @param left the sideways input, positive to the left
	 */
	static void recordAirTurn(final float[] yaws, final double left) {
		for (int k = 1; k < yaws.length; k++) {
			final float turn = yaws[k] - yaws[k - 1];
			if (Math.abs(turn) < TURN_EPSILON) {
				continue;
			}

			turningSteps++;
			// Yaw grows as the view turns right.
			if (turn > 0.0F && left < 0.0D || turn < 0.0F && left > 0.0D) {
				syncedSteps++;
			}
		}
	}

	/**
	 * Advances one tick.
	 *
	 * @param onGround      whether the player started the tick on the ground
	 * @param jumping       whether they jumped this tick
	 * @param takeoffSpeed  horizontal speed after the jump, in blocks per second; read only when jumping
	 */
	static void tick(final boolean onGround, final boolean jumping, final double takeoffSpeed) {
		ticksSinceHop++;

		if (jumping) {
			if (!Double.isNaN(lastTakeoffSpeed)) {
				perfectStreak = groundTicks == 0 ? perfectStreak + 1 : 0;
				last = new Hop(takeoffSpeed - lastTakeoffSpeed, groundTicks, perfectStreak, syncPercent());
				ticksSinceHop = 0;
			}

			lastTakeoffSpeed = takeoffSpeed;
			groundTicks = 0;
			turningSteps = 0;
			syncedSteps = 0;
		} else if (onGround && ++groundTicks > CHAIN_BREAK_TICKS) {
			lastTakeoffSpeed = Double.NaN;
			perfectStreak = 0;
		}
	}

	/** Ends the chain, for when Source movement stops driving the player. */
	static void reset() {
		last = null;
		lastTakeoffSpeed = Double.NaN;
		groundTicks = 0;
		perfectStreak = 0;
		turningSteps = 0;
		syncedSteps = 0;
	}

	private static int syncPercent() {
		return turningSteps == 0 ? -1 : Math.round(100.0F * syncedSteps / turningSteps);
	}
}
