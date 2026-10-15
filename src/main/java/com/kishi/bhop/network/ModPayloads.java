package com.kishi.bhop.network;

import com.kishi.bhop.BhopMod;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The mod's network messages, registered on both sides.
 *
 * <p>The protocol version is part of every channel name. Fabric only lets a side send on a channel
 * the other side registered under exactly that name, so a client and server on different versions
 * simply see each other as not running the mod, instead of failing to decode a message and
 * disconnecting the player.
 */
public final class ModPayloads {

	/** Bump whenever any message's layout changes, including the list of physics values. */
	public static final int PROTOCOL = 1;

	private ModPayloads() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(ServerSettingsPayload.TYPE, ServerSettingsPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BhopStatePayload.TYPE, BhopStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetBhopStatePayload.TYPE, SetBhopStatePayload.STREAM_CODEC);
	}

	/** {@code bhop:<name>_v<protocol>}. */
	static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(final String name) {
		return new CustomPacketPayload.Type<>(BhopMod.id(name + "_v" + PROTOCOL));
	}
}
