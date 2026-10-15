package com.kishi.bhop;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.hud.HopFeedbackHudElement;
import com.kishi.bhop.hud.SpeedHudElement;
import com.kishi.bhop.hud.StatusMessageHudElement;
import com.kishi.bhop.input.ModKeyMappings;
import com.kishi.bhop.session.BhopToggle;
import com.kishi.bhop.session.ServerSession;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;

/**
 * Client entry point.
 *
 * <p>Everything the player sees and controls lives here: Source movement for the local player, the
 * HUD and the settings screen. What is shared with servers is set up by {@link BhopMod}.
 */
public final class BhopClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ConfigManager.load();
		ModKeyMappings.register();
		ServerSession.register();
		HudElementRegistry.addLast(BhopMod.id("speed"), new SpeedHudElement());
		HudElementRegistry.addLast(BhopMod.id("hop_feedback"), new HopFeedbackHudElement());
		HudElementRegistry.addLast(BhopMod.id("status_message"), new StatusMessageHudElement());
		ClientTickEvents.END_CLIENT_TICK.register(BhopClient::onEndTick);

		BhopMod.LOGGER.info("BHOP client ready");
	}

	private static void onEndTick(final Minecraft client) {
		BhopToggle.tick();
		ModKeyMappings.handleInput(client);
		StatusMessageHudElement.tick();
	}
}
