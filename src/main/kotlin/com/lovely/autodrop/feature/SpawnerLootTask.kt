package com.lovely.autodrop.feature

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.core.Task
import com.lovely.autodrop.util.Chat
import com.lovely.autodrop.util.GuiHelper
import com.lovely.autodrop.util.ItemMatcher
import net.minecraft.client.MinecraftClient
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.SlotActionType
import kotlin.random.Random

/**
 * Skeleton / Blaze spawner loot handler.
 *
 * GUI mode
 *   Clicks the loot slots of an open spawner screen. Before every single
 *   click the whole visible page is scanned for the blocked items
 *   (arrow, glowstone dust by default). If one is present the configured
 *   [ModConfig.BlockAction] decides what happens:
 *
 *     STOP  -> the task ends immediately   (your requested behaviour)
 *     SKIP  -> that slot is left alone, other slots keep being clicked
 *     PAUSE -> clicking halts until the page changes, then resumes
 *
 * Inventory mode
 *   Drops allowed items straight out of the player inventory, keeping
 *   [ModConfig.spawnerKeepAmount] of each. Blocked items are never dropped.
 */
class SpawnerLootTask : Task {

    override val name = "Spawner"

    private var clicks = 0
    private var dropped = 0
    private var cooldown = 0
    private var lastHash = Int.MIN_VALUE
    private var idleTicks = 0
    private var paused = false
    private var pausedHash = Int.MIN_VALUE
    private var note = ""
    private var clickedOnCurrentHash = false

    override val status: String
        get() = buildString {
            if (paused) append("PAUSED ") else append("running ")
            append("| clicks $clicks")
            if (dropped > 0) append(" | dropped $dropped")
            if (note.isNotEmpty()) append(" | $note")
        }

