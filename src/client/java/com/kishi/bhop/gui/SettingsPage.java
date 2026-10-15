package com.kishi.bhop.gui;

import net.minecraft.network.chat.Component;

/**
 * One page of the settings screen: either a sidebar section or a sub-page opened from one.
 *
 * <p>A page only describes its widgets. The screen owns the frame around it, so adding a section is
 * a matter of writing a page and listing it in {@link SettingsScreen}.
 */
public interface SettingsPage {

	/** Heading shown above the page, and on the sidebar button for a section. */
	Component title();

	/** Lays out the page's widgets. Called again whenever the screen is rebuilt, e.g. on resize. */
	void build(PageBuilder page);

	/** Called each time the page is navigated to, before it is built. */
	default void onOpen() {
	}
}
