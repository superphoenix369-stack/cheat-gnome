package com.cheatgnome.gameplay;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;

import com.cheatgnome.GnomeConfig;
import com.cheatgnome.block.GnomeBlockEntity;
import com.cheatgnome.net.OpenGnomeMenuPayload;

/** What happens when a player right-clicks or punches the gnome. */
public final class GnomeInteractions {
	private GnomeInteractions() {
	}

	/** True when the player has cheat (gamemaster) permissions, like "Allow Cheats" in a single-player world. */
	public static boolean hasCheats(ServerPlayer player) {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** Whether this gnome should act as the cheat menu for this player. */
	public static boolean usesCheatMenu(ServerPlayer player) {
		return switch (GnomeConfig.get().mode) {
			case CHEAT_MENU -> true;
			case GARDEN -> false;
			case AUTO -> hasCheats(player);
		};
	}

	// ---------------------------------------------------------------------
	// Right-click with an empty hand
	// ---------------------------------------------------------------------

	public static void onEmptyHandUse(ServerPlayer player, GnomeBlockEntity gnome) {
		ServerLevel level = (ServerLevel) gnome.getLevel();
		long now = level.getGameTime();
		BlockPos pos = gnome.getBlockPos();

		if (usesCheatMenu(player)) {
			gnome.markVisited(now);
			openMenu(player, pos);
			return;
		}

		GnomeConfig config = GnomeConfig.get();

		if (!config.dailyGift) {
			gnome.markVisited(now);
			Fx.say(player, Component.translatable(config.offerings ? "gnome.cheatgnome.wants_offering" : "gnome.cheatgnome.smiles"));
			Fx.particles(level, "minecraft:happy_villager", pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0.3, 5, 0.0);
			return;
		}

		// A gnome left alone too long may get its prank in before handing over a gift.
		Mischief.maybeNeglectPrank(level, player, gnome, now);
		gnome.markVisited(now);

		long readyAt = gnome.getLastGiftTick() + config.giftCooldownTicks;

		if (now >= readyAt) {
			ItemStack gift = Rewards.dailyGift(level.getRandom());
			Component giftName = describe(gift);
			give(player, gift);
			gnome.setLastGiftTick(now);
			Fx.tell(player, Component.translatable("gnome.cheatgnome.gift", giftName).withStyle(ChatFormatting.GREEN));
			celebrate(level, pos);
		} else {
			long ticksLeft = readyAt - now;
			long minutes = Math.max(1, (ticksLeft + 1199) / 1200);
			Fx.say(player, Component.translatable("gnome.cheatgnome.resting", minutes));
			Fx.particles(level, "minecraft:note", pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0.2, 1, 0.0);
		}
	}

	public static void openMenu(ServerPlayer player, BlockPos pos) {
		GnomeConfig config = GnomeConfig.get();
		int bits = 0;

		for (int i = 0; i < GnomeConfig.TOGGLE_KEYS.length; i++) {
			if (config.getToggle(GnomeConfig.TOGGLE_KEYS[i])) {
				bits |= 1 << i;
			}
		}

		ServerPlayNetworking.send(player, new OpenGnomeMenuPayload(pos, hasCheats(player), config.mode.name(), bits));
	}

	// ---------------------------------------------------------------------
	// Right-click holding something: offerings
	// ---------------------------------------------------------------------

	/** Returns true if the item was used as an offering. */
	public static boolean onItemUse(ServerPlayer player, GnomeBlockEntity gnome, ItemStack stack) {
		GnomeConfig config = GnomeConfig.get();
		ServerLevel level = (ServerLevel) gnome.getLevel();
		long now = level.getGameTime();
		BlockPos pos = gnome.getBlockPos();

		if (!config.offerings) {
			// No offerings: an item in hand acts like an empty hand.
			onEmptyHandUse(player, gnome);
			return true;
		}

		Offering offering = Offering.of(stack);

		if (offering == Offering.NONE) {
			if (usesCheatMenu(player)) {
				gnome.markVisited(now);
				openMenu(player, pos);
			} else {
				Fx.say(player, Component.translatable("gnome.cheatgnome.not_interested"));
			}

			return true;
		}

		gnome.markVisited(now);
		RandomSource random = level.getRandom();
		ItemStack offered = stack.copyWithCount(1);

		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}

		switch (offering) {
			case FLOWER -> {
				ItemStack gift = Rewards.natureGift(random);
				Component giftName = describe(gift);
				give(player, gift);
				Fx.tell(player, Component.translatable("gnome.cheatgnome.offering.flower", offered.getHoverName(), giftName).withStyle(ChatFormatting.GREEN));
				Fx.particles(level, "minecraft:heart", pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.3, 3, 0.0);
				Fx.sound(level, "minecraft:block.azalea_leaves.place", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0F, 1.2F);
			}
			case SHINY -> {
				int rolls = offered.is(Items.GOLD_BLOCK) ? 9 : 1;

				for (int i = 0; i < rolls; i++) {
					give(player, Rewards.shinyGift(random));
				}

				Fx.tell(player, Component.translatable(rolls > 1 ? "gnome.cheatgnome.offering.shiny_lots" : "gnome.cheatgnome.offering.shiny",
						offered.getHoverName()).withStyle(ChatFormatting.GOLD));
				celebrate(level, pos);
			}
			case TREAT -> {
				String[] effect = Rewards.TREAT_EFFECTS[random.nextInt(Rewards.TREAT_EFFECTS.length)];
				Fx.effect(player, effect[0], Integer.parseInt(effect[1]), 0);
				Fx.command(level.getServer(), "experience add " + player.getUUID() + " 5 points");
				Fx.tell(player, Component.translatable("gnome.cheatgnome.offering.treat", offered.getHoverName(),
						Component.translatable("effect." + effect[0].replace(':', '.'))).withStyle(ChatFormatting.LIGHT_PURPLE));
				Fx.particles(level, "minecraft:heart", pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.3, 4, 0.0);
				Fx.sound(level, "minecraft:entity.generic.eat", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0F, 1.3F);
			}
			case INSULT -> {
				Fx.say(player, Component.translatable("gnome.cheatgnome.offering.insult").withStyle(ChatFormatting.RED));
				Fx.particles(level, "minecraft:angry_villager", pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.3, 3, 0.0);
				Fx.sound(level, "minecraft:entity.villager.no", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0F, 1.5F);

				if (config.mischief) {
					Mischief.randomPrank(level, player, gnome);
				}
			}
			default -> {
			}
		}

		return true;
	}

