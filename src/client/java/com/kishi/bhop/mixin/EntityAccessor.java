package com.kishi.bhop.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes how blocks are slowing an entity, so Source movement can leave those cases to vanilla. */
@Mixin(Entity.class)
public interface EntityAccessor {

	/** Non-zero while caught in a cobweb, berry bush or powder snow; applied and cleared by the next move. */
	@Accessor("stuckSpeedMultiplier")
	Vec3 bhop$getStuckSpeedMultiplier();

	/** Below 1 on soul sand and honey: the share of velocity the block lets the entity keep each tick. */
	@Invoker("getBlockSpeedFactor")
	float bhop$getBlockSpeedFactor();
}
