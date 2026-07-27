package com.lovely.autodrop.feature

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.core.Task
import com.lovely.autodrop.util.Chat
import com.lovely.autodrop.util.GuiHelper
import com.lovely.autodrop.util.ItemMatcher
import net.minecraft.client.MinecraftClient
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import java.util.Locale
import kotlin.random.Random

/**
 * Automates the /order collection flow:
 * 1. Opens /orders (or configured command).
 * 2. Clicks "ORDER CỦA BẠN" button in the main orders menu.
 * 3. Selects matching order item (e.g. bone, blaze rod).
 * 4. Clicks "NHẬN" button in the order details/edit screen.
 * 5. In "Collect Items" screen: clicks "DROP ALL", waits for page to finish,
 *    then clicks NEXT page button in line 6 to repeat until all pages are done.
 */
class OrderDeliverTask : Task {

    private enum class Phase { OPENING, WAITING, NAVIGATING, DONE }

    override val name = "Orders"

    private var phase = Phase.OPENING
    private var cooldown = 0
    private var waited = 0
    private var clicks = 0
    private var lastHash = Int.MIN_VALUE
    private var idleTicks = 0
    private var commandSent = false

    override val status: String
        get() = when (phase) {
            Phase.OPENING -> "opening /${ModConfig.INSTANCE.ordersCommand}"
            Phase.WAITING -> "waiting for GUI ($waited)"
            Phase.NAVIGATING -> "processing order ($clicks clicks)"
            Phase.DONE -> "done ($clicks clicks)"
        }

