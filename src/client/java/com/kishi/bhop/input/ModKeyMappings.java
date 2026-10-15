package com.kishi.bhop.input;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.kishi.bhop.BhopMod;
import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.gui.SettingsScreen;
import com.kishi.bhop.session.BhopToggle;
import com.kishi.bhop.session.ServerSession;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Registers the mod's key bindings and dispatches them each tick.
 *
 * <p>Every binding appears under its own "BHOP" category in Options -> Controls -> Key Binds and
 * can be rebound there like any vanilla control. Adding one is a single {@link #bind} line in
 * {@link #register} plus its {@code key.bhop.<name>} entry in the lang file.
 */
public final class ModKeyMappings {

	/** Custom category so the bindings are grouped together in the controls screen. */
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(BhopMod.id("main"));

	/** A registered key and what it does when pressed. */
	private record Binding(KeyMapping mapping, Consumer<Minecraft> action) {
	}

	private static final List<Binding> BINDINGS = new ArrayList<>();

	private ModKeyMappings() {
	}

	public static void register() {
		bind("toggle_bhop", InputConstants.KEY_V, client -> BhopToggle.toggle());
		bind("open_settings", InputConstants.KEY_U, client -> client.gui.setScreen(new SettingsScreen()));
		bind("reload", InputConstants.KEY_R, ModKeyMappings::reload);
	}

	private static void bind(final String name, final int defaultKey, final Consumer<Minecraft> action) {
		final KeyMapping mapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.bhop." + name, InputConstants.Type.KEYBOARD, defaultKey, CATEGORY));
		BINDINGS.add(new Binding(mapping, action));
	}

	/** Drains any queued presses. Called once per client tick. */
	public static void handleInput(final Minecraft client) {
		if (client.player == null) {
			return;
		}

		for (final Binding binding : BINDINGS) {
			while (binding.mapping().consumeClick()) {
				binding.action().accept(client);
			}
		}
	}

	private static void reload(final Minecraft client) {
		ConfigManager.load();
		ServerSession.publishHostPhysics(client);
		client.player.sendOverlayMessage(Component.translatable("bhop.message.reloaded"));
	}
}
