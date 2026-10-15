package com.kishi.bhop.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.kishi.bhop.config.ConfigManager;
import com.kishi.bhop.session.ServerSession;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The mod's settings window: a sidebar of sections on the left and the selected page beside it.
 *
 * <p>Deliberately leaves the rest of the screen clear - no blur, no darkening, no pause - so the
 * world stays visible and running while settings are changed. Changes apply as they are made and
 * are saved when the window closes.
 */
public final class SettingsScreen extends Screen {

	private static final int MARGIN = 8;
	private static final int PADDING = 6;
	private static final int SIDEBAR_WIDTH = 100;
	private static final int CONTENT_WIDTH = 180;
	private static final int GAP = 4;
	private static final int HEADER_HEIGHT = 22;
	private static final int SECTION_BUTTON_HEIGHT = 20;
	private static final int SECTION_BUTTON_SPACING = 2;

	private static final int PANEL = 0xC0101014;
	private static final int ACCENT = 0xFFFFC24A;
	private static final int HEADING = 0xFFFFFFFF;

	private static final Component SIDEBAR_HEADING = Component.translatable("bhop.settings.heading");

	/** The sidebar, in display order. A new section is one more entry here. */
	private final List<SettingsPage> sections = List.of(new GeneralPage(), new HudPage());

	/** Reopening the window returns to the section that was open last. */
	private static int lastSection;

	private int section;
	private SettingsPage page;

	private record Label(int x, int y, Supplier<Component> text, int colour) {
	}

	private final List<Label> labels = new ArrayList<>();

	public SettingsScreen() {
		super(Component.translatable("bhop.settings.title"));
		this.section = Math.min(lastSection, this.sections.size() - 1);
		this.page = this.sections.get(this.section);
		this.page.onOpen();
	}

	/** Shows a sidebar section's main page. */
	public void openSection(final int index) {
		this.section = index;
		lastSection = index;
		this.open(this.sections.get(index));
	}

	/** Shows any page, such as a sub-page of the current section. */
	public void open(final SettingsPage next) {
		this.page = next;
		next.onOpen();
		this.rebuildWidgets();
	}

	@Override
	protected void init() {
		this.labels.clear();

		final int sidebarX = MARGIN + PADDING;
		int y = MARGIN + HEADER_HEIGHT;
		for (int i = 0; i < this.sections.size(); i++) {
			final int index = i;
			this.addRenderableWidget(Button.builder(this.sections.get(i).title(), button -> this.openSection(index))
					.bounds(sidebarX, y, SIDEBAR_WIDTH - 2 * PADDING, SECTION_BUTTON_HEIGHT)
					.build());
			y += SECTION_BUTTON_HEIGHT + SECTION_BUTTON_SPACING;
		}

		this.page.build(new PageBuilder(this, contentLeft() + PADDING, MARGIN + HEADER_HEIGHT, CONTENT_WIDTH - 2 * PADDING));
	}

	<T extends AbstractWidget> T addPageWidget(final T widget) {
		return this.addRenderableWidget(widget);
	}

	void addPageLabel(final int x, final int y, final Supplier<Component> text, final int colour) {
		this.labels.add(new Label(x, y, text, colour));
	}

	private static int contentLeft() {
		return MARGIN + SIDEBAR_WIDTH + GAP;
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		// Panels only, in place of vanilla's full-screen blur, so the world stays in view.
		final int bottom = this.height - MARGIN;
		graphics.fill(MARGIN, MARGIN, MARGIN + SIDEBAR_WIDTH, bottom, PANEL);
		graphics.fill(contentLeft(), MARGIN, contentLeft() + CONTENT_WIDTH, bottom, PANEL);

		// Accent bar beside the open section.
		final int selectedY = MARGIN + HEADER_HEIGHT + this.section * (SECTION_BUTTON_HEIGHT + SECTION_BUTTON_SPACING);
		graphics.fill(MARGIN + 2, selectedY, MARGIN + 4, selectedY + SECTION_BUTTON_HEIGHT, ACCENT);

		this.minecraft.gui.hud.extractDeferredSubtitles();
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		final int headingY = MARGIN + (HEADER_HEIGHT - this.font.lineHeight) / 2;
		// The full title is narrated; the sidebar is only wide enough for the short name.
		graphics.text(this.font, SIDEBAR_HEADING, MARGIN + PADDING, headingY, ACCENT);
		graphics.text(this.font, this.page.title(), contentLeft() + PADDING, headingY, HEADING);

		for (final Label label : this.labels) {
			graphics.text(this.font, label.text().get(), label.x(), label.y(), label.colour());
		}
	}

	@Override
	public boolean isPauseScreen() {
		// Keeps the world running behind the window.
		return false;
	}

	/** Lays the current page out again, for when a change affects what the page should show. */
	public void refresh() {
		this.rebuildWidgets();
	}

	@Override
	public void removed() {
		ConfigManager.save();
		// Guests of a world opened to LAN move with the host's physics, so they get the new ones too.
		ServerSession.publishHostPhysics(this.minecraft);
	}
}
