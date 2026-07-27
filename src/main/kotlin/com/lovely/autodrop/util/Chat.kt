package com.lovely.autodrop.util

import com.lovely.autodrop.config.ModConfig
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/** Client-side chat output. Nothing here is ever sent to the server. */
object Chat {

    private val PREFIX: Text = Text.literal("[LAD] ").formatted(Formatting.LIGHT_PURPLE)

    private fun send(body: Text, force: Boolean = false) {
        if (!force && !ModConfig.INSTANCE.chatFeedback) return
        val mc = MinecraftClient.getInstance()
        mc.player?.sendMessage(Text.empty().append(PREFIX).append(body), false)
    }

    fun info(msg: String) = send(Text.literal(msg).formatted(Formatting.GRAY))

    fun success(msg: String) = send(Text.literal(msg).formatted(Formatting.GREEN))

    fun warn(msg: String) = send(Text.literal(msg).formatted(Formatting.YELLOW), force = true)

    fun error(msg: String) = send(Text.literal(msg).formatted(Formatting.RED), force = true)

    /** Always shown, used by the /lad command replies. */
    fun reply(msg: String) = send(Text.literal(msg).formatted(Formatting.WHITE), force = true)
}
