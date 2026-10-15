package com.kishi.bhop.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kishi.bhop.BhopMod;
import com.kishi.bhop.physics.PhysicsSetting;
import com.kishi.bhop.physics.PhysicsSettings;

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
		if (!Files.exists(PATH)) {
			config = new ModConfig();
			save();
			return;
		}

		try {
			final JsonObject tree = JsonFiles.read(PATH, JsonObject.class);
			final boolean migrated = tree != null && migratePhysics(tree);
			final ModConfig loaded = tree != null ? JsonFiles.fromTree(tree, ModConfig.class) : null;
			if (loaded != null) {
				loaded.sanitize();
			}

			config = loaded != null ? loaded : new ModConfig();
			if (migrated) {
				save();
			}
		} catch (IOException | JsonParseException e) {
			BhopMod.LOGGER.warn("Could not read {}, falling back to defaults", PATH, e);
			config = new ModConfig();
		}
	}

	public static void save() {
		try {
			JsonFiles.write(PATH, config);
		} catch (IOException e) {
			BhopMod.LOGGER.warn("Could not write {}", PATH, e);
		}
	}

	/**
	 * Moves the physics values out of {@code movement} into their own {@code physics} section.
	 *
	 * <p>Files written before servers could share physics kept them under {@code movement}, with the
	 * same names they have now. Moving them keeps a player's own tuning instead of resetting it.
	 *
	 * @return whether anything was moved, so the file is rewritten in the new layout
	 */
	private static boolean migratePhysics(final JsonObject tree) {
		if (tree.has("physics") || !(tree.get("movement") instanceof JsonObject movement)) {
			return false;
		}

		final JsonObject physics = new JsonObject();
		for (final PhysicsSetting field : PhysicsSettings.FIELDS) {
			if (movement.has(field.name())) {
				physics.add(field.name(), movement.remove(field.name()));
			}
		}

		tree.add("physics", physics);
		BhopMod.LOGGER.info("Moved {} physics values in {} to their own section", physics.size(), PATH);
		return true;
	}
}
