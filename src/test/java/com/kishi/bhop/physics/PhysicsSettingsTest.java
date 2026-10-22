package com.kishi.bhop.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

/** Checks that {@link PhysicsSettings} repairs bad values and copies every field. */
class PhysicsSettingsTest {

	@Test
	void sanitizeRestoresDefaultsAndClamps() {
		final PhysicsSettings defaults = new PhysicsSettings();
		final PhysicsSettings settings = new PhysicsSettings();
		settings.airAccelerate = Double.NaN;
		settings.maxTotalSpeed = 1.0E300D;
		settings.friction = -1.0D;
		settings.airSubsteps = 0;

		settings.sanitize();

		assertEquals(defaults.airAccelerate, settings.airAccelerate);
		assertEquals(PhysicsSettings.MAX_SPEED, settings.maxTotalSpeed);
		assertEquals(0.0D, settings.friction);
		assertEquals(1, settings.airSubsteps);
	}

	@Test
	void sanitizeNeverChangesTheDefaultsItFallsBackTo() {
		final PhysicsSettings first = new PhysicsSettings();
		first.gravityScale = Double.NaN;
		first.sanitize();

		final PhysicsSettings second = new PhysicsSettings();
		second.gravityScale = Double.NaN;
		second.sanitize();

		assertEquals(new PhysicsSettings().gravityScale, second.gravityScale);
	}

	@Test
	void copyCarriesEveryField() {
		final PhysicsSettings original = new PhysicsSettings();
		original.groundAccelerate = 1.5D;
		original.airSubsteps = 7;
		original.rampUp = true;
		original.gravityScale = 2.0D;

		final PhysicsSettings copy = original.copy();

		assertNotSame(original, copy);
		for (final PhysicsSetting field : PhysicsSettings.FIELDS) {
			assertEquals(field.describe(original), field.describe(copy), field.name());
		}
	}
}
