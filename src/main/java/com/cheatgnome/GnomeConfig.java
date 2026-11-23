package com.cheatgnome;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings for the gnomes. Stored in config/cheatgnome.json and editable in-game from a Cheat Gnome's
 * Settings button, or by sneak + right-clicking any gnome with cheats on.
 */
public final class GnomeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static GnomeConfig instance = new GnomeConfig();

	// ----- General -----
	/** Gives each player one Garden Gnome the first time they join a world. */
	public boolean giveGnomeOnFirstJoin = false;

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
	public boolean prankCreeperHiss = true;
	public boolean prankShrink = true;
	public boolean prankHiccups = true;
	public boolean prankSpotlight = true;
	public boolean prankSerenade = true;
	public boolean prankSwitcheroo = true;

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
	// On/off settings used by the in-game settings screen. Keys are shared with the client.
	// ---------------------------------------------------------------------

	public static final String[] TOGGLE_KEYS = {
			"dailyGift", "offerings", "giveGnomeOnFirstJoin", "mischief",
			"prankFakeExplosion", "prankWander", "prankStare", "prankChickens", "prankRainCloud",
			"prankPumpkinHead", "prankDecoys", "prankCreeperHiss", "prankShrink", "prankHiccups",
			"prankSpotlight", "prankSerenade", "prankSwitcheroo"
	};

	public boolean getToggle(String key) {
		return switch (key) {
			case "dailyGift" -> dailyGift;
			case "offerings" -> offerings;
			case "giveGnomeOnFirstJoin" -> giveGnomeOnFirstJoin;
			case "mischief" -> mischief;
			case "prankFakeExplosion" -> prankFakeExplosion;
			case "prankWander" -> prankWander;
			case "prankStare" -> prankStare;
			case "prankChickens" -> prankChickens;
			case "prankRainCloud" -> prankRainCloud;
			case "prankPumpkinHead" -> prankPumpkinHead;
			case "prankDecoys" -> prankDecoys;
			case "prankCreeperHiss" -> prankCreeperHiss;
			case "prankShrink" -> prankShrink;
			case "prankHiccups" -> prankHiccups;
			case "prankSpotlight" -> prankSpotlight;
			case "prankSerenade" -> prankSerenade;
			case "prankSwitcheroo" -> prankSwitcheroo;
			default -> false;
		};
	}

	/** Applies an on/off setting by key. Returns false if the key is unknown. */
	public boolean set(String key, String value) {
		boolean on = Boolean.parseBoolean(value);

		switch (key) {
			case "dailyGift" -> dailyGift = on;
			case "offerings" -> offerings = on;
			case "giveGnomeOnFirstJoin" -> giveGnomeOnFirstJoin = on;
			case "mischief" -> mischief = on;
			case "prankFakeExplosion" -> prankFakeExplosion = on;
			case "prankWander" -> prankWander = on;
			case "prankStare" -> prankStare = on;
			case "prankChickens" -> prankChickens = on;
			case "prankRainCloud" -> prankRainCloud = on;
			case "prankPumpkinHead" -> prankPumpkinHead = on;
			case "prankDecoys" -> prankDecoys = on;
			case "prankCreeperHiss" -> prankCreeperHiss = on;
			case "prankShrink" -> prankShrink = on;
			case "prankHiccups" -> prankHiccups = on;
			case "prankSpotlight" -> prankSpotlight = on;
			case "prankSerenade" -> prankSerenade = on;
			case "prankSwitcheroo" -> prankSwitcheroo = on;
			default -> {
				return false;
			}
		}

		return true;
	}
}
