package com.kishi.bhop.gametest;

import static com.kishi.bhop.gametest.TestSupport.check;
import static com.kishi.bhop.gametest.TestSupport.clientActive;
import static com.kishi.bhop.gametest.TestSupport.pressToggle;
import static com.kishi.bhop.gametest.TestSupport.serverState;

import com.kishi.bhop.BhopMod;
import com.kishi.bhop.gui.SettingsScreen;
import com.kishi.bhop.rules.BhopPolicy;
import com.kishi.bhop.server.ServerConfig;
import com.kishi.bhop.session.ServerSession;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.phys.Vec3;

/**
 * Singleplayer end to end: the integrated server hands the host their rules, the toggle key goes
 * through those rules and reaches the server, and {@code /bhop} changes reach the client live.
 */
public final class SingleplayerGameTest implements FabricClientGameTest {

	private static final int COOLDOWN_TICKS = 10;

	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			final TestServerContext server = world.getServer();

			// Joining: the client knows it is the host, and has the integrated server's rules.
			check(context.computeOnClient(client -> ServerSession.kind()) == ServerSession.Kind.LOCAL, "session should be LOCAL");
			check(context.computeOnClient(client -> ServerSession.rules().allowed()), "host should be allowed to use the mode");
			check(serverState(server) == Boolean.FALSE, "server should track the host, with the mode off");

			// The toggle key switches the mode on, and the server hears about it.
			pressToggle(context);
			world.getConnection().waitForServerboundPackets();
			check(clientActive(context), "toggle key should switch the mode on");
			check(serverState(server) == Boolean.TRUE, "server should see the mode on");

			// A second press within the cooldown is refused.
			pressToggle(context);
			check(clientActive(context), "a press during the cooldown should be refused");

			// After the cooldown it goes through.
			context.waitTicks(COOLDOWN_TICKS);
			pressToggle(context);
			world.getConnection().waitForServerboundPackets();
			check(!clientActive(context), "toggle after the cooldown should switch the mode off");
			check(serverState(server) == Boolean.FALSE, "server should see the mode off");

			// Policies take effect on the client straight away.
			runCommand(world, "bhop policy forced_on");
			check(clientActive(context), "forced_on should switch the mode on");
			context.waitTicks(COOLDOWN_TICKS);
			pressToggle(context);
			check(clientActive(context), "forced_on should keep the mode on through a toggle");

			runCommand(world, "bhop policy disabled");
			check(!clientActive(context), "disabled should switch the mode off");
			context.waitTicks(COOLDOWN_TICKS);
			pressToggle(context);
			check(!clientActive(context), "disabled should keep the mode off through a toggle");

			runCommand(world, "bhop policy optional");
			check(context.computeOnClient(client -> ServerSession.rules().policy()) == BhopPolicy.OPTIONAL, "policy should be back to optional");

			// Rules reach the client.
			runCommand(world, "bhop rule allowAutoHop false");
			check(!context.computeOnClient(client -> ServerSession.rules().allowAutoHop()), "auto hop should be disallowed");
			runCommand(world, "bhop rule allowAutoHop true");

			// A world opened from the client takes its physics from the host's screen, not the file.
			final double fileSpeed = ServerConfig.get().physics.maxTotalSpeed;
			runCommand(world, "bhop physics maxTotalSpeed 20");
			check(ServerConfig.get().physics.maxTotalSpeed == fileSpeed, "physics command should be refused in a host's world");

			// Movement runs with the mode on.
			context.waitTicks(COOLDOWN_TICKS);
			pressToggle(context);
			check(clientActive(context), "mode should be on again for the movement check");
			hop(context);

			// The settings screen shows what the server allows.
			context.setScreen(SettingsScreen::new);
			context.waitTicks(2);
			context.takeScreenshot("bhop-settings-singleplayer");
			context.setScreen(() -> null);
		}
	}

	/** Holds forward, sprint and jump for a few seconds and logs the speed reached. */
	private static void hop(final ClientGameTestContext context) {
		final Vec3 start = context.computeOnClient(client -> client.player.position());
		context.getInput().holdKey(options -> options.keyUp);
		context.getInput().holdKey(options -> options.keySprint);
		context.getInput().holdKey(options -> options.keyJump);
		context.waitTicks(60);
		final double speed = context.computeOnClient(TestSupport::horizontalSpeed);
		context.getInput().releaseKey(options -> options.keyUp);
		context.getInput().releaseKey(options -> options.keySprint);
		context.getInput().releaseKey(options -> options.keyJump);

		final double moved = context.computeOnClient(client -> client.player.position()).subtract(start).horizontalDistance();
		BhopMod.LOGGER.info("Game test: hopped {} blocks, ending at {} b/s", moved, speed);
		check(speed > 1.0D, "hopping forward should move the player");
	}

	private static void runCommand(final TestSingleplayerContext world, final String command) {
		world.getServer().runCommand(command);
		world.getConnection().waitForClientboundPackets();
	}
}
