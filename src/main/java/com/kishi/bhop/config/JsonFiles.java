package com.kishi.bhop.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.kishi.bhop.BhopMod;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Shared plumbing for the mod's hand-editable JSON files in the Fabric config directory.
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

	/** Parses the file, returning {@code null} if it is empty. */
	public static <T> T read(final Path path, final Type type) throws IOException, JsonParseException {
		try (Reader reader = Files.newBufferedReader(path)) {
			return GSON.fromJson(reader, type);
		}
	}

	/** Converts an already parsed tree, such as one a migration has just rewritten. */
	public static <T> T fromTree(final JsonElement tree, final Class<T> type) throws JsonParseException {
		return GSON.fromJson(tree, type);
	}

	public static void write(final Path path, final Object value) throws IOException {
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
