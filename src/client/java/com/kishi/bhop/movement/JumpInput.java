package com.kishi.bhop.movement;

import com.kishi.bhop.config.ModConfig;
import com.kishi.bhop.session.ServerSession;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Decides whether the player is asking to jump this tick.
 *
 * <p>Three things count:
 * <ul>
 *   <li>holding the jump key, while auto hop is on;</li>
 *   <li>a press of the jump key, remembered for a few ticks, so a press made just before landing
 *       still hops on the landing tick. Presses are counted rather than read from the key state, so a
 *       tap shorter than a tick is not lost;</li>
 *   <li>a turn of the mouse wheel, when scroll jumping is on, remembered the same way.</li>
 * </ul>
 */
public final class JumpInput {

	/** Set by a wheel turn between ticks, picked up on the next one. */
	private static boolean scrolled;

	/** Ticks left in which a recent press still counts, including this one. */
	private static int bufferedTicks;

	private JumpInput() {
	}

	/**
	 * Whether a mouse wheel turn should jump instead of changing the hotbar slot right now.
	 *
	 * <p>Only while bunny hop mode is on, so scrolling through the hotbar works as normal otherwise.
	 */
	public static boolean wantsScroll(final ModConfig.Movement cfg) {
		return cfg.scrollJump && BhopMode.isActive();
	}

	/** Records a mouse wheel turn as a jump press. */
	public static void scroll() {
		scrolled = true;
	}

	/** Reads this tick's jump request. Call exactly once per tick, on the ground or not. */
	static boolean poll(final LocalPlayer player, final ModConfig.Movement cfg) {
		if (drainPresses() || scrolled) {
			bufferedTicks = ServerSession.jumpBufferTicks(cfg) + 1;
		}

		scrolled = false;

		final boolean requested = bufferedTicks > 0 || ServerSession.autoHop(cfg) && player.input.keyPresses.jump();
		if (bufferedTicks > 0) {
			bufferedTicks--;
		}

		return requested;
	}

	/** Spends the buffered press on a jump, so one press is never two hops. */
	static void consume() {
		bufferedTicks = 0;
	}

	/** Discards anything pending, for when Source movement is not driving the player. */
	static void reset() {
		drainPresses();
		scrolled = false;
		bufferedTicks = 0;
	}

	private static boolean drainPresses() {
		boolean pressed = false;
		while (Minecraft.getInstance().options.keyJump.consumeClick()) {
			pressed = true;
		}

		return pressed;
	}
}
