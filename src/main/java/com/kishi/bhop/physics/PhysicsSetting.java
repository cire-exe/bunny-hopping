package com.kishi.bhop.physics;

import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import com.kishi.bhop.config.Values;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One entry in {@link PhysicsSettings#FIELDS}: a named physics value and everything needed to copy,
 * repair and transmit it.
 *
 * <p>Sealed so that code which needs the value's type, such as the {@code /bhop physics} command
 * choosing an argument type, can switch over the three kinds exhaustively.
 */
public sealed interface PhysicsSetting {

	/** The JSON key, the command literal and the log key, all in one. */
	String name();

	void copy(PhysicsSettings from, PhysicsSettings to);

	/** Repairs the value in place, falling back to the one in {@code defaults}. */
	void sanitize(PhysicsSettings settings, PhysicsSettings defaults);

	void write(FriendlyByteBuf buf, PhysicsSettings settings);

	void read(FriendlyByteBuf buf, PhysicsSettings settings);

	/** The current value as text, for command feedback. */
	String describe(PhysicsSettings settings);

	/** A value in blocks per second or a dimensionless factor. */
	record Decimal(
			String name,
			double min,
			double max,
			ToDoubleFunction<PhysicsSettings> getter,
			ObjDoubleConsumer<PhysicsSettings> setter) implements PhysicsSetting {

		public double get(final PhysicsSettings settings) {
			return this.getter.applyAsDouble(settings);
		}

		public void set(final PhysicsSettings settings, final double value) {
			this.setter.accept(settings, value);
		}

		@Override
		public void copy(final PhysicsSettings from, final PhysicsSettings to) {
			this.set(to, this.get(from));
		}

		@Override
		public void sanitize(final PhysicsSettings settings, final PhysicsSettings defaults) {
			this.set(settings, Values.inRange(key(this), this.get(settings), this.min, this.max, this.get(defaults)));
		}

		@Override
		public void write(final FriendlyByteBuf buf, final PhysicsSettings settings) {
			buf.writeDouble(this.get(settings));
		}

		@Override
		public void read(final FriendlyByteBuf buf, final PhysicsSettings settings) {
			this.set(settings, buf.readDouble());
		}

		@Override
		public String describe(final PhysicsSettings settings) {
			return Double.toString(this.get(settings));
		}
	}

	/** A count, such as sub-steps. */
	record Whole(
			String name,
			int min,
			int max,
			ToIntFunction<PhysicsSettings> getter,
			ObjIntConsumer<PhysicsSettings> setter) implements PhysicsSetting {

		public int get(final PhysicsSettings settings) {
			return this.getter.applyAsInt(settings);
		}

		public void set(final PhysicsSettings settings, final int value) {
			this.setter.accept(settings, value);
		}

		@Override
		public void copy(final PhysicsSettings from, final PhysicsSettings to) {
			this.set(to, this.get(from));
		}

		@Override
		public void sanitize(final PhysicsSettings settings, final PhysicsSettings defaults) {
			this.set(settings, Values.inRange(key(this), this.get(settings), this.min, this.max));
		}

		@Override
		public void write(final FriendlyByteBuf buf, final PhysicsSettings settings) {
			buf.writeVarInt(this.get(settings));
		}

		@Override
		public void read(final FriendlyByteBuf buf, final PhysicsSettings settings) {
			this.set(settings, buf.readVarInt());
		}

		@Override
		public String describe(final PhysicsSettings settings) {
			return Integer.toString(this.get(settings));
		}
	}

	/** An on/off switch. */
	record Flag(
			String name,
			Predicate<PhysicsSettings> getter,
			BiConsumer<PhysicsSettings, Boolean> setter) implements PhysicsSetting {

		public boolean get(final PhysicsSettings settings) {
			return this.getter.test(settings);
		}

		public void set(final PhysicsSettings settings, final boolean value) {
			this.setter.accept(settings, value);
		}

		@Override
		public void copy(final PhysicsSettings from, final PhysicsSettings to) {
			this.set(to, this.get(from));
		}

		@Override
		public void sanitize(final PhysicsSettings settings, final PhysicsSettings defaults) {
			// Every boolean is usable.
		}

		@Override
		public void write(final FriendlyByteBuf buf, final PhysicsSettings settings) {
			buf.writeBoolean(this.get(settings));
		}

		@Override
		public void read(final FriendlyByteBuf buf, final PhysicsSettings settings) {
			this.set(settings, buf.readBoolean());
		}

		@Override
		public String describe(final PhysicsSettings settings) {
			return Boolean.toString(this.get(settings));
		}
	}

	private static String key(final PhysicsSetting setting) {
		return "physics." + setting.name();
	}
}
