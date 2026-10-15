package com.kishi.bhop.gui;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** A slider bound to one numeric config value, snapped to a fixed step. */
public final class ConfigSlider extends AbstractSliderButton {

	private final Component caption;
	private final double min;
	private final double max;
	private final double step;
	private final String format;
	private final DoubleConsumer set;

	ConfigSlider(
			final int x,
			final int y,
			final int width,
			final int height,
			final Component caption,
			final double min,
			final double max,
			final double step,
			final String format,
			final DoubleSupplier get,
			final DoubleConsumer set) {
		super(x, y, width, height, Component.empty(), Mth.clamp((get.getAsDouble() - min) / (max - min), 0.0D, 1.0D));
		this.caption = caption;
		this.min = min;
		this.max = max;
		this.step = step;
		this.format = format;
		this.set = set;
		this.updateMessage();
	}

	/** The slider position as a config value, on the step grid. */
	private double current() {
		final double raw = this.min + this.value * (this.max - this.min);
		return Mth.clamp(Math.round(raw / this.step) * this.step, this.min, this.max);
	}

	@Override
	protected void updateMessage() {
		this.setMessage(Component.translatable(
				"options.generic_value", this.caption, Component.literal(this.format.formatted(this.current()))));
	}

	@Override
	protected void applyValue() {
		this.set.accept(this.current());
	}
}
