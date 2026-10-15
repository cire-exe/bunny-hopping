package com.kishi.bhop.session;

import com.kishi.bhop.BhopMod;
import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.config.ModConfig;
import com.kishi.bhop.network.BhopStatePayload;
import com.kishi.bhop.network.ServerSettingsPayload;
import com.kishi.bhop.network.SetBhopStatePayload;
import com.kishi.bhop.physics.PhysicsSettings;
import com.kishi.bhop.rules.BhopPolicy;
import com.kishi.bhop.rules.PlayerRules;
import com.kishi.bhop.rules.ServerRules;
import com.kishi.bhop.server.BhopServer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

/**
 * The client's view of the server it is connected to: which physics apply, and which rules.
 *
 * <p>Everything that needs to know whether the server runs the mod asks here. Movement reads its
 * physics from {@link #physics()} and the toggle checks {@link #rules()}, so neither has to care
 * whether they came from the player's own config or from a server.
 */
public final class ServerSession {

	/** What kind of server the client is connected to. */
	public enum Kind {
		/** Not connected to anything. */
		NONE,
		/** Singleplayer, or a world this client opened to LAN. The physics are the player's own. */
		LOCAL,
		/** A remote server running this version of the mod. The physics are the server's. */
		MODDED,
		/** A remote server running a version of the mod this one cannot talk to. */
		INCOMPATIBLE,
		/** A remote server without the mod. */
		UNMODDED
	}

	/** Used until the server's first settings arrive, which is a matter of milliseconds. */
	private static final PlayerRules PENDING =
			new PlayerRules(BhopPolicy.DISABLED, false, false, 0, 0);

	/**
	 * A server without the mod, or with a version this one cannot talk to. The mode is never
	 * available there: such a server cannot share its physics or set rules.
	 */
	private static final PlayerRules BLOCKED =
			new PlayerRules(BhopPolicy.DISABLED, false, false, ServerRules.MAX_JUMP_BUFFER_TICKS, 0);

	private static Kind kind = Kind.NONE;

	/** The server's physics and this player's rules, once received. */
	private static PhysicsSettings serverPhysics;
	private static PlayerRules serverRules;

	private ServerSession() {
	}

	public static void register() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ServerSession::reset));
		ClientPlayNetworking.registerGlobalReceiver(ServerSettingsPayload.TYPE, (payload, context) -> onSettings(payload));
		ClientPlayNetworking.registerGlobalReceiver(BhopStatePayload.TYPE, (payload, context) -> BhopToggle.onRefused(payload));

		// A world opened to LAN hands its guests the host's physics, starting with the ones it opens with.
		// Only this client's own world: a dedicated server in the same process, as in the game tests, keeps its config.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			if (server instanceof IntegratedServer) {
				BhopServer.publishHostPhysics(server, localPhysics().copy());
			}
		});
	}

	public static Kind kind() {
		return kind;
	}

	/** The physics the local player moves with right now. */
	public static PhysicsSettings physics() {
		return kind == Kind.MODDED && serverPhysics != null ? serverPhysics : localPhysics();
	}

	/** Whether the server decides the physics, so the player's own sliders do not apply. */
	public static boolean serverControlsPhysics() {
		return kind == Kind.MODDED;
	}

	/** The rules the local player is under right now. */
	public static PlayerRules rules() {
		return switch (kind) {
			case NONE -> PlayerRules.UNRESTRICTED;
			case LOCAL, MODDED -> serverRules != null ? serverRules : PENDING;
			case INCOMPATIBLE, UNMODDED -> BLOCKED;
		};
	}

	/** Whether the server keeps track of the mode's state, and so has to hear about every toggle. */
	static boolean serverTracksState() {
		return (kind == Kind.LOCAL || kind == Kind.MODDED) && ClientPlayNetworking.canSend(SetBhopStatePayload.TYPE);
	}

	/** Auto hop as the player set it, unless the server has switched it off. */
	public static boolean autoHop(final ModConfig.Movement movement) {
		return movement.autoHop && rules().allowAutoHop();
	}

	/** The player's jump buffer, capped by the server. */
	public static int jumpBufferTicks(final ModConfig.Movement movement) {
		return Math.min(movement.jumpBufferTicks, rules().maxJumpBufferTicks());
	}

	/** Why the player may not use the mode at all here, for when they try. */
	static Component notAllowedMessage() {
		return Component.translatable(switch (kind) {
			case UNMODDED -> "bhop.message.refused.unmodded";
			case INCOMPATIBLE -> "bhop.message.refused.incompatible";
			case LOCAL, MODDED -> serverRules == null ? "bhop.message.refused.pending" : "bhop.message.refused.not_allowed";
			case NONE -> "bhop.message.refused.not_allowed";
		});
	}

	/**
	 * Hands the player's current physics to the world they have opened to LAN, if any, so guests
	 * move with the same ones. Call after the physics change.
	 */
	public static void publishHostPhysics(final Minecraft client) {
		final IntegratedServer server = client.getSingleplayerServer();
		if (server != null) {
			BhopServer.publishHostPhysics(server, localPhysics().copy());
		}
	}

	private static void onJoin(final Minecraft client) {
		reset();
		if (client.isLocalServer()) {
			kind = Kind.LOCAL;
		} else if (ClientPlayNetworking.canSend(SetBhopStatePayload.TYPE)) {
			kind = Kind.MODDED;
		} else if (ClientPlayNetworking.getSendable().stream().anyMatch(id -> id.getNamespace().equals(BhopMod.MOD_ID))) {
			kind = Kind.INCOMPATIBLE;
		} else {
			kind = Kind.UNMODDED;
		}

		BhopMod.LOGGER.info("Connected to a server of kind {}", kind);
	}

	private static void onSettings(final ServerSettingsPayload payload) {
		// Only a server running this version of the mod gets a say; anything else stays blocked.
		if (kind != Kind.LOCAL && kind != Kind.MODDED) {
			return;
		}

		serverPhysics = payload.physics();
		serverRules = payload.rules();
		BhopToggle.onRulesChanged(payload.rules());
	}

	private static void reset() {
		kind = Kind.NONE;
		serverPhysics = null;
		serverRules = null;
		BhopToggle.reset();
	}

	private static PhysicsSettings localPhysics() {
		return ConfigManager.get().physics;
	}
}