    override fun start(mc: MinecraftClient) {
        phase = if (isOrderScreen(mc)) Phase.NAVIGATING else Phase.OPENING
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
            Phase.NAVIGATING -> doNavigate(mc, cfg)
            Phase.DONE -> false
        }
    }

    // ------------------------------------------------------------------ phases

    private fun doOpen(mc: MinecraftClient, cfg: ModConfig): Boolean {
        if (isOrderScreen(mc)) {
            phase = Phase.NAVIGATING
            return true
        }
        if (!commandSent) {
            val net = mc.networkHandler
            if (net == null) {
                Chat.error("No network handler.")
                return false
            }
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
            phase = Phase.NAVIGATING
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

    private fun doNavigate(mc: MinecraftClient, cfg: ModConfig): Boolean {
        val handler = GuiHelper.handler(mc)
        if (handler == null || !isOrderScreen(mc)) {
            phase = Phase.DONE
            return false
        }

        if (cfg.maxClicksPerRun in 1..clicks) {
            Chat.warn("Click limit (${cfg.maxClicksPerRun}) reached.")
            return false
        }

        val title = GuiHelper.title(mc).lowercase(Locale.ROOT)
        val unaccentedTitle = ItemMatcher.removeAccents(title)

        // Determine current screen type
        return when {
            // Screen 4: ORDERS -> Collect Items / Deliver Items
            title.contains("collect") || title.contains("deliver") ||
                unaccentedTitle.contains("nhan vat pham") || unaccentedTitle.contains("giao vat pham") ||
                unaccentedTitle.contains("giao hang") || unaccentedTitle.contains("tha vat pham") ||
                hasDropAllButton(handler, cfg) -> {
                handleCollectItemsScreen(mc, handler, cfg)
            }
            // Screen 3: ORDERS -> Edit Order
            title.contains("edit order") || title.contains("edit") ||
                unaccentedTitle.contains("chi tiet") || hasClaimButton(handler, cfg) -> {
                handleEditOrderScreen(mc, handler, cfg)
            }
            // Screen 2: ORDERS -> Order của bạn
            title.contains("order của bạn") || unaccentedTitle.contains("order cua ban") ||
                title.contains("your order") || unaccentedTitle.contains("don hang cua ban") -> {
                handleMyOrdersScreen(mc, handler, cfg)
            }
            // Screen 1: Main ORDERS menu (e.g. ORDERS (1/7))
            else -> {
                handleMainMenuScreen(mc, handler, cfg)
            }
        }
    }

    // ---------------------------------------------------------------- screen handlers

    /** Screen 1: Main Orders Menu -> Click "ORDER CỦA BẠN" button or direct order. */
    private fun handleMainMenuScreen(mc: MinecraftClient, handler: ScreenHandler, cfg: ModConfig): Boolean {
        val containerSlots = GuiHelper.containerSlots(handler)
        val yourOrdersSlot = containerSlots.firstOrNull { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            ItemMatcher.hasKeyword(stack, cfg.ordersYourOrdersButtonNames)
        } ?: containerSlots.firstOrNull { slot ->
            // Fallback 1: direct matching order item in main menu (e.g. bone, blaze rod)
            val stack = slot.stack
            !stack.isEmpty && ItemMatcher.matchesAny(stack, cfg.ordersItems)
        } ?: containerSlots.firstOrNull { slot ->
            // Fallback 2: any sign item in container (oak_sign, spruce_sign, sign)
            val stack = slot.stack
            !stack.isEmpty && ItemMatcher.pathOf(stack).contains("sign")
        } ?: containerSlots.firstOrNull { slot ->
            // Fallback 3: slot 49 in row 6
            slot.id == 49 && !slot.stack.isEmpty
        }

        if (yourOrdersSlot == null) {
            Chat.warn("Could not find 'ORDER CỦA BẠN' button in main menu.")
            return finish(mc, cfg)
        }

        Chat.info("§a[LAD] Bấm 'ORDER CỦA BẠN' (Slot ${yourOrdersSlot.id})")
        return doClick(mc, yourOrdersSlot.id, cfg)
    }

    /** Screen 2: ORDERS -> Order của bạn -> Click matching order item (bone / blaze rod). */
    private fun handleMyOrdersScreen(mc: MinecraftClient, handler: ScreenHandler, cfg: ModConfig): Boolean {
        val containerSlots = GuiHelper.containerSlots(handler)
        val targetOrderSlot = containerSlots.firstOrNull { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            ItemMatcher.matchesAny(stack, cfg.ordersItems)
        } ?: containerSlots.firstOrNull { slot ->
            // Fallback: non-empty slot in rows 0..44 that isn't glass pane or barrier
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            val path = ItemMatcher.pathOf(stack)
            !path.contains("glass_pane") && !path.contains("barrier") && slot.id in 0..44
        }

        if (targetOrderSlot == null) {
            Chat.success("No matching order items found to collect.")
            return finish(mc, cfg)
        }

        Chat.info("§a[LAD] Bấm Đơn Hàng (Slot ${targetOrderSlot.id})")
        return doClick(mc, targetOrderSlot.id, cfg)
    }

    /** Screen 3: ORDERS -> Edit Order -> Click "NHẬN" / "GIAO HÀNG" button or deliver items directly. */
    private fun handleEditOrderScreen(mc: MinecraftClient, handler: ScreenHandler, cfg: ModConfig): Boolean {
        val containerSlots = GuiHelper.containerSlots(handler)

        // 1. Match by keywords (ordersClaimButtonNames)
        val claimSlot = containerSlots.firstOrNull { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            ItemMatcher.hasKeyword(stack, cfg.ordersClaimButtonNames)
        } ?: containerSlots.firstOrNull { slot ->
            // 2. Fallback 1: chest, minecart, hopper, dropper, dispenser, emerald, lime/green items, book, paper, sign, star, sunflower, gold
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            val path = ItemMatcher.pathOf(stack)
            path.contains("chest") || path.contains("minecart") || path.contains("hopper") ||
                path.contains("dropper") || path.contains("dispenser") ||
                path.contains("emerald") || path.contains("lime") || path.contains("green") ||
                path.contains("paper") || path.contains("book") || path.contains("sign") ||
                path.contains("star") || path.contains("sunflower") || path.contains("gold")
        } ?: containerSlots.firstOrNull { slot ->
            // 3. Fallback 2: Any non-glass, non-barrier, non-empty slot in container that isn't the order target item itself
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            val path = ItemMatcher.pathOf(stack)
            !path.contains("glass_pane") && !path.contains("barrier") && !ItemMatcher.matchesAny(stack, cfg.ordersItems)
        }

        if (claimSlot != null) {
            Chat.info("§a[LAD] Bấm nút 'NHẬN/GIAO HÀNG' (Slot ${claimSlot.id})")
            return doClick(mc, claimSlot.id, cfg)
        }

        // 4. Fallback 3: Direct delivery from player inventory if no separate claim button exists
        Chat.info("§a[LAD] Thử giao vật phẩm trực tiếp...")
        return handleCollectItemsScreen(mc, handler, cfg)
    }

    /**
     * Screen 4: ORDERS -> Collect Items / Deliver Items
     * 1. If "DROP ALL" / "GIAO TẤT CẢ" button exists in container -> click it.
     * 2. Else if container items exist in slots 0..44 -> click container item.
     * 3. Else if matching items exist in player inventory -> shift-click matching inventory item.
     * 4. Else check NEXT page button in line 6 to repeat, or finish when done.
     */
    private fun handleCollectItemsScreen(mc: MinecraftClient, handler: ScreenHandler, cfg: ModConfig): Boolean {
        val containerSlots = GuiHelper.containerSlots(handler)

        // 1. Check for "DROP ALL" / "GIAO TẤT CẢ" button in container
        val dropAllSlot = containerSlots.firstOrNull { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            ItemMatcher.hasKeyword(stack, cfg.ordersDropAllButtonNames)
        } ?: containerSlots.firstOrNull { slot ->
            val stack = slot.stack
            !stack.isEmpty && (ItemMatcher.pathOf(stack).contains("dropper") || ItemMatcher.pathOf(stack).contains("dispenser"))
        }

        if (dropAllSlot != null) {
            Chat.info("§a[LAD] Bấm nút 'DROP ALL' / 'Giao Tất Cả' (Slot ${dropAllSlot.id})")
            return doClick(mc, dropAllSlot.id, cfg)
        }

        // 2. Check if top container slots (0..44) have collectable items
        val containerItemSlot = containerSlots.filter { it.id in 0..44 }.firstOrNull { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            val path = ItemMatcher.pathOf(stack)
            !path.contains("glass_pane") && !path.contains("barrier")
        }

        if (containerItemSlot != null) {
            Chat.info("§a[LAD] Bấm vật phẩm container (Slot ${containerItemSlot.id})")
            return doClick(mc, containerItemSlot.id, cfg)
        }

        // 3. Check player inventory for items matching ordersItems
        val playerMatchingSlot = GuiHelper.playerSlots(handler).firstOrNull { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@firstOrNull false
            if (cfg.ordersProtectLastHotbarSlot && GuiHelper.isLastHotbarSlot(slot)) return@firstOrNull false
            if (cfg.ordersSkipNamedItems && ItemMatcher.isCustom(stack)) return@firstOrNull false
            ItemMatcher.matchesAny(stack, cfg.ordersItems)
        }

        if (playerMatchingSlot != null) {
            Chat.info("§a[LAD] Giao vật phẩm ${ItemMatcher.cleanDisplayName(playerMatchingSlot.stack)} từ túi đồ (Slot ${playerMatchingSlot.id})")
            return doClick(mc, playerMatchingSlot.id, cfg)
        }

        // 4. No container items, no matching inventory items, no drop all button -> check NEXT page button
        val nextButtonSlot = findNextPageSlot(containerSlots, cfg)
        if (nextButtonSlot != null) {
            Chat.info("§a[LAD] Bấm 'Trang Tiếp' (Slot ${nextButtonSlot.id})")
            return doClick(mc, nextButtonSlot.id, cfg)
        } else {
            Chat.success("Hoàn thành giao/drop tất cả vật phẩm đơn hàng!")
            return finish(mc, cfg)
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun findNextPageSlot(containerSlots: List<Slot>, cfg: ModConfig): Slot? {
        // Line 6 contains slots 45..53. Next button is typically slot 53 (right arrow)
        val slot53 = containerSlots.firstOrNull { it.id == 53 && !it.stack.isEmpty }
        if (slot53 != null && isNextArrowStack(slot53.stack, cfg)) {
            return slot53
        }

        // Search any slot in row 6 (45..53) matching next page keywords or arrow stack
        return containerSlots.firstOrNull { slot ->
            slot.id in 45..53 && !slot.stack.isEmpty && isNextArrowStack(slot.stack, cfg)
        }
    }

    private fun isNextArrowStack(stack: net.minecraft.item.ItemStack, cfg: ModConfig): Boolean {
        if (stack.isEmpty) return false
        val path = ItemMatcher.pathOf(stack)
        if (path == "arrow") return true
        if (ItemMatcher.hasKeyword(stack, cfg.ordersNextPageButtonNames)) return true
        return false
    }

    private fun hasDropAllButton(handler: ScreenHandler, cfg: ModConfig): Boolean {
        return GuiHelper.containerSlots(handler).any { slot ->
            !slot.stack.isEmpty && (ItemMatcher.hasKeyword(slot.stack, cfg.ordersDropAllButtonNames) || ItemMatcher.pathOf(slot.stack).contains("dropper") || ItemMatcher.pathOf(slot.stack).contains("dispenser"))
        }
    }

    private fun hasClaimButton(handler: ScreenHandler, cfg: ModConfig): Boolean {
        return GuiHelper.containerSlots(handler).any { slot ->
            !slot.stack.isEmpty && (ItemMatcher.hasKeyword(slot.stack, cfg.ordersClaimButtonNames) || ItemMatcher.pathOf(slot.stack).contains("chest"))
        }
    }

    private fun doClick(mc: MinecraftClient, slotId: Int, cfg: ModConfig): Boolean {
        val action = if (cfg.ordersUseShiftClick) SlotActionType.QUICK_MOVE else SlotActionType.PICKUP
        if (!GuiHelper.click(mc, slotId, button = 0, action = action)) {
            Chat.error("Click failed on slot $slotId.")
            return false
        }
        clicks++
        cooldown = nextDelay(cfg)
        return true
    }

    private fun finish(mc: MinecraftClient, cfg: ModConfig): Boolean {
        if (cfg.ordersCloseWhenDone) GuiHelper.closeScreen(mc)
        phase = Phase.DONE
        return false
    }

    private fun isOrderScreen(mc: MinecraftClient): Boolean {
        val screen = GuiHelper.openHandled(mc) ?: return false
        val cfg = ModConfig.INSTANCE

        // 1. Title matches config or contains order terms
        if (GuiHelper.titleMatches(mc, cfg.ordersTitleMatch)) return true
        val cleanTitle = GuiHelper.title(mc).lowercase(Locale.ROOT)
        val unaccentedTitle = ItemMatcher.removeAccents(cleanTitle)
        if (cleanTitle.contains("order") || cleanTitle.contains("collect") || cleanTitle.contains("deliver") ||
            unaccentedTitle.contains("nhan") || unaccentedTitle.contains("giao") || cleanTitle.contains("edit")) return true

        // 2. Container contents contain order buttons or order items
        val handler = screen.screenHandler ?: return false
        val containerSlots = GuiHelper.containerSlots(handler)
        return containerSlots.any { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@any false
            ItemMatcher.hasKeyword(stack, cfg.ordersYourOrdersButtonNames) ||
                ItemMatcher.hasKeyword(stack, cfg.ordersClaimButtonNames) ||
                ItemMatcher.hasKeyword(stack, cfg.ordersDropAllButtonNames) ||
                ItemMatcher.matchesAny(stack, cfg.ordersItems) ||
                ItemMatcher.pathOf(stack).contains("sign")
        }
    }

    private fun nextDelay(cfg: ModConfig): Int {
        val base = cfg.clickDelayTicks.coerceAtLeast(1)
        val jitter = cfg.jitterTicks.coerceAtLeast(0)
        return if (jitter == 0) base else base + Random.nextInt(jitter + 1)
    }

    override fun stop(mc: MinecraftClient, reason: String) {
        if (clicks > 0) Chat.success("Orders task finished ($clicks clicks performed).")
    }
}
