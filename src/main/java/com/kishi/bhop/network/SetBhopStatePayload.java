package com.kishi.bhop.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player switched bunny hop mode on or off.
 *
 * <p>The client applies the change straight away and tells the server afterwards, so toggling never
 * waits on a round trip. If the server's rules do not allow the change, it answers with a
 * {@link BhopStatePayload} that puts the client back.
 */
public record SetBhopStatePayload(boolean enabled) implements CustomPacketPayload {

	public static final Type<SetBhopStatePayload> TYPE = ModPayloads.type("set_state");

	public static final StreamCodec<ByteBuf, SetBhopStatePayload> STREAM_CODEC =
			ByteBufCodecs.BOOL.map(SetBhopStatePayload::new, SetBhopStatePayload::enabled);

	@Override
	public Type<SetBhopStatePayload> type() {
		return TYPE;
	}
}