	// ---------------------------------------------------------------------
	// Left-click
	// ---------------------------------------------------------------------

	public static void onPunched(ServerPlayer player, GnomeBlockEntity gnome) {
		if (player.isShiftKeyDown() || player.getAbilities().instabuild) {
			return;
		}

		ServerLevel level = (ServerLevel) gnome.getLevel();
		long now = level.getGameTime();

		if (GnomeConfig.get().mischief && now - gnome.getLastPrankTick() > 100 && level.getRandom().nextFloat() < 0.5F) {
			Fx.say(player, Component.translatable("gnome.cheatgnome.punched_prank").withStyle(ChatFormatting.RED));
			Mischief.randomPrank(level, player, gnome);
		} else {
			Fx.say(player, Component.translatable("gnome.cheatgnome.punched"));
			Fx.sound(level, "minecraft:block.decorated_pot.hit", gnome.getBlockPos().getX() + 0.5, gnome.getBlockPos().getY() + 0.5, gnome.getBlockPos().getZ() + 0.5, 1.0F, 1.4F);
		}
	}

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------

	public static Component describe(ItemStack stack) {
		return Component.translatable("gnome.cheatgnome.count", stack.getCount(), stack.getHoverName());
	}

	/** Puts the stack in the player's inventory, dropping whatever doesn't fit at their feet. */
	public static void give(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		player.getInventory().add(stack);

		if (!stack.isEmpty()) {
			Block.popResource(player.level(), player.blockPosition(), stack);
		}
	}

	public static void celebrate(ServerLevel level, BlockPos pos) {
		Fx.particles(level, "minecraft:happy_villager", pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.35, 10, 0.0);
		Fx.sound(level, "minecraft:block.amethyst_block.chime", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0F, 1.2F);
		Fx.sound(level, "minecraft:entity.experience_orb.pickup", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.6F, 1.0F);
	}

	/** Kinds of things the gnome accepts. */
	public enum Offering {
		NONE, FLOWER, SHINY, TREAT, INSULT;

		public static Offering of(ItemStack stack) {
			if (stack.is(ConventionalItemTags.FLOWERS)) {
				return FLOWER;
			}

			if (stack.is(Items.GOLD_INGOT) || stack.is(Items.RAW_GOLD) || stack.is(Items.GOLD_BLOCK)
					|| stack.is(Items.EMERALD) || stack.is(Items.DIAMOND) || stack.is(Items.AMETHYST_SHARD)) {
				return SHINY;
			}

			if (stack.is(Items.COOKIE) || stack.is(Items.CAKE) || stack.is(Items.PUMPKIN_PIE)
					|| stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES) || stack.is(Items.HONEY_BOTTLE)
					|| stack.is(Items.GOLDEN_CARROT) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.APPLE)
					|| stack.is(Items.BREAD) || stack.is(Items.MUSHROOM_STEW)) {
				return TREAT;
			}

			if (stack.is(Items.ROTTEN_FLESH) || stack.is(Items.POISONOUS_POTATO) || stack.is(Items.SPIDER_EYE)
					|| stack.is(Items.BONE) || stack.is(Items.DIRT)) {
				return INSULT;
			}

			return NONE;
		}
	}
}
