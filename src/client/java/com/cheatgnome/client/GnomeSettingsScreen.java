package com.cheatgnome.client;

import org.jspecify.annotations.Nullable;

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

/** In-game settings for the gnomes. Changes are sent to the server, which saves config/cheatgnome.json. */
public class GnomeSettingsScreen extends Screen {
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;

	private final @Nullable Screen parent;
	private final OpenGnomeMenuPayload info;
	private final boolean[] toggles = new boolean[GnomeConfig.TOGGLE_KEYS.length];

	private int columns;
	private int buttonWidth;

	public GnomeSettingsScreen(@Nullable Screen parent, OpenGnomeMenuPayload info) {
		super(Component.translatable("screen.cheatgnome.settings"));
		this.parent = parent;
		this.info = info;

		for (int i = 0; i < toggles.length; i++) {
			toggles[i] = (info.toggles() & (1 << i)) != 0;
		}
	}

	private int rows() {
		return (toggles.length + columns - 1) / columns;
	}

	private int gridHeight() {
		return rows() * BUTTON_HEIGHT + (rows() - 1) * GAP;
	}

	private int top() {
		return Math.max(30, (this.height - gridHeight()) / 2 - 10);
	}

	@Override
	protected void init() {
		// Three columns when there is room, two on small windows.
		columns = this.width >= 3 * 140 + 2 * GAP + 10 ? 3 : 2;
		buttonWidth = Math.min(150, (this.width - 10 - (columns - 1) * GAP) / columns);

		int gridWidth = columns * buttonWidth + (columns - 1) * GAP;
		int left = (this.width - gridWidth) / 2;
		int top = top();

		for (int i = 0; i < toggles.length; i++) {
			final int index = i;
			int x = left + (i % columns) * (buttonWidth + GAP);
			int y = top + (i / columns) * (BUTTON_HEIGHT + GAP);
			String key = GnomeConfig.TOGGLE_KEYS[i];

			this.addRenderableWidget(Button.builder(toggleLabel(index), b -> {
				toggles[index] = !toggles[index];
				b.setMessage(toggleLabel(index));
				ClientPlayNetworking.send(new GnomeSettingPayload(info.pos(), key, Boolean.toString(toggles[index])));
			}).pos(x, y).size(buttonWidth, BUTTON_HEIGHT)
					.tooltip(Tooltip.create(Component.translatable("setting.cheatgnome." + key + ".desc")))
					.build());
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
				.pos(this.width / 2 - 60, top + gridHeight() + 10)
				.size(120, 20)
				.build());
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
		int top = top();
		graphics.centeredText(this.font, this.title.getString(), this.width / 2, top - 16, 0xFFFFD86B);
		graphics.centeredText(this.font, Component.translatable("screen.cheatgnome.settings_hint").getString(),
				this.width / 2, top + gridHeight() + 36, 0xFFA0A0A0);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
