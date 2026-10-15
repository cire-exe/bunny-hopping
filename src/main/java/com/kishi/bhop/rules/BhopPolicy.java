package com.kishi.bhop.rules;

/** What a server allows players to do with the bunny hop toggle. */
public enum BhopPolicy {

	/** Each player switches the mode on and off as they like. */
	OPTIONAL,

	/** Everyone permitted to use the mode has it on, and cannot switch it off. */
	FORCED_ON,

	/** Nobody can switch the mode on. */
	DISABLED;

	private static final BhopPolicy[] VALUES = values();

	/** Unknown ids, from a newer server, are treated as the most restrictive policy. */
	public static BhopPolicy byId(final int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : DISABLED;
	}

	/** Whether players choose the state themselves rather than the server choosing for them. */
	public boolean playerChooses() {
		return this == OPTIONAL;
	}
}
