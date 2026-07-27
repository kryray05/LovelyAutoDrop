package com.lovely.autodrop

import com.lovely.autodrop.util.SpawnerStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnerInfoParserTest {

    @Test
    fun `test spawner stats calculations for 2095 spawners`() {
        val stats = SpawnerStats(
            title = "Skeleton LỒNG SINH SẢN",
            speedMinSec = 13,
            speedMaxSec = 16,
            itemsMinPerCycle = 2095,
            itemsMaxPerCycle = 4190,
            xpPerCycle = 4190,
            stackSize = 2095
        )

        assertEquals(14.5, stats.averageSpeedSec)
        assertEquals(3142.5, stats.averageItemsPerCycle)
        assertTrue(stats.itemsPerSecond > 200.0)

        val fillMins = stats.calculateFillTimeMinutes(320)
        assertTrue(fillMins < 0.1)
    }

    @Test
    fun `test spawner stats calculations for small spawner stack`() {
        val stats = SpawnerStats(
            title = "Blaze LỒNG SINH SẢN",
            speedMinSec = 15,
            speedMaxSec = 15,
            itemsMinPerCycle = 30,
            itemsMaxPerCycle = 30,
            xpPerCycle = 60,
            stackSize = 10
        )

        assertEquals(2.0, stats.itemsPerSecond) // 30 items per 15 sec = 2 items/sec
        val fillMins = stats.calculateFillTimeMinutes(320) // 320 / 2 = 160s = 2.67 mins
        assertEquals(2.6666666666666665, fillMins)
    }
}
