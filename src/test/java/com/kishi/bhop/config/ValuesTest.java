package com.kishi.bhop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/** Checks how {@link Values} repairs hand-edited config values. */
class ValuesTest {

	@Test
	void doublesAreClampedIntoRange() {
		assertEquals(2.0D, Values.inRange("k", 2.0D, 0.0D, 10.0D, 5.0D));
		assertEquals(0.0D, Values.inRange("k", -1.0D, 0.0D, 10.0D, 5.0D));
		assertEquals(10.0D, Values.inRange("k", 1.0E300D, 0.0D, 10.0D, 5.0D));
		assertEquals(1.0E300D, Values.atLeast("k", 1.0E300D, 0.0D, 5.0D));
	}

	@Test
	void nonFiniteDoublesFallBack() {
		assertEquals(5.0D, Values.inRange("k", Double.NaN, 0.0D, 10.0D, 5.0D));
		assertEquals(5.0D, Values.inRange("k", Double.POSITIVE_INFINITY, 0.0D, 10.0D, 5.0D));
	}

	@Test
	void floatsAreClampedAndFallBack() {
		assertEquals(0.5F, Values.atLeast("k", 0.5F, 0.1F, 0.56F));
		assertEquals(0.1F, Values.atLeast("k", 0.0F, 0.1F, 0.56F));
		assertEquals(0.56F, Values.atLeast("k", Float.NaN, 0.1F, 0.56F));
		assertEquals(-3.0F, Values.finite("k", -3.0F, 1.0F));
		assertEquals(1.0F, Values.finite("k", Float.NEGATIVE_INFINITY, 1.0F));
	}

	@Test
	void intsAreClamped() {
		assertEquals(3, Values.inRange("k", 3, 0, 10));
		assertEquals(0, Values.inRange("k", -3, 0, 10));
		assertEquals(10, Values.inRange("k", 30, 0, 10));
		assertEquals(Integer.MAX_VALUE, Values.atLeast("k", Integer.MAX_VALUE, 0));
	}

	@Test
	void missingSectionsGetDefaults() {
		final Object present = new Object();
		final Object fallback = new Object();

		assertSame(present, Values.orDefault("k", present, () -> fallback));
		assertSame(fallback, Values.orDefault("k", null, () -> fallback));
	}
}
