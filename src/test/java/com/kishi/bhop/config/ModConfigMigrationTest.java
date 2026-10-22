package com.kishi.bhop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/** Checks that {@link ModConfig#migrate} carries a player's tuning over from the old file layout. */
class ModConfigMigrationTest {

	@Test
	void movesPhysicsOutOfMovement() {
		final JsonObject tree = parse("""
				{"movement": {"autoHop": true, "airAccelerate": 20.0, "friction": 4.0}}""");

		assertTrue(ModConfig.migrate(tree));

		final JsonObject movement = tree.getAsJsonObject("movement");
		assertTrue(movement.has("autoHop"));
		assertFalse(movement.has("airAccelerate"));
		assertFalse(movement.has("friction"));

		final ModConfig config = new Gson().fromJson(tree, ModConfig.class);
		assertTrue(config.movement.autoHop);
		assertEquals(20.0D, config.physics.airAccelerate);
		assertEquals(4.0D, config.physics.friction);
	}

	@Test
	void leavesTheNewLayoutAlone() {
		final JsonObject tree = parse("""
				{"physics": {"airAccelerate": 20.0}, "movement": {"friction": 4.0}}""");
		final JsonObject before = tree.deepCopy();

		assertFalse(ModConfig.migrate(tree));
		assertEquals(before, tree);
	}

	@Test
	void ignoresFilesWithoutAMovementSection() {
		assertFalse(ModConfig.migrate(parse("{}")));
		assertFalse(ModConfig.migrate(parse("""
				{"movement": null}""")));
	}

	private static JsonObject parse(final String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}
}