    override fun tick(mc: MinecraftClient): Boolean {
        val cfg = ModConfig.INSTANCE
        if (!cfg.spawnerEnabled) {
            Chat.warn("Spawner feature is disabled in the config.")
            return false
        }

        if (cooldown > 0) {
            cooldown--
            return true
        }

        if (cfg.maxClicksPerRun in 1..clicks) {
            Chat.warn("Click limit (${cfg.maxClicksPerRun}) reached.")
            return false
        }

        val handler = GuiHelper.handler(mc)

        // ---------------------------------------------------------- GUI mode
        if (cfg.spawnerGuiMode && handler != null) {
            if (!GuiHelper.titleMatches(mc, cfg.spawnerTitleMatch)) {
                // Some other GUI opened -> stop rather than click a random chest.
                if (cfg.stopOnScreenChange) {
                    Chat.warn("Different GUI opened (\"${GuiHelper.title(mc)}\"). Stopping.")
                    return false
                }
                return true
            }

            val hash = GuiHelper.contentHash(handler)
            if (hash != lastHash) {
                lastHash = hash
                idleTicks = 0
                clickedOnCurrentHash = false
                if (paused && hash != pausedHash) {
                    paused = false
                    note = ""
                    Chat.info("Page changed, resuming.")
                }
            } else {
                idleTicks++
                if (clickedOnCurrentHash) {
                    if (idleTicks > 60) {
                        // 3 seconds passed after button click without hash change -> allow retry
                        clickedOnCurrentHash = false
                        idleTicks = 0
                    } else {
                        return true // Wait for GUI update after button click
                    }
                }
            }

            if (paused) return true

            val container = GuiHelper.containerSlots(handler)
            // Top 5 lines in a 6-line container GUI (first 45 slots = lines 1..5).
            // Filter out menu & navigation buttons (like "GO BACK", "Kho Chứa", "Drop Loot", "SELL ALL").
            val containerLootSlots = if (container.size > 45) container.take(45) else container
            val lootSlots = containerLootSlots.filter { !isMenuButton(it.stack, cfg) }

            val sellSlot = container.firstOrNull { slot ->
                val s = slot.stack
                !s.isEmpty && ItemMatcher.hasKeyword(s, cfg.spawnerSellAllButtonNames)
            }
            val dropSlot = container.firstOrNull { slot ->
                val s = slot.stack
                !s.isEmpty && ItemMatcher.hasKeyword(s, cfg.spawnerDropLootButtonNames)
            }
            val storageSlot = container.firstOrNull { slot ->
                val s = slot.stack
                !s.isEmpty && ItemMatcher.hasKeyword(s, cfg.spawnerStorageButtonNames)
            }

            // ---- 1. MENU NAVIGATION: "KHO CHỨA" Storage Button ----
            // Always click "KHO CHỨA" first if we are still on the Main Menu
            if (storageSlot != null) {
                if (!clickedOnCurrentHash && GuiHelper.click(mc, storageSlot.id, 0, SlotActionType.PICKUP)) {
                    clicks++
                    clickedOnCurrentHash = true
                    cooldown = nextDelay(cfg)
                    note = "clicked Kho Chứa"
                    return true
                }
                return true
            }

            // ---- 2. INSIDE "KHO CHỨA" STORAGE GUI ----
            val hasStorageActionButtons = sellSlot != null || dropSlot != null
            val hasLootItemsInSlots = lootSlots.any { !it.stack.isEmpty }

            if (hasLootItemsInSlots && hasStorageActionButtons) {
                val hasTriggerItem = lootSlots.any { slot ->
                    val s = slot.stack
                    !s.isEmpty && (
                        cfg.spawnerSellTriggerItems.isEmpty() ||
                        ItemMatcher.matchesAny(s, cfg.spawnerSellTriggerItems) ||
                        ItemMatcher.matchesAny(s, cfg.spawnerBlockItems)
                    )
                }

                if (hasTriggerItem && sellSlot != null) {
                    if (!clickedOnCurrentHash && GuiHelper.click(mc, sellSlot.id, 0, SlotActionType.PICKUP)) {
                        clicks++
                        clickedOnCurrentHash = true
                        cooldown = nextDelay(cfg)
                        note = "clicked SELL ALL (trigger item in GUI)"
                        return true
                    }
                } else if (dropSlot != null) {
                    if (!clickedOnCurrentHash && GuiHelper.click(mc, dropSlot.id, 0, SlotActionType.PICKUP)) {
                        clicks++
                        clickedOnCurrentHash = true
                        cooldown = nextDelay(cfg)
                        note = "clicked Drop Loot"
                        return true
                    }
                }
            }

            // ---- 3. THE GUARD (Blocked Items Check) ----
            val blockedSlot = lootSlots.firstOrNull { slot ->
                val s = slot.stack
                !s.isEmpty && isBlocked(s, cfg)
            }

            if (blockedSlot != null) {
                val what = ItemMatcher.displayName(blockedSlot.stack)
                when (cfg.spawnerBlockAction) {
                    ModConfig.BlockAction.STOP -> {
                        Chat.warn("Blocked item on page ($what). Stopping as configured.")
                        return finish(mc, cfg)
                    }

                    ModConfig.BlockAction.PAUSE -> {
                        paused = true
                        pausedHash = GuiHelper.contentHash(handler)
                        note = "blocked: $what"
                        Chat.info("Blocked item on page ($what). Pausing until it changes.")
                        return true
                    }

                    ModConfig.BlockAction.SKIP -> {
                        // fall through, individual slots are filtered below
                    }
                }
            }

            // ---- 4. PICK A SLOT TO CLICK (Collect items) ----
            val target = lootSlots.firstOrNull { slot ->
                val s = slot.stack
                if (s.isEmpty) return@firstOrNull false
                if (isBlocked(s, cfg)) return@firstOrNull false // never click blocked
                ItemMatcher.matchesAny(s, cfg.spawnerAllowItems)
            }

            if (target == null) {
                if (cfg.spawnerInventoryMode) return doInventoryDrop(mc, cfg)
                Chat.info("No collectable loot on this page.")
                return finish(mc, cfg)
            }

            if (!GuiHelper.click(mc, target.id, 0, SlotActionType.QUICK_MOVE)) {
                Chat.error("Click failed on slot ${target.id}.")
                return false
            }
            clicks++
            cooldown = nextDelay(cfg)
            return true
        }

        // ---------------------------------------------------- inventory mode
        if (cfg.spawnerInventoryMode) return doInventoryDrop(mc, cfg)

        // GUI mode on but no GUI open -> just idle, waiting for one to appear.
        return true
    }

