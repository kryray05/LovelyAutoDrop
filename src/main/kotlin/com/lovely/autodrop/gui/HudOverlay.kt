package com.lovely.autodrop.gui

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.core.TaskEngine
import com.lovely.autodrop.feature.AutoSpawnerRoutine
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

/** Sleek, modern status card rendered on HUD while automation is active. */
object HudOverlay {

    fun render(ctx: DrawContext) {
        val cfg = ModConfig.INSTANCE
        if (!cfg.hudEnabled) return

        val taskRunning = TaskEngine.isRunning
        val autoLoopEnabled = AutoSpawnerRoutine.isEnabled

        if (!taskRunning && !autoLoopEnabled) return

        val mc = MinecraftClient.getInstance()
        if (mc.options.hudHidden) return

        val tr = mc.textRenderer

        val title = if (taskRunning) "LAD §d§l${TaskEngine.activeName}" else "LAD §a§lAuto Loop"
        val line1 = if (taskRunning) "§f${TaskEngine.activeStatus}" else "§7Next run: §a${AutoSpawnerRoutine.formattedRemainingTime}"
        val line2 = if (taskRunning && autoLoopEnabled) "§7Loop in: §a${AutoSpawnerRoutine.formattedRemainingTime}" else null

        val w1 = tr.getWidth(title) + 14
        val w2 = tr.getWidth(line1) + 8
        val w3 = if (line2 != null) tr.getWidth(line2) + 8 else 0
        val contentW = maxOf(w1, w2, w3, 120)

        val padding = 6
        val cardW = contentW + padding * 2
        val cardH = if (line2 != null) 36 else 26
        val x = cfg.hudX
        val y = cfg.hudY

        // Translucent dark card background
        ctx.fill(x, y, x + cardW, y + cardH, 0xD00F0F18.toInt())

        // Top accent line & borders
        val accentColor = if (taskRunning) 0xFFBB86FC.toInt() else 0xFF00E676.toInt()
        ctx.fill(x, y, x + cardW, y + 2, accentColor)
        ctx.fill(x, y + cardH - 1, x + cardW, y + cardH, 0x30FFFFFF.toInt())
        ctx.fill(x, y + 2, x + 1, y + cardH - 1, 0x30FFFFFF.toInt())
        ctx.fill(x + cardW - 1, y + 2, x + cardW, y + cardH - 1, 0x30FFFFFF.toInt())

        // Active indicator pulse dot
        val dotColor = if (TaskEngine.activeStatus.contains("PAUSED", ignoreCase = true)) 0xFFFFA000.toInt() else 0xFF00E676.toInt()
        ctx.fill(x + 6, y + 6, x + 10, y + 10, dotColor)

        // Draw text with fully opaque ARGB color (0xFFFFFFFF / 0xFFDDDDDD)
        ctx.drawTextWithShadow(tr, Text.literal(title), x + 14, y + 4, 0xFFFFFFFF.toInt())
        ctx.drawTextWithShadow(tr, Text.literal(line1), x + 6, y + 14, 0xFFDDDDDD.toInt())

        if (line2 != null) {
            ctx.drawTextWithShadow(tr, Text.literal(line2), x + 6, y + 24, 0xFFAAAAAA.toInt())
        }
    }
}
