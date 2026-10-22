package com.kishi.bhop.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks the rules client and server both apply: which state a player ends up in, and why a toggle
 * is refused.
 */
class PlayerRulesTest {

	@Test
	void optionalAllowsTogglingOutsideTheCooldown() {
		final PlayerRules rules = rules(BhopPolicy.OPTIONAL, true);

		assertNull(rules.refusal(false));
		assertEquals(Refusal.COOLDOWN, rules.refusal(true));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void notAllowedComesFirst(final boolean coolingDown) {
		for (final BhopPolicy policy : BhopPolicy.values()) {
			assertEquals(Refusal.NOT_ALLOWED, rules(policy, false).refusal(coolingDown), policy.name());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void serverChosenStatesCannotBeToggled(final boolean coolingDown) {
		assertEquals(Refusal.FORCED_ON, rules(BhopPolicy.FORCED_ON, true).refusal(coolingDown));
		assertEquals(Refusal.DISABLED, rules(BhopPolicy.DISABLED, true).refusal(coolingDown));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void resolveFollowsThePolicy(final boolean chosen) {
		assertEquals(chosen, rules(BhopPolicy.OPTIONAL, true).resolve(chosen));
		assertTrue(rules(BhopPolicy.FORCED_ON, true).resolve(chosen));
		assertFalse(rules(BhopPolicy.DISABLED, true).resolve(chosen));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void notAllowedIsAlwaysOff(final boolean chosen) {
		for (final BhopPolicy policy : BhopPolicy.values()) {
			assertFalse(rules(policy, false).resolve(chosen), policy.name());
		}
	}

	@Test
	void unknownIdsFallBackSafely() {
		assertEquals(Refusal.NOT_ALLOWED, Refusal.byId(Refusal.values().length));
		assertEquals(Refusal.NOT_ALLOWED, Refusal.byId(-1));
		assertEquals(BhopPolicy.DISABLED, BhopPolicy.byId(BhopPolicy.values().length));
		assertEquals(BhopPolicy.DISABLED, BhopPolicy.byId(-1));
	}

	private static PlayerRules rules(final BhopPolicy policy, final boolean allowed) {
		return new PlayerRules(policy, allowed, true, ServerRules.MAX_JUMP_BUFFER_TICKS, 0);
	}
}
