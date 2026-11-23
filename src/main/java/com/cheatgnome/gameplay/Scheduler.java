package com.cheatgnome.gameplay;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;

import com.cheatgnome.CheatGnome;

/** Runs small jobs a few ticks from now (prank timers, the gnome's serenade). Cleared when the world closes. */
public final class Scheduler {
	private Scheduler() {
	}

	private record Task(long due, Runnable job) {
	}

	private static final List<Task> TASKS = new ArrayList<>();
	private static long tick;

	public static void later(int delayTicks, Runnable job) {
		TASKS.add(new Task(tick + Math.max(1, delayTicks), job));
	}

	public static void tick(MinecraftServer server) {
		tick++;

		if (TASKS.isEmpty()) {
			return;
		}

		List<Task> due = new ArrayList<>();
		TASKS.removeIf(task -> {
			if (task.due() <= tick) {
				due.add(task);
				return true;
			}

			return false;
		});

		for (Task task : due) {
			try {
				task.job().run();
			} catch (Exception e) {
				CheatGnome.LOGGER.warn("A gnome prank tripped over its own feet", e);
			}
		}
	}

	public static void clear() {
		TASKS.clear();
	}
}
