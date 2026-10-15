package com.kishi.bhop.gui;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.config.ModConfig;
import com.kishi.bhop.movement.BhopMode;
import com.kishi.bhop.physics.PhysicsSettings;
import com.kishi.bhop.rules.PlayerRules;
import com.kishi.bhop.rules.ServerRules;
import com.kishi.bhop.session.BhopToggle;
import com.kishi.bhop.session.ServerSession;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Movement and jump options, and what the current server allows.
 *
 * <p>Options the server decides are still shown, but greyed out with a tooltip saying why, so the
 * player can see what changed rather than wondering where a setting went.
 */
final class GeneralPage implements SettingsPage {

	private static final String KEY = "bhop.settings.general.";
	private static final int STATUS_COLOUR = 0xFFA0A0A0;

	@Override
	public Component title() {
		return Component.translatable("bhop.settings.section.general");
	}

	@Override
	public void build(final PageBuilder page) {
		final ModConfig.Movement cfg = ConfigManager.get().movement;
		final PhysicsSettings physics = ConfigManager.get().physics;
		final PlayerRules rules = ServerSession.rules();

		page.label(page.x(), page.y(), GeneralPage::status, STATUS_COLOUR);
		page.nextRow(Minecraft.getInstance().font.lineHeight);

		final AbstractWidget mode = page.toggle(KEY + "bhop_mode", BhopMode.isActive(), value -> {
			BhopToggle.request(value);
			// Shows the real state again if the request was refused.
			page.screen().refresh();
		});
		mode.active = rules.playerChooses();

		final AbstractWidget autoHop = page.toggle(KEY + "auto_hop", cfg.autoHop, value -> cfg.autoHop = value);
		if (!rules.allowAutoHop()) {
			lockedByServer(autoHop);
		}

		page.slider(KEY + "jump_buffer", 0.0D, ServerRules.MAX_JUMP_BUFFER_TICKS, 1.0D, "%.0f ticks",
				() -> cfg.jumpBufferTicks, value -> cfg.jumpBufferTicks = (int) value);
		page.toggle(KEY + "scroll_jump", cfg.scrollJump, value -> cfg.scrollJump = value);

		// The player's own physics. A server running the mod replaces them while connected.
		final boolean serverPhysics = ServerSession.serverControlsPhysics();
		lockIf(serverPhysics, page.slider(KEY + "speed_cap", 5.0D, 30.0D, 0.5D, "%.1f b/s",
				() -> physics.maxTotalSpeed, value -> physics.maxTotalSpeed = value));
		lockIf(serverPhysics, page.toggle(KEY + "ramp_up", physics.rampUp, value -> physics.rampUp = value));
		lockIf(serverPhysics, page.slider(KEY + "ramp_up_multiplier", 1.0D, 4.0D, 0.1D, "%.1fx",
				() -> physics.rampUpMultiplier, value -> physics.rampUpMultiplier = value));
		lockIf(serverPhysics, page.slider(KEY + "ramp_up_speed", 5.0D, 15.0D, 0.5D, "%.1f b/s",
				() -> physics.rampUpSpeed, value -> physics.rampUpSpeed = value));
	}

	/** One line on what the current server allows, re-read every frame. */
	private static Component status() {
		final PlayerRules rules = ServerSession.rules();
		return Component.translatable(switch (ServerSession.kind()) {
			case NONE -> "bhop.settings.general.status.none";
			case LOCAL -> "bhop.settings.general.status.local";
			case INCOMPATIBLE -> "bhop.settings.general.status.incompatible";
			case UNMODDED -> "bhop.settings.general.status.unmodded";
			case MODDED -> !rules.allowed()
					? "bhop.settings.general.status.not_allowed"
					: switch (rules.policy()) {
						case OPTIONAL -> "bhop.settings.general.status.optional";
						case FORCED_ON -> "bhop.settings.general.status.forced_on";
						case DISABLED -> "bhop.settings.general.status.disabled";
					};
		});
	}

	private static void lockIf(final boolean locked, final AbstractWidget widget) {
		if (locked) {
			lockedByServer(widget);
		}
	}

	private static void lockedByServer(final AbstractWidget widget) {
		widget.active = false;
		widget.setTooltip(Tooltip.create(Component.translatable(KEY + "set_by_server")));
	}
}
