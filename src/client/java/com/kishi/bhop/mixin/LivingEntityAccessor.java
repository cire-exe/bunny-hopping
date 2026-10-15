package com.kishi.bhop.mixin;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the protected parts of vanilla's movement that Source movement reuses.
 *
 * <p>Calling vanilla rather than copying its formulas keeps every effect, attribute and block that
 * feeds into them working, including ones added by other mods.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

	/** Upward velocity of a jump, in blocks per tick: Jump Boost, honey blocks and the jump attribute. */
	@Invoker("getJumpPower")
	float bhop$getJumpPower();

	/** Gravity for this tick, in blocks per tick squared: Slow Falling and the gravity attribute. */
	@Invoker("getEffectiveGravity")
	double bhop$getEffectiveGravity();
}
