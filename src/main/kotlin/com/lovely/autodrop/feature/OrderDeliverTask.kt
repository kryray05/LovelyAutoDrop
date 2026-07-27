package com.lovely.autodrop.feature

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.core.Task
import com.lovely.autodrop.util.Chat
import com.lovely.autodrop.util.GuiHelper
import com.lovely.autodrop.util.ItemMatcher
import net.minecraft.client.MinecraftClient
import net.minecraft.screen.slot.SlotActionType
import kotlin.random.Random

/**
 * Full /orders delivery cycle.
 *
 * 1. Sends the configured command (default `/orders`) if no GUI is open.
 * 2. Waits for the order screen and verifies the title.
 * 3. Shift-clicks every matching stack (bone / blaze rod ...) out of the
 *    player inventory into the order.
 * 4. Closes the GUI and stops. The mod then waits for your next key press --
 *    it never re-opens the order on its own.
 */
class OrderDeliverTask : Task {

    private enum class Phase { OPENING, WAITING, DELIVERING, DONE }

    override val name = "Orders"

    private var phase = Phase.OPENING
    private var cooldown = 0
    private var waited = 0
    private var delivered = 0
    private var clicks = 0
    private var lastHash = Int.MIN_VALUE
    private var idleTicks = 0
    private var commandSent = false

    override val status: String
        get() = when (phase) {
            Phase.OPENING -> "opening /${ModConfig.INSTANCE.ordersCommand}"
            Phase.WAITING -> "waiting for GUI ($waited)"
            Phase.DELIVERING -> "delivered $delivered stack(s)"
            Phase.DONE -> "done, $delivered stack(s)"
        }

    override fun start(mc: MinecraftClient) {
        // If an order screen is already open we can skip straight to work.
        phase = if (isOrderScreen(mc)) Phase.DELIVERING else Phase.OPENING
    }

    override fun tick(mc: MinecraftClient): Boolean {
        val cfg = ModConfig.INSTANCE
        if (!cfg.ordersEnabled) {
            Chat.warn("Orders feature is disabled in the config.")
            return false
        }

        if (cooldown > 0) {
            cooldown--
            return true
        }

        return when (phase) {
            Phase.OPENING -> doOpen(mc, cfg)
            Phase.WAITING -> doWait(mc, cfg)
            Phase.DELIVERING -> doDeliver(mc, cfg)
            Phase.DONE -> false
        }
    }

    // ------------------------------------------------------------------ phases

    private fun doOpen(mc: MinecraftClient, cfg: ModConfig): Boolean {
        if (isOrderScreen(mc)) {
            phase = Phase.DELIVERING
            return true
        }
        if (!commandSent) {
            val net = mc.networkHandler
            if (net == null) {
                Chat.error("No network handler.")
                return false
            }
            // Close whatever unrelated screen might be open first.
            val commandName = cfg.ordersCommand.trim().replace(Regex("^/+"), "")
            net.sendChatCommand(commandName)
            commandSent = true
            waited = 0
            phase = Phase.WAITING
            cooldown = 5
        }
        return true
    }


    private fun doWait(mc: MinecraftClient, cfg: ModConfig): Boolean {
        if (isOrderScreen(mc)) {
            phase = Phase.DELIVERING
            cooldown = cfg.settleTicks
            lastHash = Int.MIN_VALUE
            return true
        }
        waited++
        if (waited > cfg.ordersOpenWaitTicks) {
            Chat.error(
                "Order GUI did not open in ${cfg.ordersOpenWaitTicks} ticks. " +
                    "Check 'ordersCommand' / 'ordersTitleMatch' in the config."
            )
            return false
        }
        return true
    }

    private fun doDeliver(mc: MinecraftClient, cfg: ModConfig): Boolean {
        val handler = GuiHelper.handler(mc)
        if (handler == null || !isOrderScreen(mc)) {
            // Server closed the GUI on us -> treat as finished.
            phase = Phase.DONE
            return false
        }

        if (cfg.maxClicksPerRun in 1..clicks) {
            Chat.warn("Click limit (${cfg.maxClicksPerRun}) reached.")
            return false
        }

        // Wait for the server to acknowledge the previous click before the next.
        val hash = GuiHelper.contentHash(handler)
        if (hash == lastHash) {
            idleTicks++
            if (idleTicks > cfg.timeoutTicks) {
                Chat.warn("Order GUI stopped responding.")
                return finish(mc, cfg)
            }
        } else {
            idleTicks = 0
            lastHash = hash
        }

        // Find the next player-inventory stack we are allowed to hand in.
        val target = GuiHelper.playerSlots(handler).firstOrNull { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            if (cfg.ordersProtectLastHotbarSlot && GuiHelper.isLastHotbarSlot(slot)) {
                return@firstOrNull false
            }
            if (cfg.ordersSkipNamedItems && ItemMatcher.isCustom(stack)) return@firstOrNull false
            ItemMatcher.matchesAny(stack, cfg.ordersItems)
        }

        if (target == null) {
            Chat.success("Nothing left to deliver ($delivered stack(s) handed in).")
            return finish(mc, cfg)
        }

        val action =
            if (cfg.ordersUseShiftClick) SlotActionType.QUICK_MOVE else SlotActionType.PICKUP

        if (!GuiHelper.click(mc, target.id, button = 0, action = action)) {
            Chat.error("Click failed on slot ${target.id}.")
            return false
        }

        clicks++
        delivered++
        cooldown = nextDelay(cfg)
        return true
    }

    private fun finish(mc: MinecraftClient, cfg: ModConfig): Boolean {
        if (cfg.ordersCloseWhenDone) GuiHelper.closeScreen(mc)
        phase = Phase.DONE
        return false
    }

    // ------------------------------------------------------------------ helpers

    private fun isOrderScreen(mc: MinecraftClient): Boolean =
        GuiHelper.openHandled(mc) != null &&
            GuiHelper.titleMatches(mc, ModConfig.INSTANCE.ordersTitleMatch)

    private fun nextDelay(cfg: ModConfig): Int {
        val base = cfg.clickDelayTicks.coerceAtLeast(1)
        val jitter = cfg.jitterTicks.coerceAtLeast(0)
        return if (jitter == 0) base else base + Random.nextInt(jitter + 1)
    }

    override fun stop(mc: MinecraftClient, reason: String) {
        if (delivered > 0) Chat.success("Orders: $delivered stack(s) delivered.")
    }
}
