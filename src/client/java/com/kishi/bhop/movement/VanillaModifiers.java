package com.kishi.bhop.movement;

import com.kishi.bhop.mixin.EntityAccessor;
import com.kishi.bhop.mixin.LivingEntityAccessor;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The parts of vanilla's movement that Source movement keeps: status effects, attributes and the
 * block underfoot.
 *
 * <p>{@link SourceMovementController} replaces vanilla's travel step outright, so anything vanilla
 * folds into that step has to be put back here or bunny hop mode silently ignores it. Each method
 * reads the value from vanilla itself wherever possible, so other mods that change the same
 * attributes or effects keep working too.
 */
final class VanillaModifiers {

	/**
	 * Vanilla's movement speed attribute while sprinting with no effects: 0.1 base, +30% sprinting.
	 *
	 * <p>{@code maxGroundSpeed} is tuned to that state, so it is the reference the attribute is
	 * divided by.
	 */
	private static final double SPRINT_MOVEMENT_SPEED = 0.13D;

	/** Friction of an ordinary block. Anything above it, like ice or slime, is slippery. */
	private static final float DEFAULT_BLOCK_FRICTION = 0.6F;

	/** Vanilla's per-tick vertical drag. */
	private static final float VERTICAL_DRAG = 0.98F;

	/** Levitation's rise rate per amplifier level, in blocks per tick, and how fast it is reached. */
	private static final double LEVITATION_SPEED = 0.05D;
	private static final double LEVITATION_RESPONSE = 0.2D;

	/** How fast the player sinks over a chunk that has not loaded yet, as vanilla does. */
	private static final double UNLOADED_CHUNK_FALL = -0.1D;

	/** Vanilla's own threshold for a stuck multiplier being set. */
	private static final double STUCK_EPSILON = 1.0E-7D;

	private VanillaModifiers() {
	}

	/**
	 * Scale on the ground wish speed from the movement speed attribute.
	 *
	 * <p>{@code 1.0} while sprinting with no effects, about {@code 0.77} walking. Speed and Slowness
	 * raise and lower it, as do armour trims, attributes set by commands and other mods.
	 */
	static double speedFactor(final LocalPlayer player) {
		return player.getAttributeValue(Attributes.MOVEMENT_SPEED) / SPRINT_MOVEMENT_SPEED;
	}

	/**
	 * Source's {@code m_surfaceFriction} for the block underfoot.
	 *
	 * <p>{@code 1.0} on ordinary blocks and lower on slippery ones: about {@code 0.23} on ice and
	 * {@code 0.42} on slime. It scales both ground friction and ground acceleration, as in Source,
	 * which is also the ratio vanilla scales its own ground acceleration by.
	 */
	static double surfaceFriction(final LocalPlayer player, final BlockState below) {
		final float blockFriction = modifiedFriction(
				below.getBlock().getFriction(),
				player.getAttributeValue(Attributes.FRICTION_MODIFIER));
		if (blockFriction <= DEFAULT_BLOCK_FRICTION) {
			return 1.0D;
		}

		final double ratio = DEFAULT_BLOCK_FRICTION / blockFriction;
		return ratio * ratio * ratio;
	}

	/**
	 * Whether a block is rewriting the player's velocity every tick, so vanilla should move them.
	 *
	 * <p>Cobwebs, berry bushes and powder snow hold the player in place, and soul sand, honey and
	 * slime underfoot cut their speed on every tick. Those slowdowns are tuned against vanilla's
	 * ground acceleration, which at walking pace is far stronger per tick than Source's; under
	 * Source movement the same blocks would be two to three times slower than in vanilla. They are
	 * left to vanilla, as water and ladders are. Ice and other slippery blocks are not slowdowns and
	 * stay with Source movement, through {@link #surfaceFriction}.
	 */
	static boolean slowedByBlock(final LocalPlayer player, final BlockState below) {
		final EntityAccessor entity = (EntityAccessor) player;
		if (entity.bhop$getStuckSpeedMultiplier().lengthSqr() > STUCK_EPSILON) {
			return true;
		}

		return player.onGround() && (entity.bhop$getBlockSpeedFactor() < 1.0F || below.getBlock() instanceof SlimeBlock);
	}

	/**
	 * The block whose friction and slowdown apply to the player, read once per tick and handed to
	 * {@link #slowedByBlock} and {@link #surfaceFriction}.
	 */
	static BlockState blockBelow(final LocalPlayer player) {
		return player.level().getBlockState(player.getBlockPosBelowThatAffectsMyMovement());
	}

	/** Vanilla's jump power in blocks per tick: Jump Boost, honey blocks and the jump attribute. */
	static double jumpPower(final LocalPlayer player) {
		return ((LivingEntityAccessor) player).bhop$getJumpPower();
	}

	/**
	 * Vertical velocity for the next tick, in blocks per tick: gravity, Slow Falling, Levitation
	 * and drag, the way vanilla's {@code travelInAir} works them out.
	 *
	 * <p>Call it after the move, so that Slow Falling sees whether the player is falling now.
	 *
	 * @param gravityScale multiplies vanilla's gravity; 1 is vanilla
	 */
	@SuppressWarnings("deprecation") // hasChunkAt: the same loaded-chunk check vanilla's travelInAir makes
	static double nextVerticalVelocity(final LocalPlayer player, final double velocityY, final double gravityScale) {
		final Level level = player.level();
		final MobEffectInstance levitation = player.getEffect(MobEffects.LEVITATION);

		final double next;
		if (levitation != null) {
			final double target = LEVITATION_SPEED * (levitation.getAmplifier() + 1);
			next = velocityY + (target - velocityY) * LEVITATION_RESPONSE;
		} else if (level.hasChunkAt(player.getBlockPosBelowThatAffectsMyMovement())) {
			next = velocityY - ((LivingEntityAccessor) player).bhop$getEffectiveGravity() * gravityScale;
		} else {
			// Hold the player above unloaded terrain rather than dropping them through it.
			next = player.getY() > level.getMinY() ? UNLOADED_CHUNK_FALL : 0.0D;
		}

		return next * modifiedFriction(VERTICAL_DRAG, player.getAttributeValue(Attributes.AIR_DRAG_MODIFIER));
	}

	/** Vanilla's {@code LivingEntity#computeModifiedFriction}, which is private. */
	private static float modifiedFriction(final float friction, final double modifier) {
		return Math.clamp(1.0F - (1.0F - friction) * (float) modifier, 0.0F, 1.0F);
	}
}
