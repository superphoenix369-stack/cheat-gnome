package com.cheatgnome.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cheatgnome.CheatGnome;

/** Client to server: run a cheat from the gnome at {@code pos}. */
public record GnomeCheatPayload(BlockPos pos, String cheat) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GnomeCheatPayload> TYPE = new CustomPacketPayload.Type<>(CheatGnome.id("cheat"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GnomeCheatPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, GnomeCheatPayload::pos,
			ByteBufCodecs.STRING_UTF8, GnomeCheatPayload::cheat,
			GnomeCheatPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
