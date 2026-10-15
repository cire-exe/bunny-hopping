package com.kishi.bhop.gui;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Stacks a page's widgets top to bottom inside the content area.
 *
 * <p>The common rows - toggles, sliders, buttons - are one call each. Pages that need a custom row
 * place widgets themselves using {@link #x()}, {@link #y()} and {@link #width()}, then call
 * {@link #nextRow}.
 *
 * <p>By convention every setting has a lang key plus a {@code <key>.tooltip} entry explaining it.
 */
public final class PageBuilder {

	public static final int ROW_HEIGHT = 18;
	public static final int ROW_SPACING = 3;

	private final SettingsScreen screen;
	private final int x;
	private final int width;
	private int y;

	PageBuilder(final SettingsScreen screen, final int x, final int y, final int width) {
		this.screen = screen;
		this.x = x;
		this.y = y;
		this.width = width;
	}

	public SettingsScreen screen() {
		return this.screen;
	}

	public int x() {
		return this.x;
	}

	public int y() {
		return this.y;
	}

	public int width() {
		return this.width;
	}

	/** Moves down past a row of the given height. */
	public void nextRow(final int height) {
		this.nextRow(height, ROW_SPACING);
	}

	/** Moves down past a row, with custom spacing for dense layouts. */
	public void nextRow(final int height, final int spacing) {
		this.y += height + spacing;
	}

	/** Adds a widget placed by the caller. Does not advance the row. */
	public <T extends AbstractWidget> T add(final T widget) {
		return this.screen.addPageWidget(widget);
	}

	/** Adds text placed by the caller, re-read every frame so it can show a live value. */
	public void label(final int labelX, final int labelY, final Supplier<Component> text, final int colour) {
		this.screen.addPageLabel(labelX, labelY, text, colour);
	}

	public CycleButton<Boolean> toggle(final String key, final boolean value, final Consumer<Boolean> onChange) {
		final CycleButton<Boolean> button = CycleButton.onOffBuilder(value).create(
				this.x, this.y, this.width, ROW_HEIGHT, Component.translatable(key),
				(cycle, newValue) -> onChange.accept(newValue));
		return this.addRow(button, key);
	}

	/**
	 * A slider over a config value.
	 *
	 * @param format how the value is printed after the caption, e.g. {@code "%.2fx"}
	 */
	public ConfigSlider slider(
			final String key,
			final double min,
			final double max,
			final double step,
			final String format,
			final DoubleSupplier get,
			final DoubleConsumer set) {
		final ConfigSlider slider = new ConfigSlider(
				this.x, this.y, this.width, ROW_HEIGHT, Component.translatable(key), min, max, step, format, get, set);
		return this.addRow(slider, key);
	}

	public Button button(final String key, final Button.OnPress onPress) {
		final Button button = Button.builder(Component.translatable(key), onPress)
				.bounds(this.x, this.y, this.width, ROW_HEIGHT)
				.build();
		return this.addRow(button, key);
	}

	private <T extends AbstractWidget> T addRow(final T widget, final String key) {
		widget.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
		this.add(widget);
		this.nextRow(ROW_HEIGHT);
		return widget;
	}
}
