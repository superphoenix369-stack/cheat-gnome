package com.cheatgnome.gameplay;

import java.util.Locale;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import com.cheatgnome.CheatGnome;

/**
 * Sounds, particles, status effects and a few world changes, done through vanilla commands.
 * Commands keep working across Minecraft updates far better than the internal Java APIs do,
 * and the server's own command source can run them even when cheats are off.
 */
public final class Fx {
	private Fx() {
	}

	public static void command(MinecraftServer server, String command) {
		try {
			CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
			server.getCommands().performPrefixedCommand(source, command);
		} catch (Exception e) {
			CheatGnome.LOGGER.debug("Gnome command failed: {}", command, e);
		}
	}

	/** Runs a command in the given level at the given position. */
	public static void commandAt(ServerLevel level, double x, double y, double z, String command) {
		command(level.getServer(), String.format(Locale.ROOT, "execute in %s positioned %.3f %.3f %.3f run %s",
				level.dimension().identifier(), x, y, z, command));
	}

	public static void particles(ServerLevel level, String particle, double x, double y, double z, double spread, int count, double speed) {
		commandAt(level, x, y, z, String.format(Locale.ROOT, "particle %s ~ ~ ~ %.3f %.3f %.3f %.3f %d",
				particle, spread, spread, spread, speed, count));
	}

	public static void sound(ServerLevel level, String sound, double x, double y, double z, float volume, float pitch) {
		commandAt(level, x, y, z, String.format(Locale.ROOT, "playsound %s block @a[distance=..48] ~ ~ ~ %.2f %.2f",
				sound, volume, pitch));
	}

	public static void effect(ServerPlayer player, String effect, int seconds, int amplifier) {
		command(player.level().getServer(), String.format(Locale.ROOT, "effect give %s %s %d %d true",
				player.getUUID(), effect, seconds, amplifier));
	}

	/** A short line above the hotbar, in the gnome's voice. */
	public static void say(ServerPlayer player, Component message) {
		player.sendOverlayMessage(message);
	}

	/** A line in chat. */
	public static void tell(ServerPlayer player, Component message) {
		player.sendSystemMessage(message);
	}
}
