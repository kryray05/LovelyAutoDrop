package com.lovely.autodrop

import com.lovely.autodrop.util.ItemMatcher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WildcardTest {

    @Test
    fun `suffix wildcard matches`() {
        assertTrue(ItemMatcher.wildcard("minecraft:bone", "*bone"))
        assertTrue(ItemMatcher.wildcard("minecraft:bone_meal", "*bone*"))
        assertTrue(ItemMatcher.wildcard("bone_meal", "bone*"))
    }

    @Test
    fun `prefix anchored wildcard`() {
        assertTrue(ItemMatcher.wildcard("minecraft:blaze_rod", "minecraft:*"))
        assertFalse(ItemMatcher.wildcard("othermod:blaze_rod", "minecraft:*"))
    }

    @Test
    fun `arrow must not match bone patterns`() {
        assertFalse(ItemMatcher.wildcard("minecraft:arrow", "*bone*"))
        assertFalse(ItemMatcher.wildcard("minecraft:glowstone_dust", "*bone*"))
    }

    @Test
    fun `glowstone matches its own wildcard`() {
        assertTrue(ItemMatcher.wildcard("minecraft:glowstone_dust", "*glowstone*"))
    }
}

