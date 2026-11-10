package com.cheatgnome.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;

import com.cheatgnome.CheatGnome;
import com.cheatgnome.block.GnomeLook;
import com.cheatgnome.net.OpenGnomeMenuPayload;

public class CheatGnomeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Hat (tint 0), beard (tint 1) and coat (tint 2) colors come from each gnome's block entity.
		BlockColorRegistry.register((state, level, pos, tints) -> {
			GnomeLook look = GnomeLook.DEFAULT;

			if (level != null && pos != null && level.getBlockEntityRenderData(pos) instanceof GnomeLook data) {
				look = data;
			}

			tints.size(3);
			tints.set(0, 0xFF000000 | look.hatRgb());
			tints.set(1, 0xFF000000 | look.beardRgb());
			tints.set(2, 0xFF000000 | look.coatRgb());
		}, CheatGnome.GNOME_BLOCK);

		// Decoys always wear the classic outfit.
		BlockColorRegistry.register(List.of(
				constant(GnomeLook.DEFAULT.hatRgb()),
				constant(GnomeLook.DEFAULT.beardRgb()),
				constant(GnomeLook.DEFAULT.coatRgb())
		), CheatGnome.DECOY_GNOME_BLOCK);

		ClientPlayNetworking.registerGlobalReceiver(OpenGnomeMenuPayload.TYPE, (payload, context) -> {
			Minecraft.getInstance().gui.setScreen(new CheatMenuScreen(payload));
		});
	}

	private static BlockTintSource constant(int rgb) {
		return new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				return 0xFF000000 | rgb;
			}
		};
	}
}
