package com.kishi.bhop.rules;

import com.kishi.bhop.config.Values;

/**
 * A server's rules for bunny hop mode, as stored in {@code config/bhop-server.json}.
 *
 * <p>Only the server reads this. What each player receives is a {@link PlayerRules}, worked out
 * from these for that player, so the permission level itself never leaves the server.
 */
public final class ServerRules {

	/** Half a second; any longer and a press would hop long after it was meant to. */
	public static final int MAX_JUMP_BUFFER_TICKS = 10;

	/** Ten seconds. A longer cooldown stops being a guard against spam and becomes a punishment. */
	public static final int MAX_TOGGLE_COOLDOWN_TICKS = 200;

	/** Vanilla's highest permission level, for server owners. */
	public static final int MAX_PERMISSION_LEVEL = 4;

	public BhopPolicy policy = BhopPolicy.OPTIONAL;

	/** Whether holding jump may chain hops. Off makes every hop a timed press. */
	public boolean allowAutoHop = true;

	/** The longest jump buffer a player may use, in ticks. 0 forces exact timing. */
	public int maxJumpBufferTicks = MAX_JUMP_BUFFER_TICKS;

	/** Ticks a player has to wait between switching the mode on and off. */
	public int toggleCooldownTicks = 10;

	/** Lowest vanilla permission level allowed to use the mode: 0 is everyone, 2 is operators. */
	public int requiredPermissionLevel = 0;

	public void sanitize() {
		if (this.policy == null) {
			this.policy = BhopPolicy.OPTIONAL;
		}

		this.maxJumpBufferTicks = Values.inRange("rules.maxJumpBufferTicks", this.maxJumpBufferTicks, 0, MAX_JUMP_BUFFER_TICKS);
		this.toggleCooldownTicks = Values.inRange(
				"rules.toggleCooldownTicks", this.toggleCooldownTicks, 0, MAX_TOGGLE_COOLDOWN_TICKS);
		this.requiredPermissionLevel = Values.inRange(
				"rules.requiredPermissionLevel", this.requiredPermissionLevel, 0, MAX_PERMISSION_LEVEL);
	}

	/** The rules as one player sees them. */
	public PlayerRules forPlayer(final boolean allowed) {
		return new PlayerRules(this.policy, allowed, this.allowAutoHop, this.maxJumpBufferTicks, this.toggleCooldownTicks);
	}
}
