package com.cheatgnome.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cheatgnome.CheatGnome;

/**
 * Server to client: open the cheat menu for the gnome at {@code pos}.
 *
 * @param canEditSettings whether the player may open the settings page
 * @param mode            current gnome mode name
 * @param toggles         current on/off settings, one bit per entry in GnomeConfig.TOGGLE_KEYS
 */
public record OpenGnomeMenuPayload(BlockPos pos, boolean canEditSettings, String mode, int toggles) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenGnomeMenuPayload> TYPE = new CustomPacketPayload.Type<>(CheatGnome.id("open_menu"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenGnomeMenuPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenGnomeMenuPayload::pos,
			ByteBufCodecs.BOOL, OpenGnomeMenuPayload::canEditSettings,
			ByteBufCodecs.STRING_UTF8, OpenGnomeMenuPayload::mode,
			ByteBufCodecs.VAR_INT, OpenGnomeMenuPayload::toggles,
			OpenGnomeMenuPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
