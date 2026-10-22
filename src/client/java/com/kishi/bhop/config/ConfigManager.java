package com.kishi.bhop.config;

import java.nio.file.Path;

/**
 * Loads and saves {@link ModConfig} as JSON in the standard Fabric config directory.
 *
 * <p>Missing or unreadable files fall back to defaults rather than failing, so a bad edit can never
 * stop the game from starting. Values that parse but are unusable are repaired by
 * {@link ModConfig#sanitize}, so everything handed out by {@link #get} is safe to use as-is.
 */
public final class ConfigManager {

	private static final Path PATH = JsonFiles.configFile("");

	private static ModConfig config = new ModConfig();

	private ConfigManager() {
	}

	public static ModConfig get() {
		return config;
	}

	/** Reads the config from disk, writing out a default file if none exists yet. */
	public static void load() {
		config = JsonFiles.load(PATH, ModConfig.class, ModConfig::new, ModConfig::migrate, ModConfig::sanitize);
	}

	public static void save() {
		JsonFiles.save(PATH, config);
	}
}
