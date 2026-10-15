package com.kishi.bhop.server;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;

import com.kishi.bhop.physics.PhysicsSetting;
import com.kishi.bhop.physics.PhysicsSettings;
import com.kishi.bhop.rules.BhopPolicy;
import com.kishi.bhop.rules.ServerRules;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /bhop}: lets operators change the server's rules and physics while it runs.
 *
 * <p>Every change is saved to {@code config/bhop-server.json} and sent to everyone straight away,
 * so nobody has to rejoin. Replies use translation keys with English fallbacks, because a dedicated
 * server has no language files of its own.
 */
public final class BhopCommands {

	private static final String VALUE = "value";

	private BhopCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
				Commands.literal("bhop")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("reload").executes(BhopCommands::reload))
						.then(Commands.literal("status").executes(BhopCommands::status))
						.then(policy())
						.then(rules())
						.then(physics())));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> policy() {
		final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("policy");
		for (final BhopPolicy policy : BhopPolicy.values()) {
			node.then(Commands.literal(policy.name().toLowerCase(Locale.ROOT)).executes(ctx -> {
				ServerConfig.get().rules.policy = policy;
				return applied(ctx, "policy", policy.name().toLowerCase(Locale.ROOT));
			}));
		}

		return node;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> rules() {
		return Commands.literal("rule")
				.then(boolRule("allowAutoHop", (rules, value) -> rules.allowAutoHop = value))
				.then(intRule("maxJumpBuffer", ServerRules.MAX_JUMP_BUFFER_TICKS, (rules, value) -> rules.maxJumpBufferTicks = value))
				.then(intRule("toggleCooldown", ServerRules.MAX_TOGGLE_COOLDOWN_TICKS, (rules, value) -> rules.toggleCooldownTicks = value))
				.then(intRule("permissionLevel", ServerRules.MAX_PERMISSION_LEVEL, (rules, value) -> rules.requiredPermissionLevel = value));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> boolRule(
			final String name, final BiConsumer<ServerRules, Boolean> setter) {
		return Commands.literal(name).then(Commands.argument(VALUE, BoolArgumentType.bool()).executes(ctx -> {
			final boolean value = BoolArgumentType.getBool(ctx, VALUE);
			setter.accept(ServerConfig.get().rules, value);
			return applied(ctx, name, Boolean.toString(value));
		}));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> intRule(
			final String name, final int max, final ObjIntConsumer<ServerRules> setter) {
		return Commands.literal(name).then(Commands.argument(VALUE, IntegerArgumentType.integer(0, max)).executes(ctx -> {
			final int value = IntegerArgumentType.getInteger(ctx, VALUE);
			setter.accept(ServerConfig.get().rules, value);
			return applied(ctx, name, Integer.toString(value));
		}));
	}

	/** One sub-command per entry in {@link PhysicsSettings#FIELDS}: no value shows it, a value sets it. */
	private static LiteralArgumentBuilder<CommandSourceStack> physics() {
		final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("physics")
				.then(Commands.literal("reset").executes(ctx -> {
					if (!physicsEditable(ctx)) {
						return 0;
					}

					ServerConfig.get().physics = new PhysicsSettings();
					return applied(ctx, "physics", "defaults");
				}));

		for (final PhysicsSetting setting : PhysicsSettings.FIELDS) {
			node.then(Commands.literal(setting.name())
					.executes(ctx -> show(ctx, setting))
					.then(Commands.argument(VALUE, argumentFor(setting)).executes(ctx -> {
						if (!physicsEditable(ctx)) {
							return 0;
						}

						final PhysicsSettings physics = ServerConfig.get().physics;
						switch (setting) {
							case PhysicsSetting.Decimal decimal -> decimal.set(physics, DoubleArgumentType.getDouble(ctx, VALUE));
							case PhysicsSetting.Whole whole -> whole.set(physics, IntegerArgumentType.getInteger(ctx, VALUE));
							case PhysicsSetting.Flag flag -> flag.set(physics, BoolArgumentType.getBool(ctx, VALUE));
						}

						return applied(ctx, setting.name(), setting.describe(physics));
					})));
		}

		return node;
	}

	private static ArgumentType<?> argumentFor(final PhysicsSetting setting) {
		return switch (setting) {
			case PhysicsSetting.Decimal decimal -> DoubleArgumentType.doubleArg(decimal.min(), decimal.max());
			case PhysicsSetting.Whole whole -> IntegerArgumentType.integer(whole.min(), whole.max());
			case PhysicsSetting.Flag flag -> BoolArgumentType.bool();
		};
	}

	private static int show(final CommandContext<CommandSourceStack> ctx, final PhysicsSetting setting) {
		final String value = setting.describe(ServerConfig.get().physics);
		ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(
				"bhop.command.value", "%s is %s", setting.name(), value), false);
		return 1;
	}

	/** A world opened to LAN takes its physics from the host's settings screen, not from here. */
	private static boolean physicsEditable(final CommandContext<CommandSourceStack> ctx) {
		if (!BhopServer.usesHostPhysics()) {
			return true;
		}

		ctx.getSource().sendFailure(Component.translatableWithFallback(
				"bhop.command.host_physics", "This world's physics come from the host's BHOP settings screen"));
		return false;
	}

	private static int reload(final CommandContext<CommandSourceStack> ctx) {
		ServerConfig.load();
		BhopServer.broadcast(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(
				"bhop.command.reloaded", "Reloaded the BHOP server config"), true);
		return 1;
	}

	private static int status(final CommandContext<CommandSourceStack> ctx) {
		final CommandSourceStack source = ctx.getSource();
		final ServerRules rules = ServerConfig.get().rules;
		source.sendSuccess(() -> Component.translatableWithFallback(
				"bhop.command.status", "BHOP policy: %s, auto hop: %s, max jump buffer: %s, cooldown: %s ticks, permission level: %s",
				rules.policy.name().toLowerCase(Locale.ROOT), rules.allowAutoHop, rules.maxJumpBufferTicks,
				rules.toggleCooldownTicks, rules.requiredPermissionLevel), false);

		for (final ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
			final Boolean state = BhopServer.stateOf(player);
			final Component line = state == null
					? Component.translatableWithFallback("bhop.command.status.no_mod", "%s: no mod", player.getName())
					: Component.translatableWithFallback(
							state ? "bhop.command.status.on" : "bhop.command.status.off",
							state ? "%s: bhop on" : "%s: bhop off",
							player.getName());
			source.sendSuccess(() -> line, false);
		}

		return 1;
	}

	/** Repairs, saves and sends the changed config, then confirms the change to every operator. */
	private static int applied(final CommandContext<CommandSourceStack> ctx, final String name, final String value) {
		final ServerConfig config = ServerConfig.get();
		config.rules.sanitize();
		config.physics.sanitize();
		ServerConfig.save();
		BhopServer.broadcast(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(
				"bhop.command.set", "Set BHOP %s to %s", name, value), true);
		return 1;
	}
}
