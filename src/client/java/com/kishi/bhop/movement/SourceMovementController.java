package com.kishi.bhop.movement;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.physics.PhysicsSettings;
import com.kishi.bhop.session.ServerSession;

import net.minecraft.SharedConstants;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Drives the player with Source engine movement in place of vanilla's.
 *
 * <p>Invoked from a mixin on {@code LivingEntity#travel}. When {@link #handleTravel} returns
 * {@code true} the vanilla implementation is cancelled and this class has already moved the player
 * for that tick. A second mixin cancels {@code LivingEntity#jumpFromGround} whenever
 * {@link #isHandling} is true, because vanilla jumps earlier in {@code aiStep} than it travels and
 * would otherwise apply its own impulse - including the sprint jump boost - on top of this one.
 *
 * <p>The tick order mirrors Source's {@code FullWalkMove}: friction is skipped on any tick the
 * player leaves the ground, which is what makes bunny hopping preserve - and then build - speed.
 * Ground acceleration still runs on that tick, so a player who never strafes can still reach a
 * normal running speed rather than being stranded at the air cap.
 *
 * <p>Air acceleration runs in several sub-steps per tick, each facing where the mouse pointed at
 * that moment; see {@link ViewSampler}. Everything vanilla folds into its own travel step - status
 * effects, attributes, sneaking, slippery blocks - comes from {@link VanillaModifiers}, so the mode
 * changes how you move without switching any of that off.
 *
 * <h2>Units</h2>
 * Minecraft stores velocity in blocks per <em>tick</em>, while Source's constants are all per
 * <em>second</em>. The horizontal components are converted on the way in and out so the config can
 * be read in blocks per second, which is the only scale the acceleration numbers make sense on.
 * The vertical component deliberately stays on vanilla's per-tick scale, so jump height and gravity
 * are vanilla's own and a one-block jump still clears a block.
 */
public final class SourceMovementController {

	private static final double TICKS_PER_SECOND = SharedConstants.TICKS_PER_SECOND;

	/** One Minecraft tick, in seconds. */
	private static final double TICK_SECONDS = 1.0D / TICKS_PER_SECOND;

	/** Below this a jump has no height at all, as vanilla treats it. */
	private static final double MIN_JUMP_POWER = 1.0E-5D;

	private SourceMovementController() {
	}

	/**
	 * Whether this class is taking over movement for the given player right now.
	 *
	 * <p>Shared with the jump mixin so both hooks agree on exactly when vanilla is suppressed.
	 */
	public static boolean isHandling(final LocalPlayer player) {
		return BhopMode.isActive() && isEligible(player);
	}

	/**
	 * Replaces vanilla movement for this tick.
	 *
	 * @param travelInput vanilla's movement input for the tick: x sideways, z forward
	 * @return {@code true} if this class handled movement and vanilla should be cancelled
	 */
	public static boolean handleTravel(final LocalPlayer player, final Vec3 travelInput) {
		if (!isHandling(player)) {
			JumpInput.reset();
			ViewSampler.reset();
			HopTracker.reset();
			return false;
		}

		final PhysicsSettings physics = ServerSession.physics();
		final float[] yaws = ViewSampler.consume(player.getYRot(), physics.airSubsteps);
		final MoveInput input = MoveInput.of(travelInput);
		final double speedFactor = VanillaModifiers.speedFactor(player);

		// Horizontal in blocks/second for the Source maths, vertical left per tick.
		Vec3 velocity = toPerSecond(player.getDeltaMovement());
		final boolean onGround = player.onGround();

		final double jumpPower = VanillaModifiers.jumpPower(player) * physics.jumpScale;
		final boolean jumping = JumpInput.poll(player, ConfigManager.get().movement) && onGround && jumpPower > MIN_JUMP_POWER;
		if (jumping) {
			JumpInput.consume();
		}

		final double wishSpeed = input.isEmpty() ? 0.0D : physics.maxGroundSpeed * speedFactor * input.scale();

		if (onGround) {
			final double surface = VanillaModifiers.surfaceFriction(player);

			// 1. Friction, but never on a tick the player leaves the ground. Skipping it is the whole
			// trick: land and jump on the same tick and none of the speed is scrubbed off.
			if (!jumping) {
				velocity = SourcePhysics.applyFriction(velocity, physics.friction * surface, stopSpeed(physics, input), TICK_SECONDS);
			}

			// 2. Acceleration towards the wish direction.
			if (wishSpeed > 0.0D) {
				velocity = SourcePhysics.accelerate(
						velocity, input.direction(player.getYRot()), wishSpeed, physics.groundAccelerate * surface, TICK_SECONDS);
			}
		} else {
			HopTracker.recordAirTurn(yaws, input.left());
			if (wishSpeed > 0.0D) {
				velocity = airStrafe(velocity, input, yaws, wishSpeed, physics);
			}
		}

		// 3. Jump, after acceleration so the landing tick still contributes ground speed.
		if (jumping) {
			velocity = new Vec3(velocity.x, Math.max(jumpPower, velocity.y), velocity.z);
			velocity = applySprintJumpBoost(player, physics, velocity, speedFactor);
		}

		velocity = SourcePhysics.clampHorizontalSpeed(velocity, physics.maxTotalSpeed);
		HopTracker.tick(onGround, jumping, SourcePhysics.horizontalSpeed(velocity));

		// 4. Move, then read the velocity back: Entity#move zeroes any axis that hit geometry, and
		// applies the block's speed factor, such as soul sand's.
		player.setDeltaMovement(toPerTick(velocity));
		player.move(MoverType.SELF, player.getDeltaMovement());
		final Vec3 resolved = player.getDeltaMovement();

		// 5. Gravity and vertical drag, on vanilla's per-tick scale. No horizontal drag: Source
		// friction already covers the ground case, and the absence of air drag is what preserves
		// speed between hops.
		player.setDeltaMovement(resolved.x, VanillaModifiers.nextVerticalVelocity(player, resolved.y, physics.gravityScale), resolved.z);

		return true;
	}

	/**
	 * Air acceleration across the tick's sub-steps, each towards where the view faced at the end of
	 * that sub-step.
	 */
	private static Vec3 airStrafe(
			Vec3 velocity, final MoveInput input, final float[] yaws, final double wishSpeed, final PhysicsSettings physics) {
		final int steps = yaws.length - 1;
		final double dt = TICK_SECONDS / steps;
		for (int k = 1; k <= steps; k++) {
			velocity = SourcePhysics.airAccelerate(
					velocity, input.direction(yaws[k]), wishSpeed, physics.airAccelerate, airWishSpeedCap(physics, velocity), dt);
		}

		return velocity;
	}

	/**
	 * Reproduces vanilla's sprint jump boost, but with a ceiling.
	 *
	 * <p>Vanilla adds a flat forward impulse on every sprinting jump. Left alone that stacks hop
	 * after hop and pins the player at the speed cap within a couple of jumps without any strafing
	 * at all. Clamping it means simply holding forward settles at roughly vanilla's sprint jump
	 * speed, and anything beyond that has to be earned by air strafing. Speed effects move the
	 * ceiling with the ground speed.
	 *
	 * <p>The ceiling never reduces an existing speed, so arriving fast from a good strafe is not
	 * punished by jumping again.
	 */
	private static Vec3 applySprintJumpBoost(
			final LocalPlayer player, final PhysicsSettings physics, final Vec3 velocity, final double speedFactor) {
		if (!player.isSprinting() || physics.sprintJumpBoost <= 0.0D) {
			return velocity;
		}

		final float yawRadians = player.getYRot() * Mth.DEG_TO_RAD;
		final double forwardX = -Mth.sin(yawRadians);
		final double forwardZ = Mth.cos(yawRadians);

		final double speedBefore = SourcePhysics.horizontalSpeed(velocity);
		final Vec3 boosted = velocity.add(forwardX * physics.sprintJumpBoost, 0.0D, forwardZ * physics.sprintJumpBoost);

		final double ceiling = Math.max(speedBefore, physics.sprintJumpSpeed * speedFactor);
		return SourcePhysics.clampHorizontalSpeed(boosted, ceiling);
	}

	/**
	 * Friction's stop speed floor, scaled down with slowed input such as sneaking or using an item.
	 *
	 * <p>Below the floor friction removes a flat amount per tick. At full stop speed that is more
	 * than ground acceleration can add towards a sneaking wish speed, so the player would crawl at a
	 * fraction of vanilla's sneak speed. Scaling it with the input keeps the floor in proportion to
	 * the wish speed. With no input the full floor applies, so stopping is unchanged.
	 */
	private static double stopSpeed(final PhysicsSettings physics, final MoveInput input) {
		return input.isEmpty() ? physics.stopSpeed : physics.stopSpeed * input.scale();
	}

	/**
	 * Air wish speed cap for this sub-step, boosted while still below the ramp-up speed.
	 *
	 * <p>Scaling the cap is the only thing that actually speeds up the climb. Source computes the
	 * per-step gain as the smaller of (airAccelerate * wishSpeed * dt) and the cap; with the
	 * shipped numbers the first term is above the cap, so the cap is the binding constraint and
	 * raising airAccelerate on its own would have no visible effect.
	 */
	private static double airWishSpeedCap(final PhysicsSettings physics, final Vec3 velocity) {
		if (!physics.rampUp || SourcePhysics.horizontalSpeed(velocity) >= physics.rampUpSpeed) {
			return physics.maxAirWishSpeed;
		}

		return physics.maxAirWishSpeed * physics.rampUpMultiplier;
	}

	/** Horizontal components from blocks per tick to blocks per second. */
	private static Vec3 toPerSecond(final Vec3 perTick) {
		return new Vec3(perTick.x * TICKS_PER_SECOND, perTick.y, perTick.z * TICKS_PER_SECOND);
	}

	/** Horizontal components from blocks per second back to blocks per tick. */
	private static Vec3 toPerTick(final Vec3 perSecond) {
		return new Vec3(perSecond.x / TICKS_PER_SECOND, perSecond.y, perSecond.z / TICKS_PER_SECOND);
	}

	/**
	 * Whether Source movement should apply at all.
	 *
	 * <p>Anything with its own specialised movement - swimming, lava, flight, elytra, ladders,
	 * vehicles, spectator - is left to vanilla so the mode never breaks normal play. So are blocks
	 * that slow the player every tick; see {@link VanillaModifiers#slowedByBlock}.
	 */
	private static boolean isEligible(final LocalPlayer player) {
		return !player.isSpectator()
				&& !player.getAbilities().flying
				&& !player.isFallFlying()
				&& !player.isPassenger()
				&& !player.isInWater()
				&& !player.isInLava()
				&& !player.isSwimming()
				&& !player.onClimbable()
				&& !player.noPhysics
				&& !VanillaModifiers.slowedByBlock(player);
	}
}
