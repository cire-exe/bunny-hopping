package com.kishi.bhop.hud;

import com.kishi.bhop.config.ConfigManager;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import org.joml.Matrix3x2fStack;

/**
 * A short status line in the action bar's spot, such as the bunny hop toggle notice.
 *
 * <p>Vanilla's action bar draws at one fixed size, so the mod draws its own: same position, same
 * three seconds on screen and one second fade, but at a size of our choosing.
 */
public final class StatusMessageHudElement implements HudElement {

	/** How long a message stays up, matching vanilla's action bar. */
	private static final int DURATION_TICKS = 60;

	/** The final stretch over which it fades out. */
	private static final int FADE_TICKS = 20;

	/** Height above the bottom of the screen, matching vanilla's action bar. */
	private static final int OFFSET_Y = 68;

	private static Component message;
	private static int remainingTicks;

	/** Shows a message, replacing any still on screen. */
	public static void show(final Component text) {
		message = text;
		remainingTicks = DURATION_TICKS;
	}

	/** Counts the message down. Called once per client tick. */
	public static void tick() {
		if (remainingTicks > 0) {
			remainingTicks--;
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		if (message == null || remainingTicks <= 0) {
			return;
		}

		final float ticksLeft = remainingTicks - deltaTracker.getGameTimeDeltaPartialTick(false);
		final int alpha = Math.min(255, (int) (ticksLeft * 255.0F / FADE_TICKS));
		if (alpha <= 0) {
			return;
		}

		final Minecraft client = Minecraft.getInstance();
		final Font font = client.font;
		final float scale = textScale(font, client.getWindow().getGuiScale());
		final int width = font.width(message);

		// Scale about the anchor point, so the text shrinks towards its own centre.
		final Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() - OFFSET_Y);
		pose.scale(scale, scale);
		graphics.textWithBackdrop(font, message, -width / 2, -font.lineHeight / 2, width, ARGB.white(alpha));
		pose.popMatrix();
	}

	/**
	 * The scale that makes the text the configured number of real screen pixels shorter than
	 * vanilla's.
	 *
	 * <p>Worked out from the live GUI scale rather than fixed, because one GUI pixel is several
	 * screen pixels and how many depends on the GUI scale setting and the window size.
	 */
	private static float textScale(final Font font, final int guiScale) {
		final float vanillaHeight = font.lineHeight * (float) guiScale;
		final int shrink = ConfigManager.get().hud.statusTextShrinkPixels;
		return Math.max(0.1F, (vanillaHeight - shrink) / vanillaHeight);
	}
}
