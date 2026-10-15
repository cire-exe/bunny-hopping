package com.kishi.bhop.session;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.hud.StatusMessageHudElement;
import com.kishi.bhop.movement.BhopMode;
import com.kishi.bhop.network.BhopStatePayload;
import com.kishi.bhop.network.SetBhopStatePayload;
import com.kishi.bhop.rules.PlayerRules;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

/**
 * Every way the player's bunny hop choice can change: the toggle key, the settings screen, the
 * server's answer to a toggle, and a change in the server's rules.
 *
 * <p>Toggles are checked against the rules here first, so an honest client never sends one the
 * server would refuse. The change applies straight away and the server is told afterwards, so the
 * mode never waits on a round trip.
 */
public final class BhopToggle {

	private static final double TICKS_PER_SECOND = 20.0D;

	/** Ticks left before the player may toggle again. */
	private static int cooldownTicks;

	private BhopToggle() {
	}

	/** Counts the cooldown down. Called once per client tick. */
	public static void tick() {
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
	}

	/** Flips the player's choice, if the rules allow it. */
	public static void toggle() {
		request(!BhopMode.isActive());
	}

	/**
	 * Sets the player's choice, if the rules allow it, and says why not if they do not.
	 *
	 * @return whether the change was made
	 */
	public static boolean request(final boolean enabled) {
		if (enabled == BhopMode.isActive()) {
			return true;
		}

		final Component refusal = refusal(ServerSession.rules());
		if (refusal != null) {
			StatusMessageHudElement.show(refusal);
			return false;
		}

		BhopMode.setEnabled(enabled);
		cooldownTicks = ServerSession.rules().toggleCooldownTicks();
		if (ServerSession.serverTracksState()) {
			ClientPlayNetworking.send(new SetBhopStatePayload(enabled));
		}

		announce(enabled ? "bhop.message.bhop_on" : "bhop.message.bhop_off");
		return true;
	}

	/** The server turned a toggle down: go back to the state it has, and say why. */
	static void onRefused(final BhopStatePayload payload) {
		BhopMode.setEnabled(payload.enabled());
		StatusMessageHudElement.show(Component.translatable(switch (payload.reason()) {
			case NOT_ALLOWED -> "bhop.message.refused.not_allowed";
			case FORCED_ON -> "bhop.message.refused.forced_on";
			case DISABLED -> "bhop.message.refused.disabled";
			case COOLDOWN -> "bhop.message.refused.cooldown_short";
		}));
	}

	/**
	 * Resolves the player's choice under new rules, exactly as the server does, and tells them if
	 * that switched the mode for them.
	 */
	static void onRulesChanged(final PlayerRules rules) {
		final boolean wasActive = BhopMode.isActive();
		BhopMode.setEnabled(rules.resolve(BhopMode.isEnabled()));
		if (BhopMode.isActive() != wasActive) {
			announce(BhopMode.isActive() ? "bhop.message.server_on" : "bhop.message.server_off");
		}
	}

	/** Leaving a server leaves the mode off, so it is never on by surprise on the next one. */
	static void reset() {
		BhopMode.setEnabled(false);
		cooldownTicks = 0;
	}

	/** Why the player cannot toggle right now, or {@code null} if they can. */
	private static Component refusal(final PlayerRules rules) {
		if (!rules.allowed()) {
			return ServerSession.notAllowedMessage();
		}

		return switch (rules.policy()) {
			case FORCED_ON -> Component.translatable("bhop.message.refused.forced_on");
			case DISABLED -> Component.translatable("bhop.message.refused.disabled");
			case OPTIONAL -> cooldownTicks > 0
					? Component.translatable("bhop.message.refused.cooldown", "%.1f".formatted(cooldownTicks / TICKS_PER_SECOND))
					: null;
		};
	}

	private static void announce(final String key) {
		if (ConfigManager.get().hud.announceToggle) {
			StatusMessageHudElement.show(Component.translatable(key));
		}
	}
}
