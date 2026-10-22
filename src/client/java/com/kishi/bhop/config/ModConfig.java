package com.kishi.bhop.config;

import static com.kishi.bhop.config.Values.atLeast;
import static com.kishi.bhop.config.Values.inRange;
import static com.kishi.bhop.config.Values.orDefault;

import com.google.gson.JsonObject;
import com.kishi.bhop.BhopMod;
import com.kishi.bhop.physics.PhysicsSetting;
import com.kishi.bhop.physics.PhysicsSettings;
import com.kishi.bhop.rules.ServerRules;

/**
 * Plain data holder for every tunable value in the mod.
 *
 * <p>Serialised to and from JSON by {@link ConfigManager}, so every field is public and the class
 * keeps a no-argument constructor. Defaults are the shipped "classic FPS feel, tuned for Minecraft" values.
 */
public final class ModConfig {

	/**
	 * Movement physics for singleplayer and for worlds opened to LAN. A server running the mod sends
	 * its own, which replace these while connected.
	 */
	public PhysicsSettings physics = new PhysicsSettings();
	public Movement movement = new Movement();
	public Hud hud = new Hud();

	/**
	 * Repairs anything a hand edit can break, so the rest of the mod can trust every value.
	 *
	 * <p>A section written as {@code null} is replaced with its defaults. A value that is not a
	 * finite number goes back to its default, and one outside its usable range is clamped to the
	 * nearest end of it. Every repair is logged, naming the key, so the edit is easy to find.
	 */
	void sanitize() {
		this.physics = orDefault("physics", this.physics, PhysicsSettings::new);
		this.movement = orDefault("movement", this.movement, Movement::new);
		this.hud = orDefault("hud", this.hud, Hud::new);

		this.physics.sanitize();
		this.movement.sanitize();
		this.hud.sanitize();
	}

	/**
	 * Moves the physics values out of {@code movement} into their own {@code physics} section.
	 *
	 * <p>Files written before servers could share physics kept them under {@code movement}, with the
	 * same names they have now. Moving them keeps a player's own tuning instead of resetting it.
	 *
	 * @return whether anything was moved, so the file is rewritten in the new layout
	 */
	static boolean migrate(final JsonObject tree) {
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
		BhopMod.LOGGER.info("Moved {} physics values in the config to their own section", physics.size());
		return true;
	}

	/**
	 * Personal movement preferences: how jumps are asked for, not how movement behaves.
	 *
	 * <p>A server running the mod can switch auto hop off and cap the jump buffer, but within its
	 * limits these stay each player's own choice.
	 */
	public static final class Movement {
		/** Hold jump to chain hops. Off by default, so every landing needs manual timing. */
		public boolean autoHop = false;

		/**
		 * Ticks a jump press is remembered for, after the tick it was made on.
		 *
		 * <p>A press up to this many ticks before landing still hops on the landing tick. 0 means the
		 * press has to land on exactly that tick.
		 */
		public int jumpBufferTicks = 0;

		/**
		 * Jump with the mouse wheel while bunny hop mode is on, as shooter players often bind it. The wheel
		 * then no longer changes the hotbar slot until the mode is switched off.
		 */
		public boolean scrollJump = true;

		private void sanitize() {
			this.jumpBufferTicks = inRange("movement.jumpBufferTicks", this.jumpBufferTicks, 0, ServerRules.MAX_JUMP_BUFFER_TICKS);
		}
	}

	public static final class Hud {
		/** Draw the speedometer while bunny hop mode is active. */
		public boolean showSpeed = true;

		/**
		 * Scale of the speedometer text.
		 *
		 * <p>The vanilla font is nine pixels tall, so the default renders it at about five.
		 */
		public float speedTextScale = 0.56F;

		/** Distance of the speedometer above the bottom of the screen, in scaled pixels. */
		public int speedOffsetY = 60;

		/** Under the speedometer: speed gained or lost on the last hop, green or red. */
		public boolean showGain = true;

		/** Under the speedometer: whether the last hop was perfect, or how many ticks late. */
		public boolean showHopTiming = true;

		/** Under the speedometer: how much of the last air strafe's mouse turning matched the key. */
		public boolean showSync = true;

		/** Show a message above the hotbar whenever the mode is toggled. */
		public boolean announceToggle = false;

		/**
		 * How much shorter the toggle message is drawn than vanilla's action bar text, in real
		 * screen pixels. Set to 0 for vanilla size.
		 *
		 * <p>Screen pixels rather than a scale factor, so the reduction looks the same at every
		 * GUI scale and window size.
		 */
		public int statusTextShrinkPixels = 10;

		private void sanitize() {
			this.speedTextScale = atLeast("hud.speedTextScale", this.speedTextScale, 0.1F, new Hud().speedTextScale);
			this.statusTextShrinkPixels = atLeast("hud.statusTextShrinkPixels", this.statusTextShrinkPixels, 0);
		}
	}
}
