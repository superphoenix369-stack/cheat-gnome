package com.cheatgnome.gameplay;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The gnome's gift tables. Each roll picks one weighted entry. */
public final class Rewards {
	private Rewards() {
	}

	private record Entry(Item item, int min, int max, int weight) {
		ItemStack roll(RandomSource random) {
			int count = min + (max > min ? random.nextInt(max - min + 1) : 0);
			return new ItemStack(item, count);
		}
	}

	private static final class Table {
		private final List<Entry> entries = new ArrayList<>();
		private int totalWeight;

		Table add(Item item, int min, int max, int weight) {
			entries.add(new Entry(item, min, max, weight));
			totalWeight += weight;
			return this;
		}

		ItemStack roll(RandomSource random) {
			int pick = random.nextInt(totalWeight);

			for (Entry entry : entries) {
				pick -= entry.weight();

				if (pick < 0) {
					return entry.roll(random);
				}
			}

			return entries.get(0).roll(random);
		}
	}

	/** The once-a-day gift: mostly handy, sometimes great. */
	private static final Table DAILY = new Table()
			.add(Items.EMERALD, 1, 3, 20)
			.add(Items.BREAD, 2, 4, 10)
			.add(Items.COOKIE, 3, 6, 10)
			.add(Items.BONE_MEAL, 4, 8, 12)
			.add(Items.IRON_INGOT, 2, 4, 10)
			.add(Items.GOLD_NUGGET, 6, 12, 8)
			.add(Items.LAPIS_LAZULI, 4, 8, 8)
			.add(Items.EXPERIENCE_BOTTLE, 2, 4, 8)
			.add(Items.GOLDEN_CARROT, 2, 3, 6)
			.add(Items.OAK_SAPLING, 1, 2, 3)
			.add(Items.CHERRY_SAPLING, 1, 2, 3)
			.add(Items.ENDER_PEARL, 1, 2, 4)
			.add(Items.NAME_TAG, 1, 1, 3)
			.add(Items.AMETHYST_SHARD, 2, 4, 4)
			.add(Items.DIAMOND, 1, 1, 2)
			.add(Items.GOLDEN_APPLE, 1, 1, 2);

	/** Flowers buy garden goodies. */
	private static final Table NATURE = new Table()
			.add(Items.BONE_MEAL, 1, 2, 30)
			.add(Items.WHEAT_SEEDS, 2, 4, 12)
			.add(Items.PUMPKIN_SEEDS, 1, 2, 8)
			.add(Items.MELON_SEEDS, 1, 2, 8)
			.add(Items.BEETROOT_SEEDS, 2, 3, 8)
			.add(Items.SWEET_BERRIES, 2, 4, 8)
			.add(Items.GLOW_BERRIES, 1, 3, 6)
			.add(Items.HONEYCOMB, 1, 2, 6)
			.add(Items.MOSS_BLOCK, 2, 4, 6)
			.add(Items.OAK_SAPLING, 1, 1, 4)
			.add(Items.BIRCH_SAPLING, 1, 1, 4)
			.add(Items.CHERRY_SAPLING, 1, 1, 3)
			.add(Items.TORCHFLOWER_SEEDS, 1, 1, 2)
			.add(Items.PITCHER_POD, 1, 1, 2);

	/** Gold and gems buy shiny things. */
	private static final Table SHINY = new Table()
			.add(Items.EMERALD, 1, 1, 30)
			.add(Items.LAPIS_LAZULI, 3, 6, 15)
			.add(Items.REDSTONE, 4, 8, 15)
			.add(Items.QUARTZ, 2, 4, 12)
			.add(Items.IRON_INGOT, 1, 2, 12)
			.add(Items.COPPER_INGOT, 3, 6, 8)
			.add(Items.AMETHYST_SHARD, 2, 3, 6)
			.add(Items.DIAMOND, 1, 1, 2);

	public static ItemStack dailyGift(RandomSource random) {
		return DAILY.roll(random);
	}

	public static ItemStack natureGift(RandomSource random) {
		return NATURE.roll(random);
	}

	public static ItemStack shinyGift(RandomSource random) {
		return SHINY.roll(random);
	}

	/** Status effects handed out for treats: id, seconds. */
	public static final String[][] TREAT_EFFECTS = {
			{"minecraft:speed", "180"},
			{"minecraft:haste", "180"},
			{"minecraft:regeneration", "30"},
			{"minecraft:jump_boost", "180"},
			{"minecraft:night_vision", "240"},
			{"minecraft:luck", "300"},
			{"minecraft:absorption", "120"}
	};
}
