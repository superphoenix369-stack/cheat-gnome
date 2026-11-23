package com.cheatgnome.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cheatgnome.CheatGnome;

/** Client to server: change one gnome setting, from the settings page opened at the gnome at {@code pos}. */
public record GnomeSettingPayload(BlockPos pos, String key, String value) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GnomeSettingPayload> TYPE = new CustomPacketPayload.Type<>(CheatGnome.id("setting"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GnomeSettingPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, GnomeSettingPayload::pos,
			ByteBufCodecs.STRING_UTF8, GnomeSettingPayload::key,
			ByteBufCodecs.STRING_UTF8, GnomeSettingPayload::value,
			GnomeSettingPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
