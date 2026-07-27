package com.lovely.autodrop.core

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.feature.AutoSpawnerRoutine
import com.lovely.autodrop.feature.OrderDeliverTask
import com.lovely.autodrop.feature.SpawnerLootTask
import com.lovely.autodrop.gui.ConfigScreen
import com.lovely.autodrop.util.Chat
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

/**
 * All keys are rebindable in Options -> Controls, which is the standard place
 * players look and works identically inside Lunar Client.
 */
object Keybinds {

    private val CATEGORY: KeyBinding.Category =
        KeyBinding.Category.create(net.minecraft.util.Identifier.of("lovelyautodrop", "main"))

    lateinit var openConfig: KeyBinding
        private set
    lateinit var runOrders: KeyBinding
        private set
    lateinit var runSpawner: KeyBinding
        private set
    lateinit var autoSpawnerLoop: KeyBinding
        private set
    lateinit var panic: KeyBinding
        private set

    fun register() {
        openConfig = bind("key.lovelyautodrop.config", GLFW.GLFW_KEY_P)
        runOrders = bind("key.lovelyautodrop.orders", GLFW.GLFW_KEY_O)
        runSpawner = bind("key.lovelyautodrop.spawner", GLFW.GLFW_KEY_K)
        autoSpawnerLoop = bind("key.lovelyautodrop.autoloop", GLFW.GLFW_KEY_J)
        panic = bind("key.lovelyautodrop.panic", GLFW.GLFW_KEY_BACKSLASH)
    }

    private fun bind(translationKey: String, defaultKey: Int): KeyBinding =
        KeyBindingHelper.registerKeyBinding(
            KeyBinding(translationKey, InputUtil.Type.KEYSYM, defaultKey, CATEGORY)
        )

    /** Polled every client tick. */
    fun handle(mc: MinecraftClient) {
        // Panic key takes priority: stops active task immediately.
        if (panic.wasPressed()) {
            while (panic.wasPressed()) {} // consume buffered inputs
            if (TaskEngine.isRunning) {
                TaskEngine.stop("stopped (panic key)")
            } else {
                Chat.info("Nothing running.")
            }
        }

        while (openConfig.wasPressed()) {
            mc.setScreen(ConfigScreen(mc.currentScreen))
        }

        while (runOrders.wasPressed()) {
            if (!ModConfig.INSTANCE.ordersEnabled) {
                Chat.warn("Orders feature is off. Enable it in the config.")
            } else {
                TaskEngine.toggle { OrderDeliverTask() }
            }
        }

        while (runSpawner.wasPressed()) {
            if (!ModConfig.INSTANCE.spawnerEnabled) {
                Chat.warn("Spawner feature is off. Enable it in the config.")
            } else {
                TaskEngine.toggle { SpawnerLootTask() }
            }
        }

        while (autoSpawnerLoop.wasPressed()) {
            AutoSpawnerRoutine.toggle()
        }
    }
}

