package com.kishi.bhop.network;

import com.kishi.bhop.rules.Refusal;

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
}
