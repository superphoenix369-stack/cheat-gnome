package com.cheatgnome.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.cheatgnome.GnomeConfig;
import com.cheatgnome.net.GnomeSettingPayload;
import com.cheatgnome.net.OpenGnomeMenuPayload;

/** In-game settings for the gnome. Changes are sent to the server, which saves config/cheatgnome.json. */
public class GnomeSettingsScreen extends Screen {
	private static final int BUTTON_WIDTH = 160;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;

	private final Screen parent;
	private GnomeConfig.Mode mode;
	private final boolean[] toggles = new boolean[GnomeConfig.TOGGLE_KEYS.length];

	public GnomeSettingsScreen(Screen parent, OpenGnomeMenuPayload info) {
		super(Component.translatable("screen.cheatgnome.settings"));
		this.parent = parent;

		GnomeConfig.Mode parsed;

		try {
			parsed = GnomeConfig.Mode.valueOf(info.mode());
		} catch (IllegalArgumentException e) {
			parsed = GnomeConfig.Mode.AUTO;
		}

		this.mode = parsed;

		for (int i = 0; i < toggles.length; i++) {
			toggles[i] = (info.toggles() & (1 << i)) != 0;
		}
	}

	@Override
	protected void init() {
		int count = 1 + toggles.length;
		int rows = (count + 1) / 2;
		int gridWidth = 2 * BUTTON_WIDTH + GAP;
		int gridHeight = rows * BUTTON_HEIGHT + (rows - 1) * GAP;
		int left = (this.width - gridWidth) / 2;
		int top = Math.max(36, (this.height - gridHeight) / 2 - 6);

		// Mode button first, then one button per on/off setting.
		Button modeButton = Button.builder(modeLabel(), b -> {
			mode = mode.next();
			b.setMessage(modeLabel());
			ClientPlayNetworking.send(new GnomeSettingPayload("mode", mode.name()));
		}).pos(left, top).size(BUTTON_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("setting.cheatgnome.mode.desc")))
				.build();
		this.addRenderableWidget(modeButton);

		for (int i = 0; i < toggles.length; i++) {
			final int index = i;
			int slot = i + 1;
			int x = left + (slot % 2) * (BUTTON_WIDTH + GAP);
			int y = top + (slot / 2) * (BUTTON_HEIGHT + GAP);
			String key = GnomeConfig.TOGGLE_KEYS[i];

			this.addRenderableWidget(Button.builder(toggleLabel(index), b -> {
				toggles[index] = !toggles[index];
				b.setMessage(toggleLabel(index));
				ClientPlayNetworking.send(new GnomeSettingPayload(key, Boolean.toString(toggles[index])));
			}).pos(x, y).size(BUTTON_WIDTH, BUTTON_HEIGHT)
					.tooltip(Tooltip.create(Component.translatable("setting.cheatgnome." + key + ".desc")))
					.build());
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
				.pos(this.width / 2 - 60, top + gridHeight + 12)
				.size(120, 20)
				.build());
	}

	private Component modeLabel() {
		return Component.translatable("setting.cheatgnome.mode", Component.translatable("setting.cheatgnome.mode." + mode.name().toLowerCase()));
	}

	private Component toggleLabel(int index) {
		return Component.translatable("setting.cheatgnome." + GnomeConfig.TOGGLE_KEYS[index],
				toggles[index] ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int count = 1 + toggles.length;
		int rows = (count + 1) / 2;
		int gridHeight = rows * BUTTON_HEIGHT + (rows - 1) * GAP;
		int top = Math.max(36, (this.height - gridHeight) / 2 - 6);
		graphics.centeredText(this.font, this.title.getString(), this.width / 2, top - 16, 0xFFFFD86B);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
