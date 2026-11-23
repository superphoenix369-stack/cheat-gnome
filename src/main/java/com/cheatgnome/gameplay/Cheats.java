package com.cheatgnome.gameplay;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.cheatgnome.CheatGnome;
import com.cheatgnome.block.GnomeBlockEntity;

/**
 * The cheat menu. Ids are sent by the client's buttons; names are a nod to The Sims.
 */
public final class Cheats {
	private Cheats() {
	}

	/** Cheat ids in menu order. The client uses this list to build its buttons. */
	public static final String[] IDS = {
			"motherlode", "superpowers", "shoo_monsters",
			"make_all_happy", "free_real_estate", "skill_boost",
			"day", "night", "clear_skies",
			"rain", "fly", "fix_everything",
			"instant_harvest", "gnome_gift", "settings"
	};

	public static void run(String cheat, ServerPlayer player, ServerLevel level, BlockPos gnomePos) {
		MinecraftServer server = level.getServer();
		String name = player.getScoreboardName();

		switch (cheat) {
			case "motherlode" -> money(player, level, gnomePos, 64);
			case "superpowers" -> {
				Fx.effect(player, "minecraft:night_vision", 600, 0);
				Fx.effect(player, "minecraft:water_breathing", 600, 0);
				Fx.effect(player, "minecraft:fire_resistance", 600, 0);
			}
			case "shoo_monsters" -> {
				int shooed = shooMonsters(level, player, 32);
				Fx.say(player, Component.translatable("cheat.cheatgnome.shoo_monsters.done", shooed));
			}
			case "make_all_happy" -> {
				Fx.command(server, "effect clear " + name);
				Fx.effect(player, "minecraft:instant_health", 1, 9);
				Fx.effect(player, "minecraft:saturation", 1, 19);
				player.clearFire();
				player.setAirSupply(player.getMaxAirSupply());
			}
			case "free_real_estate" -> {
				GnomeInteractions.give(player, new ItemStack(Items.OAK_PLANKS, 64));
				GnomeInteractions.give(player, new ItemStack(Items.STONE_BRICKS, 64));
				GnomeInteractions.give(player, new ItemStack(Items.GLASS, 32));
				GnomeInteractions.give(player, new ItemStack(Items.TORCH, 32));
			}
			case "skill_boost" -> player.giveExperienceLevels(30);
			case "day" -> Fx.command(server, "time set day");
			case "night" -> Fx.command(server, "time set night");
			case "clear_skies" -> Fx.command(server, "weather clear");
			case "rain" -> Fx.command(server, "weather rain");
			case "fly" -> {
				boolean canFly = !player.getAbilities().mayfly;
				player.getAbilities().mayfly = canFly;

				if (!canFly) {
					player.getAbilities().flying = false;
				}

				player.onUpdateAbilities();
				player.setAttached(CheatGnome.GNOME_FLIGHT, canFly);
				Fx.say(player, Component.translatable(canFly ? "cheat.cheatgnome.fly.on" : "cheat.cheatgnome.fly.off"));
			}
			case "fix_everything" -> {
				int fixed = repairAll(player.getInventory());
				Fx.say(player, Component.translatable("cheat.cheatgnome.fix_everything.done", fixed));
			}
			case "instant_harvest" -> {
				int grown = growCrops(level, player.blockPosition(), 8);
				Fx.say(player, Component.translatable("cheat.cheatgnome.instant_harvest.done", grown));
			}
			case "gnome_gift" -> {
				ItemStack gift = Rewards.dailyGift(level.getRandom());
				Component name = GnomeInteractions.describe(gift);
				GnomeInteractions.give(player, gift);
				Fx.tell(player, Component.translatable("gnome.cheatgnome.gift", name).withStyle(ChatFormatting.GREEN));
			}
			default -> {
				return;
			}
		}

		if (level.getBlockEntity(gnomePos) instanceof GnomeBlockEntity gnome) {
			gnome.markVisited(level.getGameTime());
		}

		Fx.particles(level, "minecraft:enchant", gnomePos.getX() + 0.5, gnomePos.getY() + 1.0, gnomePos.getZ() + 0.5, 0.4, 20, 0.5);
		Fx.sound(level, "minecraft:block.amethyst_block.chime", gnomePos.getX() + 0.5, gnomePos.getY() + 0.5, gnomePos.getZ() + 0.5, 1.0F, 1.5F);
		Fx.tell(player, Component.translatable("cheat.cheatgnome.used", Component.translatable("cheat.cheatgnome." + cheat))
				.withStyle(ChatFormatting.GRAY)
				.withStyle(style -> style.withItalic(true)));
	}

	private static void money(ServerPlayer player, ServerLevel level, BlockPos gnomePos, int emeralds) {
		GnomeInteractions.give(player, new ItemStack(Items.EMERALD, emeralds));
		Fx.sound(level, "minecraft:entity.experience_orb.pickup", gnomePos.getX() + 0.5, gnomePos.getY() + 0.5, gnomePos.getZ() + 0.5, 1.0F, 0.8F);
	}

	/** Removes hostile mobs near the player, without drops. Named mobs and bosses are left alone. */
	private static int shooMonsters(ServerLevel level, ServerPlayer player, double radius) {
		AABB area = player.getBoundingBox().inflate(radius);
		List<Mob> monsters = level.getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy && mob.isAlive() && !mob.hasCustomName());
		int shooed = 0;

		for (Mob mob : monsters) {
			String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();

			if (type.equals("wither") || type.equals("ender_dragon") || type.equals("warden") || type.equals("elder_guardian")) {
				continue;
			}

			Fx.particles(level, "minecraft:poof", mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 0.3, 12, 0.02);
			mob.discard();
			shooed++;
		}

		return shooed;
	}

	private static int repairAll(Inventory inventory) {
		int fixed = 0;

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);

			if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > 0) {
				stack.setDamageValue(0);
				fixed++;
			}
		}

		return fixed;
	}

	/** Grows every crop around the player to full size. */
	private static int growCrops(ServerLevel level, BlockPos center, int radius) {
		int grown = 0;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
			BlockState state = level.getBlockState(pos);

			if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
				level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 2);
				Fx.particles(level, "minecraft:happy_villager", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.3, 4, 0.0);
				grown++;
			}
		}

		return grown;
	}
}
