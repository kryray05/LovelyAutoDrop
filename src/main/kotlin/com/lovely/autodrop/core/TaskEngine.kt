package com.lovely.autodrop.core

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.util.Chat
import net.minecraft.client.MinecraftClient

/**
 * Runs at most one [Task] at a time and owns every global safety rule.
 *
 * Single-task-at-a-time is deliberate: two tasks clicking the same screen is
 * the classic way to get kicked for "invalid packet" on a picky SMP.
 */
object TaskEngine {

    @Volatile
    private var current: Task? = null

    private var lastHealth: Float = -1f
    private var ticksRun: Int = 0

    val isRunning: Boolean get() = current != null
    val activeName: String get() = current?.name ?: "idle"
    val activeStatus: String get() = current?.status ?: ""

    fun start(task: Task) {
        val mc = MinecraftClient.getInstance()
        val cfg = ModConfig.INSTANCE

        if (!cfg.masterEnabled) {
            Chat.warn("Mod is disabled (master switch off).")
            return
        }
        if (mc.player == null) {
            Chat.warn("Not in a world.")
            return
        }
        if (current != null) {
            Chat.warn("'${current!!.name}' is already running. Press the stop key first.")
            return
        }

        current = task
        ticksRun = 0
        lastHealth = mc.player?.health ?: -1f
        task.start(mc)
        Chat.info("Started ${task.name}.")
    }

    fun stop(reason: String = "stopped") {
        val task = current ?: return
        current = null
        val mc = MinecraftClient.getInstance()
        try {
            task.stop(mc, reason)
        } catch (e: Exception) {
            Chat.error("Error while stopping ${task.name}: ${e.message}")
        }
        Chat.info("${task.name} $reason.")
    }

    fun toggle(factory: () -> Task) {
        if (isRunning) stop("stopped") else start(factory())
    }

    /** Called every client tick from the mod entrypoint. */
    fun tick(mc: MinecraftClient) {
        val task = current ?: return
        val cfg = ModConfig.INSTANCE

        // --- global safety checks -------------------------------------------
        if (!cfg.masterEnabled) {
            stop("aborted (mod disabled)"); return
        }
        val player = mc.player
        if (player == null || mc.world == null) {
            stop("aborted (left world)"); return
        }
        if (cfg.stopOnDamage && lastHealth >= 0f && player.health < lastHealth - 0.01f) {
            stop("aborted (took damage)"); return
        }
        lastHealth = player.health

        ticksRun++
        if (cfg.taskTimeoutSeconds > 0 && ticksRun > cfg.taskTimeoutSeconds * 20) {
            stop("aborted (global timeout)"); return
        }

        // --- run --------------------------------------------------------------
        val keepGoing = try {
            task.tick(mc)
        } catch (e: Exception) {
            Chat.error("${task.name} crashed: ${e.message}")
            stop("aborted (error)")
            return
        }

        if (!keepGoing) stop("finished")
    }
}
