package com.cheatgnome.gameplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.cheatgnome.CheatGnome;
import com.cheatgnome.GnomeConfig;
import com.cheatgnome.block.DecoyGnomeBlock;
import com.cheatgnome.block.GnomeBlock;
import com.cheatgnome.block.GnomeBlockEntity;

/**
 * Harmless pranks. Nothing here breaks blocks, hurts the player or takes items away.
 */
public final class Mischief {
	private Mischief() {
	}

	private static final long TICKS_PER_DAY = 24000L;
	private static final int STARE_TICKS = 20 * 120;
	private static final int RAIN_CLOUD_TICKS = 20 * 30;

	/** Players currently followed by a personal rain cloud, and when it stops. */
	private static final Map<UUID, Long> RAIN_CLOUDS = new HashMap<>();

	public enum Prank {
		FAKE_EXPLOSION, WANDER, STARE, CHICKENS, RAIN_CLOUD, PUMPKIN_HEAD, DECOYS;

		boolean enabled(GnomeConfig config) {
			return switch (this) {
				case FAKE_EXPLOSION -> config.prankFakeExplosion;
				case WANDER -> config.prankWander;
				case STARE -> config.prankStare;
				case CHICKENS -> config.prankChickens;
				case RAIN_CLOUD -> config.prankRainCloud;
				case PUMPKIN_HEAD -> config.prankPumpkinHead;
				case DECOYS -> config.prankDecoys;
			};
		}
	}

	// ---------------------------------------------------------------------
	// Triggers
	// ---------------------------------------------------------------------

	/** Called every tick for each loaded gnome on the server. */
	public static void tickGnome(ServerLevel level, BlockPos pos, BlockState state, GnomeBlockEntity gnome) {
		long now = level.getGameTime();

		// Turn to face the nearest player while staring.
		if (gnome.isStaring(now) && now % 10 == 0) {
			Player target = nearestPlayer(level, pos, 24.0);

			if (target != null) {
				Direction facing = directionTowards(pos, target);

				if (state.getValue(GnomeBlock.FACING) != facing) {
					level.setBlock(pos, state.setValue(GnomeBlock.FACING, facing), Block.UPDATE_ALL);
				}
			}
		}

		// A neglected gnome occasionally pranks whoever wanders by.
		if (now % 200 == 0) {
			GnomeConfig config = GnomeConfig.get();

			if (config.mischief && isNeglected(gnome, now, config) && now - gnome.getLastPrankTick() > 20 * 90
					&& level.getRandom().nextFloat() < 0.25F) {
				Player target = nearestPlayer(level, pos, 10.0);

				if (target instanceof ServerPlayer player && !player.isSpectator()) {
					Fx.say(player, Component.translatable("gnome.cheatgnome.neglected").withStyle(ChatFormatting.DARK_PURPLE));
					randomPrank(level, player, gnome);
				}
			}
		}
	}

	private static boolean isNeglected(GnomeBlockEntity gnome, long now, GnomeConfig config) {
		return config.neglectDays > 0 && now - gnome.getLastVisitTick() > config.neglectDays * TICKS_PER_DAY;
	}

	/** When a player finally visits a neglected gnome, the gnome may get one prank in first. */
	public static void maybeNeglectPrank(ServerLevel level, ServerPlayer player, GnomeBlockEntity gnome, long now) {
		GnomeConfig config = GnomeConfig.get();

		if (config.mischief && isNeglected(gnome, now, config) && level.getRandom().nextFloat() < 0.5F) {
			Fx.tell(player, Component.translatable("gnome.cheatgnome.neglected").withStyle(ChatFormatting.DARK_PURPLE));
			randomPrank(level, player, gnome);
		}
	}

	public static void randomPrank(ServerLevel level, ServerPlayer player, GnomeBlockEntity gnome) {
		GnomeConfig config = GnomeConfig.get();
		List<Prank> options = new ArrayList<>();

		for (Prank prank : Prank.values()) {
			if (prank.enabled(config)) {
				options.add(prank);
			}
		}

		// No point putting a pumpkin on someone already wearing a helmet.
		if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			options.remove(Prank.PUMPKIN_HEAD);
		}

		if (options.isEmpty()) {
			return;
		}

