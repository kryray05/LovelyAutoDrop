package com.lovely.autodrop.util

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.text.Normalizer
import java.util.Locale

/**
 * Turns the string lists from the config into real item checks with robust
 * Minecraft color code stripping and accent-insensitive matching.
 */
object ItemMatcher {

    /** Strips Minecraft section color formatting (§a, §l, etc) and zero-width spaces. */
    fun stripFormatting(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input.replace(Regex("§[0-9a-fk-orA-FK-OR]|[\u00A0\u200B-\u200D\uFEFF]"), "").trim()
    }

    /** Removes diacritics / accent marks (e.g. "ORDER CỦA BẠN" -> "order cua ban"). */
    fun removeAccents(input: String): String {
        val temp = Normalizer.normalize(input, Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
            .replace('đ', 'd').replace('Đ', 'D')
    }

    fun idOf(stack: ItemStack): String =
        Registries.ITEM.getId(stack.item).toString()

    fun pathOf(stack: ItemStack): String =
        Registries.ITEM.getId(stack.item).path

    fun displayName(stack: ItemStack): String =
        stack.name.string

    fun cleanDisplayName(stack: ItemStack): String =
        stripFormatting(displayName(stack))

    /** True when [stack] matches any pattern in [patterns]. */
    fun matchesAny(stack: ItemStack, patterns: Collection<String>): Boolean {
        if (stack.isEmpty || patterns.isEmpty()) return false

        val id = idOf(stack).lowercase(Locale.ROOT)
        val path = pathOf(stack).lowercase(Locale.ROOT)
        val cleanName = cleanDisplayName(stack).lowercase(Locale.ROOT)
        val unaccentedName = removeAccents(cleanName)

        for (raw in patterns) {
            val p = raw.trim().lowercase(Locale.ROOT)
            if (p.isEmpty()) continue

            // @ prefix -> display-name match
            if (p.startsWith("@")) {
                val search = stripFormatting(p.substring(1)).lowercase(Locale.ROOT)
                val unaccentedSearch = removeAccents(search)
                if (search.isNotEmpty() && (cleanName.contains(search) || unaccentedName.contains(unaccentedSearch))) return true
                continue
            }

            // wildcard match on id, path, or display name
            if (p.contains('*')) {
                if (wildcard(id, p) || wildcard(path, p) || wildcard(cleanName, p)) return true
                continue
            }

            val normalised = if (p.contains(':')) p else "minecraft:$p"
            if (id == normalised || path == p) return true
            if (cleanName.contains(p) || unaccentedName.contains(removeAccents(p))) return true
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

    /** Any lore/name line contains one of the keywords (formatting & accent insensitive). */
    fun hasKeyword(stack: ItemStack, keywords: Collection<String>): Boolean {
        if (keywords.isEmpty() || stack.isEmpty()) return false

        val cleanName = cleanDisplayName(stack).lowercase(Locale.ROOT)
        val unaccentedName = removeAccents(cleanName)

        val loreLines = stack.get(DataComponentTypes.LORE)?.lines()
        val cleanLore = loreLines?.map { stripFormatting(it.string).lowercase(Locale.ROOT) }
        val unaccentedLore = cleanLore?.map { removeAccents(it) }

        for (rawKw in keywords) {
            val kw = stripFormatting(rawKw).lowercase(Locale.ROOT)
            if (kw.isEmpty()) continue
            val unaccentedKw = removeAccents(kw)

            if (cleanName.contains(kw) || unaccentedName.contains(unaccentedKw)) return true
            if (cleanLore != null && unaccentedLore != null) {
                for (i in cleanLore.indices) {
                    if (cleanLore[i].contains(kw) || unaccentedLore[i].contains(unaccentedKw)) return true
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
