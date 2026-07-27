package com.lovely.autodrop.feature

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.core.TaskEngine
import com.lovely.autodrop.util.Chat
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import kotlin.random.Random

/**
 * Handles periodic automated spawner right-clicking and drop/sell routine.
 *
 * Configured in [ModConfig]:
 * - autoSpawnerEnabled (Boolean)
 * - autoSpawnerMinMinutes (Int)
 * - autoSpawnerMaxMinutes (Int)
 * - autoSpawnerInteractBeforeLoot (Boolean)
 */
object AutoSpawnerRoutine {

    private var ticksUntilNextRun: Int = -1
    private var isWaitingForSettle: Boolean = false
    private var settleCooldown: Int = 0

    val isEnabled: Boolean
        get() = ModConfig.INSTANCE.autoSpawnerEnabled

    val remainingSeconds: Int
        get() = if (ticksUntilNextRun > 0) ticksUntilNextRun / 20 else 0

    val formattedRemainingTime: String
        get() {
            val totalSec = remainingSeconds
            if (totalSec <= 0) return "Ready"
            val mins = totalSec / 60
            val secs = totalSec % 60
            return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }

    fun toggle() {
        val cfg = ModConfig.INSTANCE
        cfg.autoSpawnerEnabled = !cfg.autoSpawnerEnabled
        cfg.save()
        if (cfg.autoSpawnerEnabled) {
            resetTimer()
            Chat.success("Auto Spawner Loop ENABLED. Next run in $formattedRemainingTime.")
        } else {
            ticksUntilNextRun = -1
            isWaitingForSettle = false
            Chat.info("Auto Spawner Loop DISABLED.")
        }
    }

    fun resetTimer() {
        val cfg = ModConfig.INSTANCE
        val minMins = cfg.autoSpawnerMinMinutes.coerceAtLeast(1)
        val maxMins = cfg.autoSpawnerMaxMinutes.coerceAtLeast(minMins)
        val chosenMins = if (minMins == maxMins) minMins else Random.nextInt(minMins, maxMins + 1)
        ticksUntilNextRun = chosenMins * 60 * 20
    }

    fun tick(mc: MinecraftClient) {
        val cfg = ModConfig.INSTANCE
        if (!cfg.masterEnabled || !cfg.autoSpawnerEnabled) {
            if (ticksUntilNextRun != -1) ticksUntilNextRun = -1
            return
        }

        if (mc.player == null || mc.world == null) return

        if (ticksUntilNextRun < 0) {
            resetTimer()
        }

        if (isWaitingForSettle) {
            if (settleCooldown > 0) {
                settleCooldown--
                return
            }
            isWaitingForSettle = false
            // Start spawner loot task
            if (!TaskEngine.isRunning) {
                TaskEngine.start(SpawnerLootTask())
                resetTimer()
            }
            return
        }

        if (ticksUntilNextRun > 0) {
            ticksUntilNextRun--
            return
        }

        // Timer reached 0! Trigger routine
        if (TaskEngine.isRunning) {
            // Task already running, retry in 5s
            ticksUntilNextRun = 100
            return
        }

        Chat.info("§a[Auto Spawner] Timer triggered! Right-clicking spawner & starting loot/sell task...")

        if (cfg.autoSpawnerInteractBeforeLoot) {
            rightClickSpawner(mc)
            isWaitingForSettle = true
            settleCooldown = (cfg.settleTicks + 10).coerceAtLeast(15) // wait for GUI to open
        } else {
            TaskEngine.start(SpawnerLootTask())
            resetTimer()
        }
    }

    private fun rightClickSpawner(mc: MinecraftClient) {
        val player = mc.player ?: return
        val im = mc.interactionManager ?: return
        val hitResult = mc.crosshairTarget

        if (hitResult != null && hitResult.type == HitResult.Type.BLOCK && hitResult is BlockHitResult) {
            im.interactBlock(player, Hand.MAIN_HAND, hitResult)
            player.swingHand(Hand.MAIN_HAND)
            val pos = hitResult.blockPos
            Chat.info("§a[Auto Spawner] Right-clicked block at (${pos.x}, ${pos.y}, ${pos.z}).")
        } else {
            im.interactItem(player, Hand.MAIN_HAND)
            player.swingHand(Hand.MAIN_HAND)
            Chat.warn("§e[Auto Spawner] No block targeted directly. Sent right-click interaction.")
        }
    }
}
