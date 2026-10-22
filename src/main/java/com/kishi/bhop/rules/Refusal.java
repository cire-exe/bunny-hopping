package com.kishi.bhop.rules;

/**
 * Why a toggle of the mode is turned down.
 *
 * <p>Worked out by {@link PlayerRules#refusal}, which the client checks before sending a toggle and
 * the server checks again on receiving one, so the two can never disagree on the rules themselves.
 * The ordinal is the wire id, so new reasons go at the end.
 */
public enum Refusal {
	/** The player's permission level is below the server's minimum. */
	NOT_ALLOWED,
	/** The server keeps the mode on for everyone. */
	FORCED_ON,
	/** The server has the mode switched off for everyone. */
	DISABLED,
	/** The player toggled again before the cooldown ran out. */
	COOLDOWN;

	private static final Refusal[] VALUES = values();

	/** Unknown ids, from a newer server, fall back to the most general reason. */
	public static Refusal byId(final int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : NOT_ALLOWED;
	}
}
