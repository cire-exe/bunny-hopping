package com.kishi.bhop.rules;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The server's rules as they apply to one player, which is what the client receives.
 *
 * @param policy whether the player or the server decides the mode's state
 * @param allowed whether this player may use the mode at all
 * @param allowAutoHop whether holding jump may chain hops
 * @param maxJumpBufferTicks the longest jump buffer the player may use
 * @param toggleCooldownTicks ticks to wait between switching the mode on and off
 */
public record PlayerRules(
		BhopPolicy policy, boolean allowed, boolean allowAutoHop, int maxJumpBufferTicks, int toggleCooldownTicks) {

	/** The rules of a world with no restrictions, such as singleplayer. */
	public static final PlayerRules UNRESTRICTED =
			new PlayerRules(BhopPolicy.OPTIONAL, true, true, ServerRules.MAX_JUMP_BUFFER_TICKS, 0);

	/** Values are clamped on the way in, since they come from a server the client cannot vouch for. */
	public static final StreamCodec<ByteBuf, PlayerRules> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.idMapper(BhopPolicy::byId, BhopPolicy::ordinal),
			PlayerRules::policy,
			ByteBufCodecs.BOOL,
			PlayerRules::allowed,
			ByteBufCodecs.BOOL,
			PlayerRules::allowAutoHop,
			ByteBufCodecs.VAR_INT.map(ticks -> Math.clamp(ticks, 0, ServerRules.MAX_JUMP_BUFFER_TICKS), ticks -> ticks),
			PlayerRules::maxJumpBufferTicks,
			ByteBufCodecs.VAR_INT.map(ticks -> Math.clamp(ticks, 0, ServerRules.MAX_TOGGLE_COOLDOWN_TICKS), ticks -> ticks),
			PlayerRules::toggleCooldownTicks,
			PlayerRules::new);

	/** Whether the player may switch the mode themselves right now, cooldown aside. */
	public boolean playerChooses() {
		return this.allowed && this.policy.playerChooses();
	}

	/** Whether the server keeps the mode on for this player regardless of their choice. */
	public boolean forcedOn() {
		return this.allowed && this.policy == BhopPolicy.FORCED_ON;
	}

	/** Whether this player can have the mode on at all. */
	public boolean permitsMode() {
		return this.allowed && this.policy != BhopPolicy.DISABLED;
	}

	/**
	 * The state a player who chose {@code enabled} actually ends up in under these rules.
	 *
	 * <p>Client and server both apply this whenever the rules change, so they agree on the state
	 * without having to exchange it.
	 */
	public boolean resolve(final boolean enabled) {
		return this.forcedOn() || this.permitsMode() && enabled;
	}
}
