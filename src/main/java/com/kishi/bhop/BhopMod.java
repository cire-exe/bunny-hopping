package com.kishi.bhop;

import com.kishi.bhop.network.ModPayloads;
import com.kishi.bhop.server.BhopCommands;
import com.kishi.bhop.server.BhopServer;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point, run on both the dedicated server and the client's integrated server.
 *
 * <p>Owns what both sides share: the mod id, the logger and, on servers, everything that hands
 * players the server's physics and enforces its rules for the bunny hop toggle. Movement itself is
 * still simulated entirely on each player's client.
 */
public final class BhopMod implements ModInitializer {

	public static final String MOD_ID = "bhop";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModPayloads.register();
		BhopServer.register();
		BhopCommands.register();
	}
}
