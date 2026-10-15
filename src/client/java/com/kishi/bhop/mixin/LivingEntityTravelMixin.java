package com.kishi.bhop.mixin;

import com.kishi.bhop.movement.SourceMovementController;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands movement over to the Source implementation while bunny hop mode is on.
 *
 * <p>Only the client's own player is affected, and only when
 * {@link SourceMovementController#handleTravel} accepts the tick - anything it declines (water,
 * flight, elytra, ladders, vehicles) falls through to vanilla untouched.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin {

	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void bhop$sourceMovement(final Vec3 input, final CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && SourceMovementController.handleTravel(player, input)) {
			ci.cancel();
		}
	}
}
