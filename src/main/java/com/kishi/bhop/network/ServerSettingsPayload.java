package com.kishi.bhop.network;

import com.kishi.bhop.physics.PhysicsSettings;
import com.kishi.bhop.rules.PlayerRules;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the physics everyone on the server moves with, and the rules for this player.
 *
 * <p>Sent when a player running the mod joins, and again to everyone whenever an admin changes a
 * value. It is the only message whose size grows with the settings, and it is never sent per tick.
 */
public record ServerSettingsPayload(PhysicsSettings physics, PlayerRules rules) implements CustomPacketPayload {

	public static final Type<ServerSettingsPayload> TYPE = ModPayloads.type("settings");

	public static final StreamCodec<FriendlyByteBuf, ServerSettingsPayload> STREAM_CODEC = StreamCodec.composite(
			PhysicsSettings.STREAM_CODEC,
			ServerSettingsPayload::physics,
			PlayerRules.STREAM_CODEC,
			ServerSettingsPayload::rules,
			ServerSettingsPayload::new);

	@Override
	public Type<ServerSettingsPayload> type() {
		return TYPE;
	}
}
