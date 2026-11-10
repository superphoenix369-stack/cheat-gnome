package com.cheatgnome.block;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import com.cheatgnome.CheatGnome;

/**
 * How a particular gnome looks: hat wool color, beard wool color and the terracotta it was made from.
 *
 * <p>Colors are stored as indexes into {@link #COLOR_NAMES} (vanilla dye order).
 * The terracotta value is 0 for plain terracotta, 1-16 for dyed terracotta and 17-32 for glazed terracotta.
 * Plain and dyed terracotta give the classic face; each glazed terracotta gives its own face.
 */
public record GnomeLook(int hat, int beard, int terracotta) {
	public static final String[] COLOR_NAMES = {
			"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
			"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
	};

	/** Vanilla wool colors, used for the hat and beard. */
	public static final int[] WOOL_RGB = {
			0xE9ECEC, 0xF07613, 0xBD44B3, 0x3AAFD9, 0xF8C627, 0x70B919, 0xED8DAC, 0x3E4447,
			0x8E8E86, 0x158991, 0x792AAC, 0x35399D, 0x724728, 0x546D1B, 0xA12722, 0x141519
	};

	/** Muted terracotta tones for the coat when made from dyed terracotta. */
	public static final int[] TERRACOTTA_RGB = {
			0xD1B2A1, 0xA15325, 0x95576C, 0x706C8A, 0xBA8523, 0x677534, 0xA04D4E, 0x392A23,
			0x876A61, 0x575B5B, 0x7A4958, 0x4C3E5C, 0x4D3323, 0x4C532A, 0x8F3D2E, 0x251610
	};

	/** Bright dye colors for the coat when made from glazed terracotta. */
	public static final int[] GLAZED_RGB = {
			0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
			0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
	};

	public static final int PLAIN_TERRACOTTA_RGB = 0x985E43;

	/** Face names. Index 0 is the classic face, 1-16 match the glazed terracotta colors in order. */
	public static final String[] FACES = {
			"classic",
			"serene", "winking", "lovestruck", "teary", "starry", "cheeky", "blushing", "grumpy",
			"sleepy", "spectacled", "pirate", "cool", "monocle", "mischievous", "angry", "spooky"
	};

	public static final int RED = 14;
	public static final int WHITE = 0;
	public static final GnomeLook DEFAULT = new GnomeLook(RED, WHITE, 0);

	public GnomeLook {
		hat = clamp(hat, 0, 15);
		beard = clamp(beard, 0, 15);
		terracotta = clamp(terracotta, 0, 32);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public int hatRgb() {
		return WOOL_RGB[hat];
	}

	public int beardRgb() {
		return WOOL_RGB[beard];
	}

	public int coatRgb() {
		if (terracotta == 0) {
			return PLAIN_TERRACOTTA_RGB;
		} else if (terracotta <= 16) {
			return TERRACOTTA_RGB[terracotta - 1];
		} else {
			return GLAZED_RGB[terracotta - 17];
		}
	}

	/** 0 = classic, 1-16 = glazed faces. */
	public int face() {
		return terracotta >= 17 ? terracotta - 16 : 0;
	}

	public String faceName() {
		return FACES[face()];
	}

	// ---------------------------------------------------------------------
	// Item stack conversion
	// ---------------------------------------------------------------------

	/** Builds a gnome item carrying this look. */
	public ItemStack toStack() {
		ItemStack stack = new ItemStack(CheatGnome.GNOME_ITEM);
		applyTo(stack);
		return stack;
	}

	public void applyTo(ItemStack stack) {
		// floats keep the raw look so it survives being placed and picked back up,
		// strings pick the face model and colors tint the hat, beard and coat.
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
				List.of((float) hat, (float) beard, (float) terracotta),
				List.of(),
				List.of(faceName()),
				List.of(hatRgb(), beardRgb(), coatRgb())
		));

		List<Component> lore = new ArrayList<>();
		lore.add(loreLine("hat", Component.translatable("color.minecraft." + COLOR_NAMES[hat])));
		lore.add(loreLine("beard", Component.translatable("color.minecraft." + COLOR_NAMES[beard])));
		lore.add(loreLine("coat", terracottaName()));
		lore.add(loreLine("face", Component.translatable("face.cheatgnome." + faceName())));
		stack.set(DataComponents.LORE, new ItemLore(lore));
	}

	private static Component loreLine(String key, Component value) {
		return Component.translatable("lore.cheatgnome." + key, value)
				.withStyle(ChatFormatting.GRAY)
				.withStyle(style -> style.withItalic(false));
	}

	private Component terracottaName() {
		if (terracotta == 0) {
			return Component.translatable("block.minecraft.terracotta");
		} else if (terracotta <= 16) {
			return Component.translatable("block.minecraft." + COLOR_NAMES[terracotta - 1] + "_terracotta");
		} else {
			return Component.translatable("block.minecraft." + COLOR_NAMES[terracotta - 17] + "_glazed_terracotta");
		}
	}

	public static GnomeLook fromStack(ItemStack stack) {
		CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);

		if (data == null || data.floats().size() < 3) {
			return DEFAULT;
		}

		List<Float> floats = data.floats();
		return new GnomeLook(Math.round(floats.get(0)), Math.round(floats.get(1)), Math.round(floats.get(2)));
	}

	// ---------------------------------------------------------------------
	// Recipe helpers: work out colors from the vanilla item ids
	// ---------------------------------------------------------------------

	public static int colorIndex(String name) {
		for (int i = 0; i < COLOR_NAMES.length; i++) {
			if (COLOR_NAMES[i].equals(name)) {
				return i;
			}
		}

		return -1;
	}

	private static @Nullable String vanillaPath(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

		if (id == null || !"minecraft".equals(id.getNamespace())) {
			return null;
		}

		return id.getPath();
	}

	/** Returns the wool color index of the stack, or -1 if it is not wool. */
	public static int woolColor(ItemStack stack) {
		String path = vanillaPath(stack);

		if (path == null || !path.endsWith("_wool")) {
			return -1;
		}

		return colorIndex(path.substring(0, path.length() - "_wool".length()));
	}

	/** Returns the terracotta value (0-32) of the stack, or -1 if it is not terracotta. */
	public static int terracottaValue(ItemStack stack) {
		String path = vanillaPath(stack);

		if (path == null) {
			return -1;
		}

		if (path.equals("terracotta")) {
			return 0;
		}

		if (path.endsWith("_glazed_terracotta")) {
			int color = colorIndex(path.substring(0, path.length() - "_glazed_terracotta".length()));
			return color < 0 ? -1 : 17 + color;
		}

		if (path.endsWith("_terracotta")) {
			int color = colorIndex(path.substring(0, path.length() - "_terracotta".length()));
			return color < 0 ? -1 : 1 + color;
		}

		return -1;
	}
}
