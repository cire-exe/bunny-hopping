package com.kishi.bhop.server;

import java.nio.file.Path;

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
		config = JsonFiles.load(PATH, ServerConfig.class, ServerConfig::new, ServerConfig::sanitize);
	}

	public static void save() {
		JsonFiles.save(PATH, config);
	}

	private void sanitize() {
		this.rules = Values.orDefault("rules", this.rules, ServerRules::new);
		this.physics = Values.orDefault("physics", this.physics, PhysicsSettings::new);
		this.rules.sanitize();
		this.physics.sanitize();
	}
}
