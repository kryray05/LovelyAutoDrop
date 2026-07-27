package com.lovely.autodrop.util

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import java.util.Locale

/**
 * Thin, null-safe wrapper around the vanilla container API.
 *
 * All clicks go through [click], which uses exactly the same call the vanilla
 * screen uses when you click with a mouse, so the server sees a normal packet.
 */
object GuiHelper {

    /** The currently open container screen, or null. */
    fun openHandled(mc: MinecraftClient): HandledScreen<*>? =
        mc.currentScreen as? HandledScreen<*>

    fun handler(mc: MinecraftClient): ScreenHandler? = openHandled(mc)?.screenHandler

    fun rawTitle(mc: MinecraftClient): String =
        mc.currentScreen?.title?.string.orEmpty()

    fun title(mc: MinecraftClient): String =
        ItemMatcher.stripFormatting(rawTitle(mc))

    fun titleMatches(mc: MinecraftClient, needles: Collection<String>): Boolean {
        if (needles.isEmpty()) return true
        val cleanT = title(mc).lowercase(Locale.ROOT)
        val unaccentedT = ItemMatcher.removeAccents(cleanT)
        return needles.any { needle ->
            if (needle.isBlank()) return@any false
            val cleanN = ItemMatcher.stripFormatting(needle).lowercase(Locale.ROOT)
            val unaccentedN = ItemMatcher.removeAccents(cleanN)
            cleanT.contains(cleanN) || unaccentedT.contains(unaccentedN)
        }
    }

    /**
     * Slots that belong to the *container* (chest / order GUI), i.e. everything
     * that is not the player's own inventory.
     */
    fun containerSlots(handler: ScreenHandler): List<Slot> =
        handler.slots.filter { !isPlayerSlot(it) }

    /** Slots that belong to the player's inventory (main + hotbar). */
    fun playerSlots(handler: ScreenHandler): List<Slot> =
        handler.slots.filter { isPlayerSlot(it) }

    fun isPlayerSlot(slot: Slot): Boolean {
        val inv = slot.inventory
        return inv is net.minecraft.entity.player.PlayerInventory
    }

    /** True when this player slot is the last hotbar slot (index 8). */
    fun isLastHotbarSlot(slot: Slot): Boolean =
        isPlayerSlot(slot) && slot.index == 8

    fun stackIn(handler: ScreenHandler, slotId: Int): ItemStack =
        handler.slots.getOrNull(slotId)?.stack ?: ItemStack.EMPTY

    /**
     * Send one click. [button] 0 = left, 1 = right.
     * Returns false when the click could not be delivered.
     */
    fun click(
        mc: MinecraftClient,
        slotId: Int,
        button: Int = 0,
        action: SlotActionType = SlotActionType.QUICK_MOVE,
    ): Boolean {
        val player = mc.player ?: return false
        val im = mc.interactionManager ?: return false
        val handler = handler(mc) ?: return false
        if (slotId < 0 || slotId >= handler.slots.size) return false

        im.clickSlot(handler.syncId, slotId, button, action, player)
        return true
    }

    /** Drop a whole stack out of an open GUI (ctrl-Q equivalent). */
    fun throwStack(mc: MinecraftClient, slotId: Int): Boolean =
        click(mc, slotId, button = 1, action = SlotActionType.THROW)

    fun closeScreen(mc: MinecraftClient) {
        mc.player?.closeHandledScreen()
    }

    /**
     * Fingerprint of the container contents. When this value changes we
     * know the server pushed new content or modified container slots.
     * [filter] can be provided to exclude menu buttons or dynamic slots.
     */
    fun contentHash(handler: ScreenHandler, filter: ((Slot) -> Boolean)? = null): Int {
        var h = 1
        for (slot in handler.slots) {
            if (isPlayerSlot(slot)) continue
            if (filter != null && !filter(slot)) continue
            val s = slot.stack
            if (s.isEmpty) {
                h = 31 * h
            } else {
                val itemHash = net.minecraft.registry.Registries.ITEM.getId(s.item).hashCode()
                h = 31 * h + (itemHash * 31 + s.count * 17)
            }
        }
        return h
    }
}

