package com.lovely.autodrop.feature

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.core.Task
import com.lovely.autodrop.util.Chat
import com.lovely.autodrop.util.GuiHelper
import com.lovely.autodrop.util.ItemMatcher
import com.lovely.autodrop.util.SpawnerInfoParser
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
    private var actionCooldown = 0
    private var lastHash = Int.MIN_VALUE
    private var idleTicks = 0
    private var paused = false
    private var pausedHash = Int.MIN_VALUE
    private var note = ""
    private var clickedOnCurrentHash = false
    private var statsParsedThisRun = false
    private var droppedOnCurrentHash = false
    private var soldOnCurrentHash = false

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

        if (actionCooldown > 0) actionCooldown--

        if (cooldown > 0) {
            cooldown--
            return true
        }

        if (cfg.maxClicksPerRun in 1..(clicks + dropped)) {
            Chat.warn("Action limit (${cfg.maxClicksPerRun}) reached.")
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

            if (!statsParsedThisRun) {
                val stats = handler.slots.firstNotNullOfOrNull { slot -> SpawnerInfoParser.parse(slot.stack) }
                if (stats != null) {
                    statsParsedThisRun = true
                    Chat.info("§a[LAD] Nhận diện Spawner: ${stats.title}")
                    Chat.info("§7  • Chồng: ${stats.stackSize} lồng | Tốc độ: ${stats.speedMinSec}-${stats.speedMaxSec}s")
                    Chat.info("§7  • Sản xuất: ${stats.itemsMinPerCycle}-${stats.itemsMaxPerCycle} món/lần (~${"%.1f".format(stats.itemsPerSecond)} món/s)")

                    if (cfg.autoSpawnerAutoAdjust) {
                        val fillTimeMins = stats.calculateFillTimeMinutes(cfg.autoSpawnerTargetItems)
                        // User requirement: Loop interval MUST BE AT LEAST 15-30 minutes
                        val newMin = fillTimeMins.toInt().coerceIn(15, 180)
                        val newMax = (fillTimeMins * 1.25).toInt().coerceIn(newMin, 180).coerceAtLeast(30)

                        if (cfg.autoSpawnerMinMinutes != newMin || cfg.autoSpawnerMaxMinutes != newMax) {
                            cfg.autoSpawnerMinMinutes = newMin
                            cfg.autoSpawnerMaxMinutes = newMax
                            cfg.save()
                            AutoSpawnerRoutine.resetTimer()
                            Chat.success("§a  -> Đã tự động cập nhật Auto Loop: ${newMin}m - ${newMax}m (Tối thiểu 15 phút/chu kỳ)")
                        }
                    }
                }
            }

            val container = GuiHelper.containerSlots(handler)
            val hash = GuiHelper.contentHash(handler) { slot ->
                if (container.size >= 54 && slot.id in 45..53) return@contentHash false
                !isMenuButton(slot.stack, cfg)
            }
            if (hash != lastHash) {
                lastHash = hash
                idleTicks = 0
                clickedOnCurrentHash = false
                droppedOnCurrentHash = false
                soldOnCurrentHash = false
                if (paused && hash != pausedHash) {
                    paused = false
                    note = ""
                    Chat.info("Page changed, resuming.")
                }
            } else {
                idleTicks++
                if (clickedOnCurrentHash) {
                    if (idleTicks > cfg.guiIdleTimeoutTicks) {
                        // Configured timeout passed after button click without hash change -> allow retry
                        clickedOnCurrentHash = false
                        idleTicks = 0
                    } else {
                        return true // Wait for GUI update after button click
                    }
                }
            }

            if (paused) return true

            // Protect Row 6 (slots 45..53 in a 54-slot chest GUI) from being treated as loot storage
            val containerLootSlots = if (container.size >= 54) container.take(45) else container
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
            if (storageSlot != null) {
                if (!clickedOnCurrentHash && actionCooldown <= 0 && GuiHelper.click(mc, storageSlot.id, 0, SlotActionType.PICKUP)) {
                    clicks++
                    clickedOnCurrentHash = true
                    val delay = nextDelay(cfg)
                    cooldown = delay
                    actionCooldown = maxOf(delay, cfg.settleTicks + 5, 10)
                    note = "clicked Kho Chứa"
                    return true
                }
                return true
            }

            // ---- 2. INSIDE STORAGE GUI: Drop Loot -> Sell All Sequence ----
            val hasValuableLootInStorage = lootSlots.any { slot ->
                val s = slot.stack
                !s.isEmpty && !isMenuButton(s, cfg) && !isBlocked(s, cfg)
            }
            val arrowCountInStorage = lootSlots
                .filter { slot -> !slot.stack.isEmpty && isBlocked(slot.stack, cfg) }
                .sumOf { it.stack.count }

            val hasTrashItemsInStorage = arrowCountInStorage > 0
            val hasMoreThanOneStackOfArrows = arrowCountInStorage > 64

            // Step 2A: Always click DROP LOOT while valuable items (blaze_rod, bone, etc.) exist in storage
            if (dropSlot != null && hasValuableLootInStorage) {
                if (actionCooldown <= 0 && GuiHelper.click(mc, dropSlot.id, 0, SlotActionType.PICKUP)) {
                    clicks++
                    dropped++
                    clickedOnCurrentHash = true
                    val delay = nextDelay(cfg)
                    cooldown = delay
                    actionCooldown = maxOf(delay, cfg.settleTicks + 5, 10)
                    note = "clicked Drop Loot"
                    return true
                }
                return true // Stay in task while valuable loot is present
            }

            // Step 2B: Trigger SELL ALL only when NO valuable loot remains and trash items exist
            val shouldSellAll = sellSlot != null && !soldOnCurrentHash && !hasValuableLootInStorage && hasTrashItemsInStorage

            if (shouldSellAll) {
                if (actionCooldown <= 0 && GuiHelper.click(mc, sellSlot!!.id, 0, SlotActionType.PICKUP)) {
                    clicks++
                    clickedOnCurrentHash = true
                    soldOnCurrentHash = true
                    val delay = nextDelay(cfg)
                    cooldown = delay
                    actionCooldown = maxOf(delay, cfg.settleTicks + 5, 10)
                    note = if (hasMoreThanOneStackOfArrows) "clicked SELL ALL (> 1 stack arrows)" else "clicked SELL ALL (trash items)"
                    return true
                }
                return true
            }

            // Step 2C: Finish when storage is empty or action sequence complete
            if (!hasValuableLootInStorage && (!hasTrashItemsInStorage || soldOnCurrentHash || dropSlot == null) && actionCooldown <= 0) {
                return finish(mc, cfg)
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
                        pausedHash = GuiHelper.contentHash(handler) { slot ->
                            if (container.size >= 54 && slot.id in 45..53) return@contentHash false
                            !isMenuButton(slot.stack, cfg)
                        }
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
                if (cfg.spawnerSkipNamedItems && ItemMatcher.isCustom(s)) return@firstOrNull false
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

        if (cfg.maxClicksPerRun in 1..(clicks + dropped)) {
            Chat.warn("Action limit (${cfg.maxClicksPerRun}) reached.")
            return false
        }

        val slot = handler.slots.firstOrNull { s ->
            if (!GuiHelper.isPlayerSlot(s)) return@firstOrNull false
            if (cfg.spawnerProtectLastHotbarSlot && GuiHelper.isLastHotbarSlot(s)) return@firstOrNull false
            val stack = s.stack
            if (stack.isEmpty) return@firstOrNull false
            if (isBlocked(stack, cfg)) return@firstOrNull false
            if (cfg.spawnerSkipNamedItems && ItemMatcher.isCustom(stack)) return@firstOrNull false
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

    /** Total amount of that item across the main player inventory. */
    private fun countOf(mc: MinecraftClient, like: ItemStack): Int {
        val inv = mc.player?.inventory ?: return 0
        var total = 0
        val maxSlot = minOf(inv.size(), 36)
        for (i in 0 until maxSlot) {
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
