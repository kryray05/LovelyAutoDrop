package com.lovely.autodrop.util

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.Locale

/**
 * Turns the string lists from the config into real item checks.
 *
 * Accepted entry formats:
 *   minecraft:bone      exact registry id
 *   bone                id without namespace (assumes minecraft: or matches path)
 *   *bone*              wildcard on the id, path, or display name
 *   @Ancient Bone       match the item's *display name* instead of its id
 */
object ItemMatcher {

    fun idOf(stack: ItemStack): String =
        Registries.ITEM.getId(stack.item).toString()

    fun pathOf(stack: ItemStack): String =
        Registries.ITEM.getId(stack.item).path

    fun displayName(stack: ItemStack): String =
        stack.name.string

    /** True when [stack] matches any pattern in [patterns]. */
    fun matchesAny(stack: ItemStack, patterns: Collection<String>): Boolean {
        if (stack.isEmpty || patterns.isEmpty()) return false

        val id = idOf(stack).lowercase(Locale.ROOT)
        val path = pathOf(stack).lowercase(Locale.ROOT)
        val name = displayName(stack).lowercase(Locale.ROOT)

        for (raw in patterns) {
            val p = raw.trim().lowercase(Locale.ROOT)
            if (p.isEmpty()) continue

            // @ prefix -> display-name match
            if (p.startsWith("@")) {
                val search = p.substring(1).trim()
                if (search.isNotEmpty() && name.contains(search)) return true
                continue
            }

            // wildcard match on id, path, or display name
            if (p.contains('*')) {
                if (wildcard(id, p) || wildcard(path, p) || wildcard(name, p)) return true
                continue
            }

            val normalised = if (p.contains(':')) p else "minecraft:$p"
            if (id == normalised || path == p) return true
        }
        return false
    }

    /** True when the stack has a custom name or lore (likely a special/unique drop). */
    fun isCustom(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) return true
        if (stack.contains(DataComponentTypes.ITEM_NAME)) return true
        val lore = stack.get(DataComponentTypes.LORE)
        return lore != null && lore.lines().isNotEmpty()
    }

    /** Any lore/name line contains one of the keywords. */
    fun hasKeyword(stack: ItemStack, keywords: Collection<String>): Boolean {
        if (keywords.isEmpty() || stack.isEmpty()) return false
        val nameLower = displayName(stack).lowercase(Locale.ROOT)
        val loreLines = stack.get(DataComponentTypes.LORE)?.lines()

        for (rawKw in keywords) {
            val kw = rawKw.trim().lowercase(Locale.ROOT)
            if (kw.isEmpty()) continue
            if (nameLower.contains(kw)) return true
            if (loreLines != null) {
                for (line in loreLines) {
                    if (line.string.lowercase(Locale.ROOT).contains(kw)) return true
                }
            }
        }
        return false
    }

    /** Validate a user-typed pattern so the config screen can warn about typos. */
    fun isKnownItem(pattern: String): Boolean {
        val p = pattern.trim()
        if (p.isEmpty()) return false
        if (p.startsWith("@") || p.contains('*')) return true // free-form
        val normalised = if (p.contains(':')) p else "minecraft:$p"
        val id = Identifier.tryParse(normalised) ?: return false
        return Registries.ITEM.containsId(id)
    }

    /** Flexible wildcard pattern matcher (* equals any sequence of characters). */
    fun wildcard(value: String, pattern: String): Boolean {
        val parts = pattern.split('*')
        var idx = 0
        for ((i, part) in parts.withIndex()) {
            if (part.isEmpty()) continue
            val found = value.indexOf(part, idx)
            if (found < 0) return false
            if (i == 0 && !pattern.startsWith("*") && found != 0) return false
            idx = found + part.length
        }
        if (!pattern.endsWith("*") && parts.last().isNotEmpty() && !value.endsWith(parts.last())) {
            return false
        }
        return true
    }
}

