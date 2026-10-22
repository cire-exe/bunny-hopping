package com.kishi.bhop.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;

/** Checks how vanilla's travel input becomes a direction and a wish speed scale. */
class MoveInputTest {

	private static final double DELTA = 1.0E-3D;

	@Test
	void noInputIsEmpty() {
		final MoveInput input = MoveInput.of(Vec3.ZERO);

		assertSame(MoveInput.NONE, input);
		assertTrue(input.isEmpty());
	}

	@Test
	void plainForwardIsFullSpeed() {
		final MoveInput input = MoveInput.of(new Vec3(0.0D, 0.0D, 0.98D));

		assertEquals(0.0D, input.left(), DELTA);
		assertEquals(1.0D, input.forward(), DELTA);
		assertEquals(1.0D, input.scale(), DELTA);
	}

	@Test
	void diagonalIsAUnitDirectionAtFullSpeed() {
		final double component = 0.98D / Math.sqrt(2.0D);
		final MoveInput input = MoveInput.of(new Vec3(component, 0.0D, component));

		assertEquals(Math.sqrt(0.5D), input.left(), DELTA);
		assertEquals(Math.sqrt(0.5D), input.forward(), DELTA);
		assertEquals(1.0D, input.scale(), DELTA);
	}

	@Test
	void slowdownsBecomeTheScale() {
		// Sneaking multiplies the input by 0.3 on top of vanilla's 0.98 trim.
		assertEquals(0.3D, MoveInput.of(new Vec3(0.0D, 0.0D, 0.98D * 0.3D)).scale(), DELTA);
	}

	@Test
	void scaleNeverExceedsOne() {
		assertEquals(1.0D, MoveInput.of(new Vec3(0.0D, 0.0D, 1.5D)).scale(), DELTA);
	}

	@Test
	void directionMatchesVanillaYaw() {
		final MoveInput forward = MoveInput.of(new Vec3(0.0D, 0.0D, 0.98D));
		final MoveInput left = MoveInput.of(new Vec3(0.98D, 0.0D, 0.0D));

		// Yaw 0 faces +Z and yaw 90 faces -X.
		assertVec(new Vec3(0.0D, 0.0D, 1.0D), forward.direction(0.0F));
		assertVec(new Vec3(-1.0D, 0.0D, 0.0D), forward.direction(90.0F));
		assertVec(new Vec3(1.0D, 0.0D, 0.0D), left.direction(0.0F));
	}

	private static void assertVec(final Vec3 expected, final Vec3 actual) {
		assertEquals(expected.x, actual.x, DELTA, "x");
		assertEquals(expected.y, actual.y, DELTA, "y");
		assertEquals(expected.z, actual.z, DELTA, "z");
	}
}
