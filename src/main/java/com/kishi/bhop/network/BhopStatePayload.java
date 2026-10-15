package com.kishi.bhop.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the server refused a toggle, so this is the state the player actually has.
 *
 * @param enabled the state the client should go back to
 * @param reason why the toggle was refused, so the client can tell the player
 */
public record BhopStatePayload(boolean enabled, Refusal reason) implements CustomPacketPayload {

	public static final Type<BhopStatePayload> TYPE = ModPayloads.type("state");

	public static final StreamCodec<ByteBuf, BhopStatePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			BhopStatePayload::enabled,
			ByteBufCodecs.idMapper(Refusal::byId, Refusal::ordinal),
			BhopStatePayload::reason,
			BhopStatePayload::new);

	@Override
	public Type<BhopStatePayload> type() {
		return TYPE;
	}

	/** Why the server turned a toggle down. */
	public enum Refusal {
		/** The player's permission level is below the server's minimum. */
		NOT_ALLOWED,
		/** The server keeps the mode on for everyone. */
		FORCED_ON,
		/** The server has the mode switched off for everyone. */
		DISABLED,
		/** The player toggled again before the cooldown ran out. */
		COOLDOWN;

		private static final Refusal[] VALUES = values();

		/** Unknown ids, from a newer server, fall back to the most general reason. */
		static Refusal byId(final int id) {
			return id >= 0 && id < VALUES.length ? VALUES[id] : NOT_ALLOWED;
		}
	}
}
