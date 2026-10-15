package com.kishi.bhop.mixin;

import com.kishi.bhop.movement.SourceMovementController;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses vanilla's jump while Source movement owns the player.
 *
 * <p>{@code aiStep} calls {@code jumpFromGround} before it calls {@code travel}, so without this
 * both implementations fire on the same tick. The duplicated upward impulse is harmless, but
 * vanilla's sprint jump boost is not: it adds a flat forward impulse on every sprinting jump,
 * which stacks hop after hop and pins the player at the speed cap within a couple of jumps with no
 * strafing involved at all.
 *
 * <p>{@link SourceMovementController} applies its own jump, including a clamped version of that
 * sprint boost.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void bhop$suppressVanillaJump(final CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && SourceMovementController.isHandling(player)) {
			ci.cancel();
		}
	}
}
