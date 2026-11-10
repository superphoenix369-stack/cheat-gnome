package com.cheatgnome;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings for the gnome. Stored in config/cheatgnome.json and editable in-game
 * (from the cheat menu's Settings button, which needs cheats on).
 */
public final class GnomeConfig {
	/** AUTO: cheat menu when cheats are on, garden gnome when they're off. */
	public enum Mode {
		AUTO, CHEAT_MENU, GARDEN;

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static GnomeConfig instance = new GnomeConfig();

	// ----- General -----
	public Mode mode = Mode.AUTO;
	/** Gives each player one gnome the first time they join a world. */
	public boolean giveGnomeOnFirstJoin = false;
	/** Swaps the Echo Shard in the recipe for a Nether Star. */
	public boolean netherStarRecipe = false;

	// ----- Garden gnome -----
	public boolean dailyGift = true;
	/** Ticks between daily gifts. 24000 = one Minecraft day (20 real minutes). */
	public int giftCooldownTicks = 24000;
	public boolean offerings = true;

	// ----- Mischief -----
	public boolean mischief = false;
	/** Days without a visit before a lonely gnome starts pranking nearby players. 0 turns this off. */
	public int neglectDays = 3;
	public boolean prankFakeExplosion = true;
	public boolean prankWander = true;
	public boolean prankStare = true;
	public boolean prankChickens = true;
	public boolean prankRainCloud = true;
	public boolean prankPumpkinHead = true;
	public boolean prankDecoys = true;

	public static GnomeConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("cheatgnome.json");
	}

	public static void load() {
		Path path = path();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				GnomeConfig loaded = GSON.fromJson(reader, GnomeConfig.class);

				if (loaded != null) {
					if (loaded.mode == null) {
						loaded.mode = Mode.AUTO;
					}

					instance = loaded;
				}
			} catch (Exception e) {
				CheatGnome.LOGGER.warn("Could not read {}, using defaults", path, e);
			}
		}

		save();
	}

	public static void save() {
		try {
			Files.createDirectories(path().getParent());

			try (Writer writer = Files.newBufferedWriter(path())) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			CheatGnome.LOGGER.warn("Could not save cheat gnome config", e);
		}
	}

	// ---------------------------------------------------------------------
	// Toggles used by the in-game settings screen. Keys are shared with the client.
	// ---------------------------------------------------------------------

	public static final String[] TOGGLE_KEYS = {
			"dailyGift", "offerings", "mischief", "netherStarRecipe", "giveGnomeOnFirstJoin",
			"prankFakeExplosion", "prankWander", "prankStare", "prankChickens",
			"prankRainCloud", "prankPumpkinHead", "prankDecoys"
	};

	public Map<String, Boolean> toggles() {
		Map<String, Boolean> map = new LinkedHashMap<>();
		map.put("dailyGift", dailyGift);
		map.put("offerings", offerings);
		map.put("mischief", mischief);
		map.put("netherStarRecipe", netherStarRecipe);
		map.put("giveGnomeOnFirstJoin", giveGnomeOnFirstJoin);
		map.put("prankFakeExplosion", prankFakeExplosion);
		map.put("prankWander", prankWander);
		map.put("prankStare", prankStare);
		map.put("prankChickens", prankChickens);
		map.put("prankRainCloud", prankRainCloud);
		map.put("prankPumpkinHead", prankPumpkinHead);
		map.put("prankDecoys", prankDecoys);
		return map;
	}

	public boolean getToggle(String key) {
		Boolean value = toggles().get(key);
		return value != null && value;
	}

	/** Applies a setting by key. Returns false if the key or value is unknown. */
	public boolean set(String key, String value) {
		boolean on = Boolean.parseBoolean(value);

		switch (key) {
			case "mode" -> {
				try {
					mode = Mode.valueOf(value);
				} catch (IllegalArgumentException e) {
					return false;
				}
			}
			case "dailyGift" -> dailyGift = on;
			case "offerings" -> offerings = on;
			case "mischief" -> mischief = on;
			case "netherStarRecipe" -> netherStarRecipe = on;
			case "giveGnomeOnFirstJoin" -> giveGnomeOnFirstJoin = on;
			case "prankFakeExplosion" -> prankFakeExplosion = on;
			case "prankWander" -> prankWander = on;
			case "prankStare" -> prankStare = on;
			case "prankChickens" -> prankChickens = on;
			case "prankRainCloud" -> prankRainCloud = on;
			case "prankPumpkinHead" -> prankPumpkinHead = on;
			case "prankDecoys" -> prankDecoys = on;
			default -> {
				return false;
			}
		}

		return true;
	}
}
