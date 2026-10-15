package com.kishi.bhop.gui;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.config.ModConfig;

import net.minecraft.network.chat.Component;

/** What the mod draws on screen: the speedometer, hop feedback beneath it, and the toggle message. */
final class HudPage implements SettingsPage {

	private static final String KEY = "bhop.settings.hud.";

	@Override
	public Component title() {
		return Component.translatable("bhop.settings.section.hud");
	}

	@Override
	public void build(final PageBuilder page) {
		final ModConfig.Hud cfg = ConfigManager.get().hud;

		page.toggle(KEY + "show_speed", cfg.showSpeed, value -> cfg.showSpeed = value);
		page.toggle(KEY + "show_gain", cfg.showGain, value -> cfg.showGain = value);
		page.toggle(KEY + "show_hop_timing", cfg.showHopTiming, value -> cfg.showHopTiming = value);
		page.toggle(KEY + "show_sync", cfg.showSync, value -> cfg.showSync = value);
		page.toggle(KEY + "announce_toggle", cfg.announceToggle, value -> cfg.announceToggle = value);
	}
}
