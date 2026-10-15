package com.kishi.bhop.hud;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.config.ModConfig;
import com.kishi.bhop.movement.BhopMode;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.SharedConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import org.joml.Matrix3x2fStack;

/**
 * Horizontal speed readout, drawn above the hotbar while bunny hop mode is active.
 *
 * <p>A HUD element rather than an action bar message, because the action bar is a fixed size and
 * would also stamp over anything else trying to use it. Drawing it here means the size is ours to
 * choose, and the toggle notification can keep the action bar to itself.
 */
public final class SpeedHudElement implements HudElement {

	private static final int WHITE = 0xFFFFFFFF;

	/** Gap between the speedometer's lines, in unscaled font pixels. */
	private static final int LINE_SPACING = 1;

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		final Minecraft client = Minecraft.getInstance();
		if (!ConfigManager.get().hud.showSpeed || !BhopMode.isActive() || client.player == null) {
			return;
		}

		// Distance actually covered last tick, rather than the stored velocity: blocks like slime and
		// soul sand scale the velocity after the move, so it under-reports how fast you went.
		final LocalPlayer player = client.player;
		final double blocksPerSecond =
				Math.hypot(player.getX() - player.xo, player.getZ() - player.zo) * SharedConstants.TICKS_PER_SECOND;
		drawLine(graphics, Component.literal("%.2f b/s".formatted(blocksPerSecond)), 0);
	}

	/**
	 * Draws one centred line of the speedometer block, at the configured text size and height.
	 *
	 * @param line 0 for the speed itself, 1 for the line beneath it, and so on
	 */
	static void drawLine(final GuiGraphicsExtractor graphics, final Component text, final int line) {
		final ModConfig.Hud cfg = ConfigManager.get().hud;
		final Font font = Minecraft.getInstance().font;
		final float scale = cfg.speedTextScale;

		// Positions are applied inside the scaled space, so divide the screen coordinates through
		// by the scale to keep the readout where it belongs regardless of the text size.
		final int x = Math.round(graphics.guiWidth() / 2.0F / scale) - font.width(text) / 2;
		final int y = Math.round((graphics.guiHeight() - cfg.speedOffsetY) / scale) + line * (font.lineHeight + LINE_SPACING);

		final Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		graphics.text(font, text, x, y, WHITE);
		pose.popMatrix();
	}
}