    /**
     * Drops allowed stacks from the player inventory using the vanilla drop packet.
     *
     * Always uses `currentScreenHandler`, never `playerScreenHandler`: when a
     * container GUI is open those are two different handlers with two different
     * sync ids, and clicking the wrong one gets the packet rejected (or the
     * player kicked on a strict server). `currentScreenHandler` falls back to
     * the survival inventory handler automatically when no GUI is open.
     */
    private fun doInventoryDrop(mc: MinecraftClient, cfg: ModConfig): Boolean {
        val player = mc.player ?: return false
        val handler = player.currentScreenHandler ?: return false

        val slot = handler.slots.firstOrNull { s ->
            if (!GuiHelper.isPlayerSlot(s)) return@firstOrNull false
            val stack = s.stack
            if (stack.isEmpty) return@firstOrNull false
            if (isBlocked(stack, cfg)) return@firstOrNull false
            if (!ItemMatcher.matchesAny(stack, cfg.spawnerAllowItems)) return@firstOrNull false
            countOf(mc, stack) > cfg.spawnerKeepAmount
        }

        if (slot == null) {
            note = "inventory clean"
            return true // keep waiting for more loot instead of ending the task
        }

        val im = mc.interactionManager ?: return false
        im.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, player)
        dropped++
        cooldown = nextDelay(cfg)
        return true
    }

    /** Total amount of that item across the whole inventory. */
    private fun countOf(mc: MinecraftClient, like: ItemStack): Int {
        val inv = mc.player?.inventory ?: return 0
        var total = 0
        for (i in 0 until inv.size()) {
            val s = inv.getStack(i)
            if (!s.isEmpty && s.item == like.item) total += s.count
        }
        return total
    }

    private fun isMenuButton(stack: ItemStack, cfg: ModConfig): Boolean {
        if (stack.isEmpty) return false
        return ItemMatcher.hasKeyword(stack, cfg.spawnerStorageButtonNames) ||
            ItemMatcher.hasKeyword(stack, cfg.spawnerDropLootButtonNames) ||
            ItemMatcher.hasKeyword(stack, cfg.spawnerSellAllButtonNames) ||
            ItemMatcher.hasKeyword(stack, cfg.spawnerBackButtonNames) ||
            ItemMatcher.hasKeyword(stack, cfg.spawnerPageButtonNames)
    }

    private fun isBlocked(stack: ItemStack, cfg: ModConfig): Boolean =
        ItemMatcher.matchesAny(stack, cfg.spawnerBlockItems) ||
            ItemMatcher.hasKeyword(stack, cfg.spawnerBlockKeywords)

    private fun finish(mc: MinecraftClient, cfg: ModConfig): Boolean {
        if (cfg.spawnerCloseWhenDone) GuiHelper.closeScreen(mc)
        return false
    }

    private fun nextDelay(cfg: ModConfig): Int {
        val base = cfg.clickDelayTicks.coerceAtLeast(1)
        val jitter = cfg.jitterTicks.coerceAtLeast(0)
        return if (jitter == 0) base else base + Random.nextInt(jitter + 1)
    }

    override fun stop(mc: MinecraftClient, reason: String) {
        if (clicks > 0 || dropped > 0) {
            Chat.success("Spawner: $clicks click(s), $dropped drop(s).")
        }
    }
}
