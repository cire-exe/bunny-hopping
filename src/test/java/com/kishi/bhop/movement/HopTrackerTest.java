package com.kishi.bhop.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plays out hop chains tick by tick and checks what {@link HopTracker} reports to the HUD. */
class HopTrackerTest {

	/** A strafe key alone: positive is left. */
	private static final double LEFT_KEY = 1.0D;
	private static final double RIGHT_KEY = -1.0D;

	@BeforeEach
	void endChain() {
		HopTracker.reset();
	}

	@Test
	void firstJumpOnlyStartsTheChain() {
		jump(5.0D);

		assertNull(HopTracker.lastHop());
	}

	@Test
	void perfectHopsBuildAStreak() {
		jump(5.0D);
		air(10);
		jump(6.0D);

		HopTracker.Hop hop = HopTracker.lastHop();
		assertNotNull(hop);
		assertEquals(1.0D, hop.gain(), 1.0E-9D);
		assertTrue(hop.perfect());
		assertEquals(1, hop.perfectStreak());

		air(10);
		jump(6.5D);

		hop = HopTracker.lastHop();
		assertEquals(0.5D, hop.gain(), 1.0E-9D);
		assertEquals(2, hop.perfectStreak());
	}

	@Test
	void lateHopCountsGroundTicksAndEndsTheStreak() {
		jump(5.0D);
		air(10);
		jump(6.0D);
		air(10);
		ground(2);
		jump(4.0D);

		final HopTracker.Hop hop = HopTracker.lastHop();
		assertEquals(-2.0D, hop.gain(), 1.0E-9D);
		assertEquals(2, hop.groundTicks());
		assertFalse(hop.perfect());
		assertEquals(0, hop.perfectStreak());
	}

	@Test
	void longStopOnTheGroundStartsANewChain() {
		jump(5.0D);
		air(10);
		ground(11);
		jump(5.0D);

		assertNull(HopTracker.lastHop());
	}

	@Test
	void syncCountsTurningThatMatchesTheStrafeKey() {
		jump(5.0D);
		// Three sub-steps turning right while holding right, then one turning left against it.
		HopTracker.recordAirTurn(new float[] {0.0F, 1.0F, 2.0F, 3.0F}, RIGHT_KEY);
		HopTracker.recordAirTurn(new float[] {3.0F, 2.0F}, RIGHT_KEY);
		air(1);
		jump(6.0D);

		final HopTracker.Hop hop = HopTracker.lastHop();
		assertTrue(hop.hasSync());
		assertEquals(75, hop.syncPercent());
	}

	@Test
	void turningWithoutAStrafeKeyIsOutOfSync() {
		jump(5.0D);
		HopTracker.recordAirTurn(new float[] {0.0F, -1.0F, -2.0F}, LEFT_KEY);
		HopTracker.recordAirTurn(new float[] {-2.0F, -3.0F, -4.0F}, 0.0D);
		jump(6.0D);

		assertEquals(50, HopTracker.lastHop().syncPercent());
	}

	@Test
	void noTurningMeansNoSync() {
		jump(5.0D);
		HopTracker.recordAirTurn(new float[] {10.0F, 10.0F, 10.0F}, LEFT_KEY);
		jump(6.0D);

		assertFalse(HopTracker.lastHop().hasSync());
	}

	@Test
	void hopClearsFromTheHudAfterThreeSeconds() {
		jump(5.0D);
		air(10);
		jump(6.0D);

		air(60);
		assertNotNull(HopTracker.lastHop());

		air(1);
		assertNull(HopTracker.lastHop());
	}

	@Test
	void resetClearsTheLastHop() {
		jump(5.0D);
		jump(6.0D);
		HopTracker.reset();

		assertNull(HopTracker.lastHop());
	}

	private static void jump(final double takeoffSpeed) {
		HopTracker.tick(true, true, takeoffSpeed);
	}

	private static void air(final int ticks) {
		for (int tick = 0; tick < ticks; tick++) {
			HopTracker.tick(false, false, 0.0D);
		}
	}

	private static void ground(final int ticks) {
		for (int tick = 0; tick < ticks; tick++) {
			HopTracker.tick(true, false, 0.0D);
		}
	}
}
