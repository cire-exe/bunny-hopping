package com.kishi.bhop.physics;

import java.util.List;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ToDoubleFunction;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Every value that shapes Source movement, and so has to be the same for everyone on a server.
 *
 * <p>In singleplayer these come from the player's own config. On a server running the mod they
 * come from the server, which sends them to each player on joining and again whenever an admin
 * changes one.
 *
 * <p>Source expresses speeds in units per second; this mod works in blocks per second using the
 * conventional 40 units = 1 block scale, then tunes the ground speed down so the mode blends with
 * vanilla distances. The acceleration constants themselves are dimensionless and are left at their
 * original values. Jump height and gravity stay on vanilla's numbers so a normal one-block
 * jump still clears a block.
 *
 * <p>Adding a value means a field here and one line in {@link #FIELDS}; repair, copying, the
 * network codec and the admin command all follow from that. Bump the network protocol version
 * whenever the list changes, since it changes the wire format.
 */
public final class PhysicsSettings {

	/** More sub-steps than this cost time without changing the feel. */
	public static final int MAX_AIR_SUBSTEPS = 16;

	/*
	 * Upper bounds for the decimal values. Generous enough for any sensible tuning, but they keep
	 * the movement maths finite: a server, LAN host or hand edit sending something like 1e300 could
	 * otherwise overflow the velocity to Infinity or NaN and get the player kicked for an invalid
	 * position.
	 */

	/** 5 blocks per tick, past which the vanilla server starts rejecting moves as too fast. */
	public static final double MAX_SPEED = 100.0D;

	/** For the Source constants; surf servers run {@code sv_airaccelerate} in the hundreds. */
	public static final double MAX_ACCELERATE = 1000.0D;

	/** For multipliers on vanilla values, where 1 means unchanged. */
	public static final double MAX_SCALE = 10.0D;

	/** {@code sv_accelerate}. Dimensionless. */
	public double groundAccelerate = 5.5D;

	/** {@code sv_airaccelerate}. Dimensionless. This is what makes air strafing gain speed. */
	public double airAccelerate = 12.0D;

	/** {@code sv_friction}. Dimensionless. */
	public double friction = 5.2D;

	/** {@code sv_stopspeed}, in blocks per second (80 u/s). */
	public double stopSpeed = 2.0D;

	/**
	 * Ground wish speed in blocks per second while sprinting, tuned to roughly vanilla's sprint
	 * speed. Walking, sneaking, using an item and Speed or Slowness scale it as in vanilla.
	 */
	public double maxGroundSpeed = 5.6D;

	/**
	 * The clamp Source applies to the wish speed while airborne, in blocks per second (30 u/s).
	 * Bunny hopping exists because of this clamp: it limits how much speed can be added along the
	 * wish direction, but not how much the velocity vector can be rotated.
	 */
	public double maxAirWishSpeed = 0.75D;

	/**
	 * Top horizontal speed, in blocks per second.
	 *
	 * <p>Air strafing has no natural ceiling - without a clamp the speed just keeps climbing for as
	 * long as you keep hopping. Raise it for an uncapped feel.
	 */
	public double maxTotalSpeed = 10.43D;

	/**
	 * Air acceleration sub-steps per tick, each facing where the mouse pointed at that moment.
	 *
	 * <p>Source gains a fixed amount of speed per step rather than per second, so this sets the
	 * strafe gain rate as much as the smoothness. 3 gives 60 steps a second, close to
	 * a 64-tick shooter server; 1 is Minecraft's 20 ticks, about three times slower.
	 */
	public int airSubsteps = 3;

	/**
	 * Boost air acceleration below {@link #rampUpSpeed}.
	 *
	 * <p>Lets the opening hops build speed quickly, then hands back to the authentic Source rate for
	 * the climb up to {@link #maxTotalSpeed}. Off by default: with {@link #airSubsteps} the
	 * authentic rate is already quick.
	 */
	public boolean rampUp = false;

	/** Speed below which the ramp-up boost applies, in blocks per second. */
	public double rampUpSpeed = 8.0D;

	/**
	 * How much the air wish speed cap is multiplied by below {@link #rampUpSpeed}.
	 *
	 * <p>It is deliberately the <em>cap</em> that is scaled and not {@link #airAccelerate}. With
	 * Source's numbers the per-step gain is {@code min(airAccelerate * wishSpeed * dt, cap)}, and
	 * the first term is already larger than the cap, so raising the acceleration alone changes
	 * nothing at all - the cap is what actually binds. 1 means no boost.
	 */
	public double rampUpMultiplier = 2.0D;

	/**
	 * Forward impulse added on a sprinting jump, in blocks per second.
	 *
	 * <p>Vanilla adds 0.2 blocks per tick, which is 4.0 here. Set to 0 to remove sprint jumping
	 * entirely.
	 */
	public double sprintJumpBoost = 4.0D;

	/**
	 * Ceiling the sprint jump boost may raise you to, in blocks per second.
	 *
	 * <p>Vanilla sprint jumping settles at about this speed. Holding forward and jumping will climb
	 * to here and stop; going faster has to be earned by air strafing. The ceiling never reduces a
	 * speed you already had, so arriving fast off a good strafe is not punished.
	 */
	public double sprintJumpSpeed = 7.13D;

	/** Multiplies vanilla's jump height, Jump Boost and honey blocks included. 1 is vanilla. */
	public double jumpScale = 1.0D;

	/** Multiplies vanilla's gravity, Slow Falling included. 1 is vanilla. */
	public double gravityScale = 1.0D;

	/** Every value above, in wire order. */
	public static final List<PhysicsSetting> FIELDS = List.of(
			decimal("groundAccelerate", MAX_ACCELERATE, s -> s.groundAccelerate, (s, v) -> s.groundAccelerate = v),
			decimal("airAccelerate", MAX_ACCELERATE, s -> s.airAccelerate, (s, v) -> s.airAccelerate = v),
			decimal("friction", MAX_ACCELERATE, s -> s.friction, (s, v) -> s.friction = v),
			decimal("stopSpeed", MAX_SPEED, s -> s.stopSpeed, (s, v) -> s.stopSpeed = v),
			decimal("maxGroundSpeed", MAX_SPEED, s -> s.maxGroundSpeed, (s, v) -> s.maxGroundSpeed = v),
			decimal("maxAirWishSpeed", MAX_SPEED, s -> s.maxAirWishSpeed, (s, v) -> s.maxAirWishSpeed = v),
			decimal("maxTotalSpeed", MAX_SPEED, s -> s.maxTotalSpeed, (s, v) -> s.maxTotalSpeed = v),
			new PhysicsSetting.Whole("airSubsteps", 1, MAX_AIR_SUBSTEPS, s -> s.airSubsteps, (s, v) -> s.airSubsteps = v),
			new PhysicsSetting.Flag("rampUp", s -> s.rampUp, (s, v) -> s.rampUp = v),
			decimal("rampUpSpeed", MAX_SPEED, s -> s.rampUpSpeed, (s, v) -> s.rampUpSpeed = v),
			decimal("rampUpMultiplier", MAX_SCALE, s -> s.rampUpMultiplier, (s, v) -> s.rampUpMultiplier = v),
			decimal("sprintJumpBoost", MAX_SPEED, s -> s.sprintJumpBoost, (s, v) -> s.sprintJumpBoost = v),
			decimal("sprintJumpSpeed", MAX_SPEED, s -> s.sprintJumpSpeed, (s, v) -> s.sprintJumpSpeed = v),
			decimal("jumpScale", MAX_SCALE, s -> s.jumpScale, (s, v) -> s.jumpScale = v),
			decimal("gravityScale", MAX_SCALE, s -> s.gravityScale, (s, v) -> s.gravityScale = v));

	/**
	 * Writes every value in {@link #FIELDS} order. Decoded settings are repaired before use, so a
	 * misbehaving server can never hand a client a value the movement code cannot cope with.
	 */
	public static final StreamCodec<FriendlyByteBuf, PhysicsSettings> STREAM_CODEC = StreamCodec.of(
			(buf, settings) -> FIELDS.forEach(field -> field.write(buf, settings)),
			buf -> {
				final PhysicsSettings settings = new PhysicsSettings();
				FIELDS.forEach(field -> field.read(buf, settings));
				settings.sanitize();
				return settings;
			});

	/** The shipped values, which repairs fall back to. Only ever read. */
	private static final PhysicsSettings DEFAULTS = new PhysicsSettings();

	/** Repairs anything a hand edit, or a remote server, could have broken. */
	public void sanitize() {
		FIELDS.forEach(field -> field.sanitize(this, DEFAULTS));
	}

	public PhysicsSettings copy() {
		final PhysicsSettings copy = new PhysicsSettings();
		FIELDS.forEach(field -> field.copy(this, copy));
		return copy;
	}

	/** Every decimal value so far is a non-negative speed or factor. */
	private static PhysicsSetting decimal(
			final String name,
			final double max,
			final ToDoubleFunction<PhysicsSettings> getter,
			final ObjDoubleConsumer<PhysicsSettings> setter) {
		return new PhysicsSetting.Decimal(name, 0.0D, max, getter, setter);
	}
}
