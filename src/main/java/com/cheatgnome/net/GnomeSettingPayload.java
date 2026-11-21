package com.cheatgnome.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cheatgnome.CheatGnome;

/** Client to server: change one gnome setting. */
public record GnomeSettingPayload(String key, String value) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GnomeSettingPayload> TYPE = new CustomPacketPayload.Type<>(CheatGnome.id("setting"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GnomeSettingPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, GnomeSettingPayload::key,
			ByteBufCodecs.STRING_UTF8, GnomeSettingPayload::value,
			GnomeSettingPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
