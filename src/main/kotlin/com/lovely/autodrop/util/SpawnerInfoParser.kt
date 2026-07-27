package com.lovely.autodrop.util

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack

data class SpawnerStats(
    val title: String,
    val speedMinSec: Int,
    val speedMaxSec: Int,
    val itemsMinPerCycle: Int,
    val itemsMaxPerCycle: Int,
    val xpPerCycle: Int,
    val stackSize: Int,
) {
    val averageSpeedSec: Double
        get() = if (speedMinSec > 0 && speedMaxSec >= speedMinSec) (speedMinSec + speedMaxSec) / 2.0 else maxOf(speedMinSec.toDouble(), 1.0)

    val averageItemsPerCycle: Double
        get() = if (itemsMinPerCycle > 0 && itemsMaxPerCycle >= itemsMinPerCycle) (itemsMinPerCycle + itemsMaxPerCycle) / 2.0 else maxOf(itemsMinPerCycle.toDouble(), 1.0)

    val itemsPerSecond: Double
        get() = if (averageSpeedSec > 0) averageItemsPerCycle / averageSpeedSec else 0.0

    /**
     * Estimates minutes required to fill [targetItemCount] items in storage.
     * Default capacity is 320 items (5 storage slots * 64 items/stack).
     */
    fun calculateFillTimeMinutes(targetItemCount: Int = 320): Double {
        val rate = itemsPerSecond
        if (rate <= 0) return 30.0
        val totalSec = targetItemCount / rate
        return totalSec / 60.0
    }
}

object SpawnerInfoParser {

    /**
     * Inspects [stack] lore and name to extract spawner statistics if present.
     */
    fun parse(stack: ItemStack): SpawnerStats? {
        if (stack.isEmpty) return null

        val name = ItemMatcher.cleanDisplayName(stack)
        val loreLines = stack.get(DataComponentTypes.LORE)?.lines()
            ?.map { ItemMatcher.stripFormatting(it.string) }
            ?: emptyList()

        val allLines = listOf(name) + loreLines
        val combinedText = allLines.joinToString("\n")

        val unaccented = ItemMatcher.removeAccents(combinedText.lowercase())
        if (!unaccented.contains("long sinh san") &&
            !unaccented.contains("spawner") &&
            !unaccented.contains("toc do san xuat") &&
            !unaccented.contains("kich thuoc chong") &&
            !unaccented.contains("production speed")
        ) {
            return null
        }

        var speedMin = 0
        var speedMax = 0
        var itemsMin = 0
        var itemsMax = 0
        var xp = 0
        var stackSize = 1

        for (line in allLines) {
            val clean = ItemMatcher.stripFormatting(line)
            val unaccentedLine = ItemMatcher.removeAccents(clean.lowercase())

            // Tốc độ sản xuất: 13 - 16 giây
            if (unaccentedLine.contains("toc do san xuat") || unaccentedLine.contains("production speed")) {
                val matchRange = Regex("(\\d+)\\s*-\\s*(\\d+)").find(clean)
                if (matchRange != null) {
                    speedMin = matchRange.groupValues[1].toIntOrNull() ?: 0
                    speedMax = matchRange.groupValues[2].toIntOrNull() ?: 0
                } else {
                    val matchSingle = Regex("(\\d+)").find(clean.substringAfter(":"))
                    if (matchSingle != null) {
                        speedMin = matchSingle.groupValues[1].toIntOrNull() ?: 0
                        speedMax = speedMin
                    }
                }
            }

            // Sản xuất vật phẩm: 2095 - 4190 mỗi lần sinh sản
            if (unaccentedLine.contains("san xuat vat pham") || unaccentedLine.contains("item production")) {
                val matchRange = Regex("(\\d+)\\s*-\\s*(\\d+)").find(clean)
                if (matchRange != null) {
                    itemsMin = matchRange.groupValues[1].toIntOrNull() ?: 0
                    itemsMax = matchRange.groupValues[2].toIntOrNull() ?: 0
                } else {
                    val matchSingle = Regex("(\\d+)").find(clean.substringAfter(":"))
                    if (matchSingle != null) {
                        itemsMin = matchSingle.groupValues[1].toIntOrNull() ?: 0
                        itemsMax = itemsMin
                    }
                }
            }

            // Sản xuất XP: 4190 mỗi lần sinh sản
            if (unaccentedLine.contains("san xuat xp") || unaccentedLine.contains("xp production")) {
                val matchSingle = Regex("(\\d+)").find(clean.substringAfter(":"))
                if (matchSingle != null) {
                    xp = matchSingle.groupValues[1].toIntOrNull() ?: 0
                }
            }

            // Kích thước chồng: 2095
            if (unaccentedLine.contains("kich thuoc chong") || unaccentedLine.contains("stack size")) {
                val matchSingle = Regex("(\\d+)").find(clean.substringAfter(":"))
                if (matchSingle != null) {
                    stackSize = matchSingle.groupValues[1].toIntOrNull() ?: 1
                }
            }
        }

        if (speedMin == 0 && itemsMin == 0 && stackSize <= 1) {
            return null
        }

        return SpawnerStats(
            title = name,
            speedMinSec = speedMin,
            speedMaxSec = speedMax,
            itemsMinPerCycle = itemsMin,
            itemsMaxPerCycle = itemsMax,
            xpPerCycle = xp,
            stackSize = stackSize
        )
    }
}
