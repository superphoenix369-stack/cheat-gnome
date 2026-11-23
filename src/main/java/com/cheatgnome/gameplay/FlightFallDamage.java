package com.cheatgnome.gameplay;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import com.cheatgnome.CheatGnome;

/**
 * Vanilla skips fall damage for anyone who is allowed to fly. That makes sense in Creative, but the gnome's
 * flight cheat is meant for survival, so this puts fall damage back for those players whenever they are
 * not actually flying.
 *
 * <p>It follows each player's highest point since they last stood on something and, when they land, applies
 * the fall damage vanilla would have (3 safe blocks, then 1 damage per block). Feather Falling and armor
 * still apply, and water, ladders, vines, Slow Falling and elytra gliding all reset the fall as usual.
 */
public final class FlightFallDamage {
	private FlightFallDamage() {
	}

	private static final double SAFE_FALL = 3.0;

	/** Largest believable drop in one tick; anything bigger is a teleport or respawn, not a fall. */
	private static final double MAX_STEP = 6.0;

	/** Highest Y since the player last touched the ground, plus where they were last tick. */
	private record Track(double peak, double lastY, ServerLevel level) {
	}

	private static final Map<UUID, Track> TRACKS = new HashMap<>();

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();

			if (!isTracked(player)) {
				TRACKS.remove(id);
				continue;
			}

			ServerLevel level = (ServerLevel) player.level();
			double y = player.getY();
			Track track = TRACKS.get(id);

			// New, teleported, respawned or changed dimension: start fresh from here.
			if (track == null || track.level() != level || Math.abs(y - track.lastY()) > MAX_STEP) {
				TRACKS.put(id, new Track(y, y, level));
				continue;
			}

			boolean resetsFall = player.getAbilities().flying
					|| player.isInWater()
					|| player.onClimbable()
					|| player.isFallFlying()
					|| player.isPassenger()
					|| player.hasEffect(MobEffects.SLOW_FALLING);

			if (resetsFall) {
				TRACKS.put(id, new Track(y, y, level));
				continue;
			}

			if (player.onGround()) {
				double fall = track.peak() - y;

				if (fall > SAFE_FALL) {
					float damage = (float) Math.ceil(fall - SAFE_FALL) * landingMultiplier(player);

					if (damage > 0) {
						Fx.command(server, String.format(Locale.ROOT, "damage %s %.1f minecraft:fall", player.getScoreboardName(), damage));
					}
				}

				TRACKS.put(id, new Track(y, y, level));
			} else {
				TRACKS.put(id, new Track(Math.max(track.peak(), y), y, level));
			}
		}
	}

	/** Only players who got flight from the gnome, are allowed to fly, and aren't in Creative or Spectator. */
	private static boolean isTracked(ServerPlayer player) {
		return Boolean.TRUE.equals(player.getAttached(CheatGnome.GNOME_FLIGHT))
				&& player.getAbilities().mayfly
				&& !player.getAbilities().instabuild
				&& !player.isSpectator();
	}

	/** Soft landings, like vanilla: slime and powder snow cancel the damage, hay and honey cut it, beds halve it. */
	private static float landingMultiplier(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos feet = player.blockPosition();
		float multiplier = 1.0F;

		for (BlockPos pos : new BlockPos[] {feet, feet.below()}) {
			String block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();

			switch (block) {
				case "slime_block", "powder_snow" -> multiplier = Math.min(multiplier, 0.0F);
				case "hay_block", "honey_block" -> multiplier = Math.min(multiplier, 0.2F);
				default -> {
					if (block.endsWith("_bed")) {
						multiplier = Math.min(multiplier, 0.5F);
					}
				}
			}
		}

		return multiplier;
	}

	public static void clear() {
		TRACKS.clear();
	}
}
