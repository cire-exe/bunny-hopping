package com.kishi.bhop.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.kishi.bhop.network.BhopStatePayload;
import com.kishi.bhop.network.BhopStatePayload.Refusal;
import com.kishi.bhop.network.ServerSettingsPayload;
import com.kishi.bhop.network.SetBhopStatePayload;
import com.kishi.bhop.physics.PhysicsSettings;
import com.kishi.bhop.rules.BhopPolicy;
import com.kishi.bhop.rules.PlayerRules;
import com.kishi.bhop.rules.ServerRules;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * The server's half of the mod: hands out physics and rules, and checks every toggle against them.
 *
 * <p>Built to cost nothing measurable. There is no tick handler and no movement hook; the server
 * only does work when a player joins, leaves, toggles the mode, or an admin changes a setting. It
 * keeps one small entry per player running the mod, and none at all for players without it.
 *
 * <p>All of this runs on the server thread, which is where Fabric delivers both the connection
 * events and the payloads, so the player map needs no locking.
 */
public final class BhopServer {

	/** The fewest ticks allowed between two toggles, even with the cooldown set to zero. */
	private static final int MIN_TOGGLE_GAP_TICKS = 2;

	/**
	 * How much earlier than the cooldown a toggle may arrive. The client times the cooldown itself,
	 * and network jitter can bunch two of its messages closer together than they were sent.
	 */
	private static final int LATENCY_SLACK_TICKS = 2;

	private static final Map<UUID, PlayerState> PLAYERS = new HashMap<>();

	/**
	 * The host's physics, when this is a world opened to LAN rather than a dedicated server.
	 * {@code null} means the physics from {@link ServerConfig} apply.
	 */
	private static volatile PhysicsSettings hostPhysics;

	/** What the server remembers about one player running the mod. */
	private static final class PlayerState {
		boolean enabled;
		long lastToggleTick = Long.MIN_VALUE / 2;
		long lastRefusalTick = Long.MIN_VALUE / 2;
	}

	private BhopServer() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerConfig.load());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PLAYERS.clear();
			hostPhysics = null;
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PLAYERS.remove(handler.player.getUUID()));
		ServerPlayNetworking.registerGlobalReceiver(
				SetBhopStatePayload.TYPE, (payload, context) -> onSetState(context.server(), context.player(), payload.enabled()));
	}

	/**
	 * Replaces the physics of a world opened to LAN with the host's current ones, and sends them to
	 * everyone. Safe to call from the client thread: the work is handed to the server thread.
	 *
	 * @param physics a copy the caller will not touch again
	 */
	public static void publishHostPhysics(final MinecraftServer server, final PhysicsSettings physics) {
		server.execute(() -> {
			hostPhysics = physics;
			broadcast(server);
		});
	}

	/** Whether the physics come from the host's settings screen rather than the config file. */
	public static boolean usesHostPhysics() {
		return hostPhysics != null;
	}

	/**
	 * Sends everyone running the mod the current physics and their rules.
	 *
	 * <p>Called after any change to either. Each player's stored state is resolved under the new
	 * rules the same way their client resolves it, so the two stay in agreement.
	 */
	public static void broadcast(final MinecraftServer server) {
		for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
			final PlayerState state = PLAYERS.get(player.getUUID());
			if (state != null) {
				sendSettings(player, state);
			}
		}
	}

	/** Whether the player has the mode on, or {@code null} if they are not running the mod. */
	public static Boolean stateOf(final ServerPlayer player) {
		final PlayerState state = PLAYERS.get(player.getUUID());
		return state != null ? state.enabled : null;
	}

	private static void onJoin(final ServerPlayer player) {
		// Without a receiver for this exact channel the player has no mod, or another version of it.
		if (!ServerPlayNetworking.canSend(player, ServerSettingsPayload.TYPE)) {
			return;
		}

		final PlayerState state = new PlayerState();
		PLAYERS.put(player.getUUID(), state);
		sendSettings(player, state);
	}

	private static void sendSettings(final ServerPlayer player, final PlayerState state) {
		final PlayerRules rules = rulesFor(player);
		state.enabled = rules.resolve(state.enabled);
		ServerPlayNetworking.send(player, new ServerSettingsPayload(physics(), rules));
	}

	private static void onSetState(final MinecraftServer server, final ServerPlayer player, final boolean requested) {
		final PlayerState state = PLAYERS.get(player.getUUID());
		if (state == null || state.enabled == requested) {
			return;
		}

		final long now = server.getTickCount();
		final PlayerRules rules = rulesFor(player);
		final Refusal refusal = refusal(rules, state, now);
		if (refusal == null) {
			state.enabled = requested;
			state.lastToggleTick = now;
			return;
		}

		// At most one correction per tick, so a client spamming toggles cannot make the server spam
		// replies. The latest one is all the client needs anyway.
		if (state.lastRefusalTick != now) {
			state.lastRefusalTick = now;
			ServerPlayNetworking.send(player, new BhopStatePayload(state.enabled, refusal));
		}
	}

	/** Why a toggle has to be turned down, or {@code null} if it is fine. */
	private static Refusal refusal(final PlayerRules rules, final PlayerState state, final long now) {
		if (!rules.allowed()) {
			return Refusal.NOT_ALLOWED;
		}

		if (rules.policy() == BhopPolicy.FORCED_ON) {
			return Refusal.FORCED_ON;
		}

		if (rules.policy() == BhopPolicy.DISABLED) {
			return Refusal.DISABLED;
		}

		final long minGap = Math.max(rules.toggleCooldownTicks() - LATENCY_SLACK_TICKS, MIN_TOGGLE_GAP_TICKS);
		return now - state.lastToggleTick < minGap ? Refusal.COOLDOWN : null;
	}

	private static PlayerRules rulesFor(final ServerPlayer player) {
		final ServerRules rules = ServerConfig.get().rules;
		final PermissionLevel level = PermissionLevel.byId(rules.requiredPermissionLevel);
		return rules.forPlayer(player.permissions().hasPermission(new Permission.HasCommandLevel(level)));
	}

	private static PhysicsSettings physics() {
		final PhysicsSettings host = hostPhysics;
		return host != null ? host : ServerConfig.get().physics;
	}
}