		Prank prank = options.get(level.getRandom().nextInt(options.size()));
		gnome.setLastPrankTick(level.getGameTime());
		play(prank, level, player, gnome);
	}

	public static void play(Prank prank, ServerLevel level, ServerPlayer player, GnomeBlockEntity gnome) {
		BlockPos pos = gnome.getBlockPos();

		switch (prank) {
			case FAKE_EXPLOSION -> fakeExplosion(level, pos);
			case WANDER -> wander(level, player, gnome);
			case STARE -> stare(level, player, gnome);
			case CHICKENS -> chickens(level, player);
			case RAIN_CLOUD -> rainCloud(level, player);
			case PUMPKIN_HEAD -> pumpkinHead(level, player);
			case DECOYS -> decoys(level, player, pos);
		}
	}

	// ---------------------------------------------------------------------
	// The pranks
	// ---------------------------------------------------------------------

	/** A big bang and some smoke. No damage, no broken blocks. */
	private static void fakeExplosion(ServerLevel level, BlockPos pos) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		Fx.sound(level, "minecraft:entity.generic.explode", x, y, z, 1.0F, 1.1F);
		Fx.particles(level, "minecraft:explosion", x, y + 0.3, z, 0.6, 4, 0.0);
		Fx.particles(level, "minecraft:poof", x, y, z, 0.8, 30, 0.05);
		Fx.particles(level, "minecraft:totem_of_undying", x, y + 0.5, z, 0.4, 40, 0.4);
	}

	/** The gnome hops to a new spot nearby. Players have to go find it. */
	private static void wander(ServerLevel level, ServerPlayer player, GnomeBlockEntity gnome) {
		BlockPos from = gnome.getBlockPos();
		BlockState fromState = level.getBlockState(from);
		RandomSource random = level.getRandom();
		BlockPos target = null;

		for (int attempt = 0; attempt < 40 && target == null; attempt++) {
			BlockPos candidate = from.offset(random.nextInt(11) - 5, random.nextInt(5) - 2, random.nextInt(11) - 5);

			if (!candidate.equals(from) && canStandAt(level, candidate)) {
				target = candidate;
			}
		}

		if (target == null) {
			// Nowhere to go: settle for a bang instead.
			fakeExplosion(level, from);
			return;
		}

		BlockState newState = fromState.setValue(GnomeBlock.FACING, directionTowards(target, player));
		GnomeBlockEntity copy = new GnomeBlockEntity(from, fromState);
		copy.copyFrom(gnome);

		level.removeBlock(from, false);
		level.setBlock(target, newState, Block.UPDATE_ALL);

		if (level.getBlockEntity(target) instanceof GnomeBlockEntity moved) {
			moved.copyFrom(copy);
		}

		Fx.particles(level, "minecraft:poof", from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5, 0.3, 15, 0.02);
		Fx.particles(level, "minecraft:poof", target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 0.3, 15, 0.02);
		Fx.sound(level, "minecraft:entity.fox.teleport", target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 1.0F, 1.3F);
		Fx.say(player, Component.translatable("gnome.cheatgnome.prank.wander").withStyle(ChatFormatting.YELLOW));
	}

	/** For two minutes the gnome turns to face you wherever you go. */
	private static void stare(ServerLevel level, ServerPlayer player, GnomeBlockEntity gnome) {
		gnome.startStaring(level.getGameTime() + STARE_TICKS);
		Fx.sound(level, "minecraft:ambient.cave", player.getX(), player.getY(), player.getZ(), 0.6F, 1.0F);
		Fx.say(player, Component.translatable("gnome.cheatgnome.prank.stare").withStyle(ChatFormatting.DARK_GRAY));
	}

	/** A handful of chickens pop out around the player. */
	private static void chickens(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();

		for (int i = 0; i < 5; i++) {
			double angle = (Math.PI * 2 * i) / 5 + random.nextDouble() * 0.5;
			double x = player.getX() + Math.cos(angle) * 2.0;
			double z = player.getZ() + Math.sin(angle) * 2.0;
			double y = player.getY() + 0.5;
			Fx.commandAt(level, x, y, z, "summon minecraft:chicken ~ ~ ~");
			Fx.particles(level, "minecraft:poof", x, y, z, 0.2, 6, 0.02);
		}

		Fx.sound(level, "minecraft:entity.chicken.egg", player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F);
		Fx.say(player, Component.translatable("gnome.cheatgnome.prank.chickens").withStyle(ChatFormatting.YELLOW));
	}

	/** A little rain cloud follows the player around for 30 seconds. */
	private static void rainCloud(ServerLevel level, ServerPlayer player) {
		RAIN_CLOUDS.put(player.getUUID(), level.getGameTime() + RAIN_CLOUD_TICKS);
		Fx.sound(level, "minecraft:weather.rain.above", player.getX(), player.getY(), player.getZ(), 0.6F, 1.2F);
		Fx.say(player, Component.translatable("gnome.cheatgnome.prank.rain_cloud").withStyle(ChatFormatting.AQUA));
	}

	/** Pops a carved pumpkin on the player's head (only when the helmet slot is empty). */
	private static void pumpkinHead(ServerLevel level, ServerPlayer player) {
		if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			fakeExplosion(level, player.blockPosition());
			return;
		}

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
		Fx.sound(level, "minecraft:block.pumpkin.carve", player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F);
		Fx.say(player, Component.translatable("gnome.cheatgnome.prank.pumpkin").withStyle(ChatFormatting.GOLD));
	}

	/** Fake gnomes appear nearby and vanish when touched (or after a minute). */
	private static void decoys(ServerLevel level, ServerPlayer player, BlockPos around) {
		RandomSource random = level.getRandom();
		int placed = 0;

		for (int attempt = 0; attempt < 40 && placed < 3; attempt++) {
			BlockPos candidate = around.offset(random.nextInt(9) - 4, random.nextInt(3) - 1, random.nextInt(9) - 4);

			if (canStandAt(level, candidate)) {
				BlockState state = CheatGnome.DECOY_GNOME_BLOCK.defaultBlockState()
						.setValue(DecoyGnomeBlock.FACING, directionTowards(candidate, player));
				level.setBlock(candidate, state, Block.UPDATE_ALL);
				level.scheduleTick(candidate, CheatGnome.DECOY_GNOME_BLOCK, DecoyGnomeBlock.LIFETIME_TICKS + random.nextInt(200));
				Fx.particles(level, "minecraft:poof", candidate.getX() + 0.5, candidate.getY() + 0.5, candidate.getZ() + 0.5, 0.25, 10, 0.02);
				placed++;
			}
		}

		if (placed == 0) {
			fakeExplosion(level, around);
			return;
		}

		Fx.sound(level, "minecraft:entity.illusioner.mirror_move", around.getX() + 0.5, around.getY() + 0.5, around.getZ() + 0.5, 1.0F, 1.2F);
		Fx.say(player, Component.translatable("gnome.cheatgnome.prank.decoys").withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	// ---------------------------------------------------------------------
	// Rain clouds tick with the server
	// ---------------------------------------------------------------------

	public static void tickRainClouds(MinecraftServer server) {
		if (RAIN_CLOUDS.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<UUID, Long>> it = RAIN_CLOUDS.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<UUID, Long> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

			if (player == null) {
				it.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) player.level();
			long now = level.getGameTime();

			if (now > entry.getValue()) {
				it.remove();
				continue;
			}

			if (now % 4 == 0) {
				double x = player.getX();
				double y = player.getY() + player.getBbHeight() + 0.9;
				double z = player.getZ();
				Fx.commandAt(level, x, y, z, String.format(Locale.ROOT, "particle minecraft:cloud ~ ~ ~ %.2f %.2f %.2f 0 4", 0.45, 0.08, 0.45));
				Fx.commandAt(level, x, y - 0.2, z, String.format(Locale.ROOT, "particle minecraft:falling_water ~ ~ ~ %.2f %.2f %.2f 0 3", 0.35, 0.0, 0.35));
			}
		}
	}

	public static void clearRainClouds() {
		RAIN_CLOUDS.clear();
	}

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------

	private static boolean canStandAt(ServerLevel level, BlockPos pos) {
		BlockPos below = pos.below();
		return level.getBlockState(pos).isAir()
				&& level.getBlockState(pos.above()).isAir()
				&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}

	private static @Nullable Player nearestPlayer(ServerLevel level, BlockPos pos, double maxDistance) {
		Player best = null;
		double bestDistance = maxDistance * maxDistance;

		for (Player player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}

			double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

			if (distance < bestDistance) {
				bestDistance = distance;
				best = player;
			}
		}

		return best;
	}

	/** The horizontal direction pointing from {@code pos} towards the player. */
	private static Direction directionTowards(BlockPos pos, Player player) {
		double dx = player.getX() - (pos.getX() + 0.5);
		double dz = player.getZ() - (pos.getZ() + 0.5);

		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? Direction.EAST : Direction.WEST;
		}

		return dz > 0 ? Direction.SOUTH : Direction.NORTH;
	}
}
