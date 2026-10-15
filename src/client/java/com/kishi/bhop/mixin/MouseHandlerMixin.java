package com.kishi.bhop.mixin;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.movement.JumpInput;
import com.kishi.bhop.movement.ViewSampler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The mouse's two jobs in bunny hop mode: recording the view every frame for sub-tick air strafing,
 * and turning the scroll wheel into a jump key.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	/** Runs once per frame, after the mouse has turned the player. */
	@Inject(method = "handleAccumulatedMovement", at = @At("TAIL"))
	private void bhop$recordView(final CallbackInfo ci) {
		if (this.minecraft.player != null) {
			ViewSampler.recordFrame(this.minecraft.player.getYRot());
		}
	}

	/**
	 * Takes the wheel for jumping when scroll jumping is on, as in shooters, instead of letting it change
	 * the hotbar slot. Menus, the spectator menu and the window's other scroll uses are untouched.
	 */
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void bhop$scrollJump(final long handle, final double xoffset, final double yoffset, final CallbackInfo ci) {
		if (yoffset == 0.0D
				|| handle != this.minecraft.getWindow().handle()
				|| this.minecraft.gui.screen() != null
				|| this.minecraft.gui.overlay() != null
				|| this.minecraft.player == null
				|| this.minecraft.player.isSpectator()
				|| !JumpInput.wantsScroll(ConfigManager.get().movement)) {
			return;
		}

		JumpInput.scroll();
		ci.cancel();
	}
}
