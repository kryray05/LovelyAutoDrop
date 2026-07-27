package com.lovely.autodrop.gui

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.core.TaskEngine
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

/** Sleek, modern status card rendered on HUD while automation is active. */
object HudOverlay {

    fun render(ctx: DrawContext) {
        val cfg = ModConfig.INSTANCE
        if (!cfg.hudEnabled || !TaskEngine.isRunning) return

        val mc = MinecraftClient.getInstance()
        if (mc.options.hudHidden) return

        val tr = mc.textRenderer
        val taskName = TaskEngine.activeName
        val statusText = TaskEngine.activeStatus

        val title = "LAD §d$taskName"
        val status = "§7$statusText"

        val titleW = tr.getWidth("LAD $taskName") + 12
        val statusW = tr.getWidth(statusText) + 12
        val contentW = maxOf(titleW, statusW, 110)

        val padding = 6
        val cardW = contentW + padding * 2
        val cardH = 26
        val x = cfg.hudX
        val y = cfg.hudY

        // Glassmorphic translucent dark background card
        ctx.fill(x, y, x + cardW, y + cardH, 0xD00F0F18.toInt())

        // Top accent line (Purple -> Pink gradient feel)
        ctx.fill(x, y, x + cardW, y + 2, 0xFFBB86FC.toInt())
        ctx.fill(x, y + cardH - 1, x + cardW, y + cardH, 0x30FFFFFF.toInt())
        ctx.fill(x, y + 2, x + 1, y + cardH - 1, 0x30FFFFFF.toInt())
        ctx.fill(x + cardW - 1, y + 2, x + cardW, y + cardH - 1, 0x30FFFFFF.toInt())

        // Active indicator pulse dot
        val dotColor = if (statusText.contains("PAUSED", ignoreCase = true)) 0xFFFFA000.toInt() else 0xFF00E676.toInt()
        ctx.fill(x + 6, y + 7, x + 10, y + 11, dotColor)

        // Title and status text
        ctx.drawTextWithShadow(tr, Text.literal(title), x + 14, y + 5, 0xFFFFFF)
        ctx.drawTextWithShadow(tr, Text.literal(status), x + 6, y + 15, 0xDDDDDD)
    }
}

