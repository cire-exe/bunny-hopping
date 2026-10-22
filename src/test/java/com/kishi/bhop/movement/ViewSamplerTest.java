package com.kishi.bhop.movement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Checks the sub-step path {@link ViewSampler} builds from the frames recorded during a tick. */
class ViewSamplerTest {

	private static final float DELTA = 1.0E-4F;

	@BeforeEach
	void forgetPreviousTick() {
		ViewSampler.reset();
	}

	@Test
	void firstTickHoldsTheCurrentYaw() {
		assertArrayEquals(new float[] {45.0F, 45.0F, 45.0F, 45.0F}, ViewSampler.consume(45.0F, 3), DELTA);
	}

	@Test
	void pathStartsWhereThePreviousTickEnded() {
		ViewSampler.consume(0.0F, 3);

		// No frames at all: a straight turn from the last tick's yaw to this one.
		assertArrayEquals(new float[] {0.0F, 10.0F, 20.0F, 30.0F}, ViewSampler.consume(30.0F, 3), DELTA);
	}

	@Test
	void followsTheRecordedFrames() {
		ViewSampler.consume(0.0F, 3);
		ViewSampler.recordFrame(5.0F);
		ViewSampler.recordFrame(25.0F);
		ViewSampler.recordFrame(30.0F);

		// A sharp turn in the middle frame shows up in the middle sub-step, not spread evenly.
		assertArrayEquals(new float[] {0.0F, 5.0F, 25.0F, 30.0F}, ViewSampler.consume(30.0F, 3), DELTA);
	}

	@Test
	void interpolatesBetweenFramesWhenThereAreFewerThanSteps() {
		ViewSampler.consume(0.0F, 4);
		ViewSampler.recordFrame(40.0F);

		assertArrayEquals(new float[] {0.0F, 10.0F, 20.0F, 30.0F, 40.0F}, ViewSampler.consume(40.0F, 4), DELTA);
	}

	@Test
	void endsAtTheCurrentYawWhenSomethingTurnedThePlayerAfterTheLastFrame() {
		ViewSampler.consume(0.0F, 2);
		ViewSampler.recordFrame(10.0F);

		// Say a teleport set the yaw to 50 after the last frame was drawn.
		assertArrayEquals(new float[] {0.0F, 10.0F, 50.0F}, ViewSampler.consume(50.0F, 2), DELTA);
	}

	@Test
	void keepsTheNewestFrameWhenFull() {
		ViewSampler.consume(0.0F, 1);
		for (int frame = 1; frame <= 300; frame++) {
			ViewSampler.recordFrame(frame);
		}

		final float[] yaws = ViewSampler.consume(300.0F, 1);
		assertEquals(0.0F, yaws[0], DELTA);
		assertEquals(300.0F, yaws[1], DELTA);
	}

	@Test
	void consumingClearsTheFrames() {
		ViewSampler.consume(0.0F, 2);
		ViewSampler.recordFrame(90.0F);
		ViewSampler.consume(90.0F, 2);

		assertArrayEquals(new float[] {90.0F, 90.0F, 90.0F}, ViewSampler.consume(90.0F, 2), DELTA);
	}

	@Test
	void resetForgetsWhereTheLastTickEnded() {
		ViewSampler.consume(0.0F, 1);
		ViewSampler.reset();

		assertArrayEquals(new float[] {90.0F, 90.0F}, ViewSampler.consume(90.0F, 1), DELTA);
	}
}
