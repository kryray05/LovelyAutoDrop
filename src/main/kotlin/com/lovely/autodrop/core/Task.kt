package com.lovely.autodrop.core

import net.minecraft.client.MinecraftClient

/**
 * A single unit of automation.
 *
 * Tasks are driven once per client tick from [TaskEngine]. They never spawn
 * threads and never touch the network directly outside of the tick, which is
 * what keeps the mod safe on Lunar Client and on strict SMP servers.
 */
interface Task {

    /** Short label shown in the HUD, e.g. "Orders". */
    val name: String

    /** One line of live status for the HUD, e.g. "delivered 3 / 27". */
    val status: String

    /** Called once when the task is accepted by the engine. */
    fun start(mc: MinecraftClient) {}

    /**
     * Runs once per client tick.
     * @return true while the task still has work to do, false when finished.
     */
    fun tick(mc: MinecraftClient): Boolean

    /** Called exactly once when the task ends, for any reason. */
    fun stop(mc: MinecraftClient, reason: String) {}
}
