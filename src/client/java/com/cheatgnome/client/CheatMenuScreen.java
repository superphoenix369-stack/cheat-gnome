package com.cheatgnome.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.cheatgnome.gameplay.Cheats;
import com.cheatgnome.net.GnomeCheatPayload;
import com.cheatgnome.net.OpenGnomeMenuPayload;

/** The gnome's cheat menu: a grid of one-click cheats. */
public class CheatMenuScreen extends Screen {
	private static final int COLUMNS = 3;
	private static final int BUTTON_WIDTH = 116;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;

	private final OpenGnomeMenuPayload info;

	public CheatMenuScreen(OpenGnomeMenuPayload info) {
		super(Component.translatable("screen.cheatgnome.title"));
		this.info = info;
	}

	@Override
	protected void init() {
		int rows = (Cheats.IDS.length + COLUMNS - 1) / COLUMNS;
		int gridWidth = COLUMNS * BUTTON_WIDTH + (COLUMNS - 1) * GAP;
		int gridHeight = rows * BUTTON_HEIGHT + (rows - 1) * GAP;
		int left = (this.width - gridWidth) / 2;
		int top = Math.max(44, (this.height - gridHeight) / 2);

		for (int i = 0; i < Cheats.IDS.length; i++) {
			String id = Cheats.IDS[i];
			int x = left + (i % COLUMNS) * (BUTTON_WIDTH + GAP);
			int y = top + (i / COLUMNS) * (BUTTON_HEIGHT + GAP);

			Button button = Button.builder(Component.translatable("cheat.cheatgnome." + id), b -> this.press(id))
					.pos(x, y)
					.size(BUTTON_WIDTH, BUTTON_HEIGHT)
					.tooltip(Tooltip.create(Component.translatable("cheat.cheatgnome." + id + ".desc")))
					.build();

			if (id.equals("settings") && !info.canEditSettings()) {
				button.active = false;
				button.setTooltip(Tooltip.create(Component.translatable("cheat.cheatgnome.settings.locked")));
			}

			this.addRenderableWidget(button);
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
				.pos(this.width / 2 - 60, top + gridHeight + 12)
				.size(120, 20)
				.build());
	}

	private void press(String id) {
		if (id.equals("settings")) {
			Minecraft.getInstance().gui.setScreen(new GnomeSettingsScreen(this, info));
			return;
		}

		ClientPlayNetworking.send(new GnomeCheatPayload(info.pos(), id));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int rows = (Cheats.IDS.length + COLUMNS - 1) / COLUMNS;
		int gridHeight = rows * BUTTON_HEIGHT + (rows - 1) * GAP;
		int top = Math.max(44, (this.height - gridHeight) / 2);

		graphics.centeredText(this.font, this.title.getString(), this.width / 2, top - 30, 0xFFFFD86B);
		graphics.centeredText(this.font, Component.translatable("screen.cheatgnome.subtitle").getString(), this.width / 2, top - 16, 0xFFB8E0A8);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
