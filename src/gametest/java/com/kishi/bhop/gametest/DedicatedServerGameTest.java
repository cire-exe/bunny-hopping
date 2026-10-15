package com.kishi.bhop.gametest;

import static com.kishi.bhop.gametest.TestSupport.check;
import static com.kishi.bhop.gametest.TestSupport.clientActive;
import static com.kishi.bhop.gametest.TestSupport.pressToggle;
import static com.kishi.bhop.gametest.TestSupport.serverState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.gui.SettingsScreen;
import com.kishi.bhop.network.SetBhopStatePayload;
import com.kishi.bhop.server.ServerConfig;
import com.kishi.bhop.session.BhopToggle;
import com.kishi.bhop.session.ServerSession;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Remote servers end to end: one running the mod decides the physics and tracks the toggle, and
 * one without it keeps the mode off however the player asks for it.
 */
public final class DedicatedServerGameTest implements FabricClientGameTest {

	private static final int COOLDOWN_TICKS = 10;

	/** Different from the default, so it shows whose physics the client is using. */
	private static final double SERVER_SPEED_CAP = 20.0D;

	@Override
	public void runTest(final ClientGameTestContext context) {
		acceptEula();
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			moddedServer(context, server);
			unmoddedServer(context, server);
		}
	}

	private static void moddedServer(final ClientGameTestContext context, final TestDedicatedServerContext server) {
		try (TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();

			check(context.computeOnClient(client -> ServerSession.kind()) == ServerSession.Kind.MODDED, "session should be MODDED");
			check(context.computeOnClient(client -> ServerSession.serverControlsPhysics()), "server should control the physics");
			check(serverState(server) == Boolean.FALSE, "server should track the player, with the mode off");

			// The server's physics replace the player's own, live.
			server.runCommand("bhop physics maxTotalSpeed " + SERVER_SPEED_CAP);
			connection.waitForClientboundPackets();
			check(ServerConfig.get().physics.maxTotalSpeed == SERVER_SPEED_CAP, "physics command should apply on a dedicated server");
			check(context.computeOnClient(client -> ServerSession.physics().maxTotalSpeed) == SERVER_SPEED_CAP, "client should move with the server's physics");
			check(context.computeOnClient(client -> ConfigManager.get().physics.maxTotalSpeed) != SERVER_SPEED_CAP, "player's own physics should be untouched");

			// The toggle reaches the server.
			pressToggle(context);
			connection.waitForServerboundPackets();
			check(clientActive(context), "toggle key should switch the mode on");
			check(serverState(server) == Boolean.TRUE, "server should see the mode on");

			context.setScreen(SettingsScreen::new);
			context.waitTicks(2);
			context.takeScreenshot("bhop-settings-modded-server");
			context.setScreen(() -> null);

			server.runCommand("bhop physics reset");
			connection.waitForClientboundPackets();
		}

		// Leaving switches the mode off and forgets the server's physics.
		check(!clientActive(context), "leaving should switch the mode off");
		check(context.computeOnClient(client -> ServerSession.kind()) == ServerSession.Kind.NONE, "session should be NONE after leaving");
	}

	/**
	 * Makes the server look like one without the mod by taking away its receiver for the toggle,
	 * which is what the client checks for. The server still sends its settings on joining, which an
	 * unmodded one never would, so this also shows they are ignored.
	 */
	private static void unmoddedServer(final ClientGameTestContext context, final TestDedicatedServerContext server) {
		final ServerPlayNetworking.PlayPayloadHandler<SetBhopStatePayload> receiver = server.computeOnServer(s -> unregisterToggleReceiver());
		check(receiver != null, "the server should have had a toggle receiver to remove");

		try (TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();

			check(context.computeOnClient(client -> ServerSession.kind()) == ServerSession.Kind.UNMODDED, "session should be UNMODDED");
			check(!context.computeOnClient(client -> ServerSession.rules().allowed()), "mode should not be allowed");
			check(!context.computeOnClient(client -> ServerSession.serverControlsPhysics()), "an unmodded server should not control the physics");
			check(!clientActive(context), "mode should start off");

			// Neither the key nor the settings screen can switch it on.
			pressToggle(context);
			check(!clientActive(context), "toggle key should be refused");
			context.waitTicks(COOLDOWN_TICKS);
			check(!context.computeOnClient(client -> BhopToggle.request(true)), "settings toggle should be refused");
			check(!clientActive(context), "mode should still be off");

			// Even a policy that forces the mode on, sent by a server the client can't vouch for.
			server.runCommand("bhop policy forced_on");
			connection.waitForClientboundPackets();
			check(!clientActive(context), "server settings should be ignored on an unmodded server");
			server.runCommand("bhop policy optional");
			connection.waitForClientboundPackets();

			context.setScreen(SettingsScreen::new);
			context.waitTicks(2);
			context.takeScreenshot("bhop-settings-unmodded-server");
			context.setScreen(() -> null);
		} finally {
			server.runOnServer(s -> ServerPlayNetworking.registerGlobalReceiver(SetBhopStatePayload.TYPE, receiver));
		}
	}

	/**
	 * The test server refuses to start without the Minecraft EULA accepted, and Loom empties the
	 * run folder before every test run, so the acceptance is written again each time.
	 */
	private static void acceptEula() {
		try {
			Files.writeString(Path.of("eula.txt"), "eula=true\n");
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not write eula.txt for the test server", e);
		}
	}

	@SuppressWarnings("unchecked")
	private static ServerPlayNetworking.PlayPayloadHandler<SetBhopStatePayload> unregisterToggleReceiver() {
		return (ServerPlayNetworking.PlayPayloadHandler<SetBhopStatePayload>) ServerPlayNetworking.unregisterGlobalReceiver(SetBhopStatePayload.TYPE.id());
	}
}
