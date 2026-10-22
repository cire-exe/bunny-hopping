package com.kishi.bhop.config;

import java.util.function.Supplier;

import com.kishi.bhop.BhopMod;

/**
 * Repairs single config values, logging every repair by key so a bad hand edit is easy to find.
 *
 * <p>A value that is not a finite number goes back to its fallback, and one outside its usable
 * range is clamped to the nearest end of it.
 */
public final class Values {

	private Values() {
	}

	public static double atLeast(final String key, final double value, final double min, final double fallback) {
		return inRange(key, value, min, Double.MAX_VALUE, fallback);
	}

	public static double inRange(
			final String key, final double value, final double min, final double max, final double fallback) {
		if (!Double.isFinite(value)) {
			warnReplaced(key, value, fallback);
			return fallback;
		}

		final double clamped = Math.clamp(value, min, max);
		if (clamped != value) {
			warnReplaced(key, value, clamped);
		}

		return clamped;
	}

	public static float atLeast(final String key, final float value, final float min, final float fallback) {
		return inRange(key, value, min, Float.MAX_VALUE, fallback);
	}

	/** The float twin of the double version, kept separate so the log shows the value as written. */
	public static float inRange(final String key, final float value, final float min, final float max, final float fallback) {
		if (!Float.isFinite(value)) {
			warnReplaced(key, value, fallback);
			return fallback;
		}

		final float clamped = Math.clamp(value, min, max);
		if (clamped != value) {
			warnReplaced(key, value, clamped);
		}

		return clamped;
	}

	public static int atLeast(final String key, final int value, final int min) {
		return inRange(key, value, min, Integer.MAX_VALUE);
	}

	public static int inRange(final String key, final int value, final int min, final int max) {
		final int clamped = Math.clamp(value, min, max);
		if (clamped != value) {
			warnReplaced(key, value, clamped);
		}

		return clamped;
	}

	public static float finite(final String key, final float value, final float fallback) {
		return atLeast(key, value, -Float.MAX_VALUE, fallback);
	}

	/** Returns the section, or a fresh default one if it was missing from the file. */
	public static <T> T orDefault(final String key, final T section, final Supplier<T> defaults) {
		if (section != null) {
			return section;
		}

		BhopMod.LOGGER.warn("Config section {} is missing, using defaults", key);
		return defaults.get();
	}

	private static void warnReplaced(final String key, final Number value, final Number replacement) {
		BhopMod.LOGGER.warn("Config value {} = {} is not usable, using {}", key, value, replacement);
	}
}
