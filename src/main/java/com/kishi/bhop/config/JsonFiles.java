package com.kishi.bhop.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kishi.bhop.BhopMod;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Shared plumbing for the mod's hand-editable JSON files in the Fabric config directory.
 *
 * <p>Missing or unreadable files fall back to defaults rather than failing, so a bad edit can never
 * stop the game or server from starting.
 *
 * <p>Writes go through a temporary file that is then moved into place, so a crash mid-write leaves
 * the previous file intact instead of a truncated one that would silently fall back to defaults.
 */
public final class JsonFiles {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private JsonFiles() {
	}

	/** {@code config/bhop<suffix>.json}. */
	public static Path configFile(final String suffix) {
		return FabricLoader.getInstance().getConfigDir().resolve(BhopMod.MOD_ID + suffix + ".json");
	}

	/** {@link #load(Path, Class, Supplier, Predicate, Consumer)} for a file that has only ever had one layout. */
	public static <T> T load(final Path path, final Class<T> type, final Supplier<T> defaults, final Consumer<T> sanitize) {
		return load(path, type, defaults, tree -> false, sanitize);
	}

	/**
	 * Reads and repairs a config file, writing out the defaults if there is none yet. Never fails.
	 *
	 * @param migrate  rewrites an older layout in place before it is read, and returns whether it
	 *                 changed anything, so the file is saved again in the new layout
	 * @param sanitize repairs the loaded value in place
	 */
	public static <T> T load(
			final Path path,
			final Class<T> type,
			final Supplier<T> defaults,
			final Predicate<JsonObject> migrate,
			final Consumer<T> sanitize) {
		if (!Files.exists(path)) {
			final T created = defaults.get();
			save(path, created);
			return created;
		}

		try {
			final JsonObject tree;
			try (Reader reader = Files.newBufferedReader(path)) {
				tree = GSON.fromJson(reader, JsonObject.class);
			}

			// An empty file parses to nothing.
			if (tree == null) {
				return defaults.get();
			}

			final boolean migrated = migrate.test(tree);
			final T loaded = GSON.fromJson(tree, type);
			sanitize.accept(loaded);
			if (migrated) {
				save(path, loaded);
			}

			return loaded;
		} catch (IOException | JsonParseException e) {
			BhopMod.LOGGER.warn("Could not read {}, falling back to defaults", path, e);
			return defaults.get();
		}
	}

	/** Writes the file, logging rather than throwing if it cannot be written. */
	public static void save(final Path path, final Object value) {
		try {
			write(path, value);
		} catch (IOException e) {
			BhopMod.LOGGER.warn("Could not write {}", path, e);
		}
	}

	private static void write(final Path path, final Object value) throws IOException {
		Files.createDirectories(path.getParent());
		final Path temp = path.resolveSibling(path.getFileName() + ".tmp");

		try (Writer writer = Files.newBufferedWriter(temp)) {
			GSON.toJson(value, writer);
		}

		try {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
