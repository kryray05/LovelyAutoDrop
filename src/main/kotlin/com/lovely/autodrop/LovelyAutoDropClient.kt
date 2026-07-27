package com.lovely.autodrop

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.core.Commands
import com.lovely.autodrop.core.Keybinds
import com.lovely.autodrop.core.TaskEngine
import com.lovely.autodrop.feature.AutoSpawnerRoutine
import com.lovely.autodrop.gui.HudOverlay
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

/**
 * LovelyAutoDrop -- client-side automation helper for order + spawner grinding.
 *
 * Design notes
 *  - client only, no mixins, no server packets other than normal slot clicks
 *  - one task at a time, driven from the client tick
 *  - every value is editable in game and saved to config/lovelyautodrop.json
 */
object LovelyAutoDropClient : ClientModInitializer {

    const val MOD_ID = "lovelyautodrop"
    private val LOGGER = LoggerFactory.getLogger("LovelyAutoDrop")

    override fun onInitializeClient() {
        ModConfig.INSTANCE // force load / create the file
        Keybinds.register()
        Commands.register()

        ClientTickEvents.END_CLIENT_TICK.register { mc: MinecraftClient ->
            Keybinds.handle(mc)
            TaskEngine.tick(mc)
            AutoSpawnerRoutine.tick(mc)
        }

        HudElementRegistry.addLast(
            Identifier.of(MOD_ID, "status"),
            HudElement { ctx, _ -> HudOverlay.render(ctx) }
        )

        LOGGER.info("LovelyAutoDrop ready. P = config, O = orders, K = spawner.")
    }
}
