package com.kishi.bhop.movement;

import com.kishi.bhop.session.ServerSession;

/**
 * Tracks whether Source movement is currently replacing vanilla movement.
 *
 * <p>Two things decide it: the player's own choice, and the rules of the server they are on. A
 * server can keep the mode on for everyone or switch it off, so anything asking whether the mode
 * is in effect should use {@link #isActive}. Changing the choice goes through
 * {@link com.kishi.bhop.session.BhopToggle}, which checks the rules first.
 */
public final class BhopMode {

	private static boolean enabled;

	private BhopMode() {
	}

	/** Whether Source movement is in effect right now. */
	public static boolean isActive() {
		return ServerSession.rules().resolve(enabled);
	}

	/** The player's own choice, which the server's rules may override. */
	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(final boolean value) {
		enabled = value;
	}
}
