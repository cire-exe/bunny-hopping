package com.kishi.bhop.gametest;

import java.util.Arrays;

import com.kishi.bhop.movement.BhopMode;
import com.kishi.bhop.server.BhopServer;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Small helpers shared by the game tests. */
final class TestSupport {

	private TestSupport() {
	}

	static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/** The mod's bunny hop toggle key, found the way the controls screen finds it. */
	static KeyMapping toggleKey(final ClientGameTestContext context) {
		return context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
				.filter(mapping -> mapping.getName().equals("key.bhop.toggle_bhop"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("toggle key not registered")));
	}

	/** Presses the toggle key and lets the client handle it. */
	static void pressToggle(final ClientGameTestContext context) {
		context.getInput().pressKey(toggleKey(context));
		context.waitTick();
	}

	static boolean clientActive(final ClientGameTestContext context) {
		return context.computeOnClient(client -> BhopMode.isActive());
	}

	/** The state the server holds for the only player, or {@code null} if it does not track them. */
	static Boolean serverState(final TestServerContext server) {
		return server.computeOnServer(s -> BhopServer.stateOf(s.getPlayerList().getPlayers().getFirst()));
	}

	static double horizontalSpeed(final Minecraft client) {
		return client.player.getDeltaMovement().horizontalDistance() * 20.0D;
	}
}
