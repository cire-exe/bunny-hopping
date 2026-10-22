package com.kishi.bhop.hud;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.config.ModConfig;
import com.kishi.bhop.movement.BhopMode;
import com.kishi.bhop.movement.HopTracker;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Strafe feedback for the last hop, on a line beneath the speedometer: the speed it gained, whether
 * it was timed perfectly, and how well the mouse matched the strafe keys.
 *
 * <p>Each part has its own toggle. The line stays up while hops keep coming and clears a few
 * seconds after the last one.
 */
public final class HopFeedbackHudElement implements HudElement {

	private static final int GREEN = 0x55FF55;
	private static final int GOLD = 0xFFC24A;
	private static final int RED = 0xFF5555;

	private static final String SEPARATOR = "   ";

	/** Sync at or above this is good, and at or above the lower one is fair. */
	private static final int GOOD_SYNC = 80;
	private static final int FAIR_SYNC = 50;

	/**
	 * The last line built, and the hop and toggles it was built from. A hop is reported once and
	 * then stays on screen for seconds, so most frames reuse the line instead of building it again.
	 */
	private HopTracker.Hop shownHop;
	private int shownParts;
	private Component shownLine = Component.empty();

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		final HopTracker.Hop hop = HopTracker.lastHop();
		if (hop == null || !BhopMode.isActive()) {
			return;
		}

		final ModConfig.Hud cfg = ConfigManager.get().hud;
		final int parts = (cfg.showGain ? 1 : 0) | (cfg.showHopTiming ? 2 : 0) | (cfg.showSync ? 4 : 0);
		if (hop != this.shownHop || parts != this.shownParts) {
			this.shownHop = hop;
			this.shownParts = parts;
			this.shownLine = line(hop, cfg);
		}

		if (!this.shownLine.getSiblings().isEmpty()) {
			SpeedHudElement.drawLine(graphics, this.shownLine, 1);
		}
	}

	private static Component line(final HopTracker.Hop hop, final ModConfig.Hud cfg) {
		final MutableComponent line = Component.empty();
		if (cfg.showGain) {
			append(line, Component.literal("%+.2f".formatted(hop.gain())).withColor(hop.gain() >= 0.0D ? GREEN : RED));
		}

		if (cfg.showHopTiming) {
			append(line, timing(hop));
		}

		if (cfg.showSync && hop.hasSync()) {
			append(line, Component.translatable("bhop.hud.sync", hop.syncPercent()).withColor(syncColour(hop.syncPercent())));
		}

		return line;
	}

	private static Component timing(final HopTracker.Hop hop) {
		if (!hop.perfect()) {
			return Component.translatable(hop.groundTicks() == 1 ? "bhop.hud.late_one" : "bhop.hud.late", hop.groundTicks())
					.withColor(hop.groundTicks() == 1 ? GOLD : RED);
		}

		return (hop.perfectStreak() > 1
				? Component.translatable("bhop.hud.perfect_streak", hop.perfectStreak())
				: Component.translatable("bhop.hud.perfect")).withColor(GREEN);
	}

	private static int syncColour(final int percent) {
		if (percent >= GOOD_SYNC) {
			return GREEN;
		}

		return percent >= FAIR_SYNC ? GOLD : RED;
	}

	private static void append(final MutableComponent line, final Component part) {
		if (!line.getSiblings().isEmpty()) {
			line.append(SEPARATOR);
		}

		line.append(part);
	}
}
