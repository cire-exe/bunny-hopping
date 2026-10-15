package com.kishi.bhop.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonParseException;
import com.kishi.bhop.BhopMod;
import com.kishi.bhop.config.JsonFiles;
import com.kishi.bhop.config.Values;
import com.kishi.bhop.physics.PhysicsSettings;
import com.kishi.bhop.rules.ServerRules;

/**
 * {@code config/bhop-server.json}: the rules for the bunny hop toggle, and the physics a dedicated
 * server sends its players.
 *
 * <p>A world opened to LAN uses the rules from here too, but its physics are the host's own, set
 * from their settings screen.
 */
public final class ServerConfig {

	private static final Path PATH = JsonFiles.configFile("-server");

	private static ServerConfig config = new ServerConfig();

	public ServerRules rules = new ServerRules();
	public PhysicsSettings physics = new PhysicsSettings();

	public static ServerConfig get() {
		return config;
	}

	/** Reads the file, writing out a default one if none exists yet. Never fails. */
	public static void load() {
		if (!Files.exists(PATH)) {
			config = new ServerConfig();
			save();
			return;
		}

		try {
			final ServerConfig loaded = JsonFiles.read(PATH, ServerConfig.class);
			config = loaded != null ? loaded : new ServerConfig();
			config.sanitize();
		} catch (IOException | JsonParseException e) {
			BhopMod.LOGGER.warn("Could not read {}, falling back to defaults", PATH, e);
			config = new ServerConfig();
		}
	}

	public static void save() {
		try {
			JsonFiles.write(PATH, config);
		} catch (IOException e) {
			BhopMod.LOGGER.warn("Could not write {}", PATH, e);
		}
	}

	private void sanitize() {
		this.rules = Values.orDefault("rules", this.rules, ServerRules::new);
		this.physics = Values.orDefault("physics", this.physics, PhysicsSettings::new);
		this.rules.sanitize();
		this.physics.sanitize();
	}
}
