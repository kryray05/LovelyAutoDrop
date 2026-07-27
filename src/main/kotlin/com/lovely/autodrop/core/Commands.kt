package com.lovely.autodrop.core

import com.lovely.autodrop.config.ModConfig
import com.lovely.autodrop.feature.AutoSpawnerRoutine
import com.lovely.autodrop.feature.OrderDeliverTask
import com.lovely.autodrop.feature.SpawnerLootTask
import com.lovely.autodrop.gui.ConfigScreen
import com.lovely.autodrop.util.Chat
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.MinecraftClient

/**
 * Client-side `/lad` command. Never reaches the server.
 *
 * /lad                 open the config screen
 * /lad orders          run the order delivery once
 * /lad spawner         toggle the spawner task
 * /lad loop            toggle the auto spawner loop
 * /lad stop            stop everything
 * /lad status          print what is running
 * /lad delay <ticks>   change the click delay on the fly
 * /lad block add|del|list <item>
 */
object Commands {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("lad")
                    .executes {
                        val mc = MinecraftClient.getInstance()
                        mc.send { mc.setScreen(ConfigScreen(null)) }
                        1
                    }
                    .then(literal("orders").executes {
                        TaskEngine.start(OrderDeliverTask()); 1
                    })
                    .then(literal("spawner").executes {
                        TaskEngine.toggle { SpawnerLootTask() }; 1
                    })
                    .then(literal("loop").executes {
                        AutoSpawnerRoutine.toggle(); 1
                    })
                    .then(literal("autospawner").executes {
                        AutoSpawnerRoutine.toggle(); 1
                    })
                    .then(literal("stop").executes {
                        TaskEngine.stop("stopped (command)"); 1
                    })
                    .then(literal("reload").executes {
                        ModConfig.reload(); Chat.reply("Config reloaded from disk."); 1
                    })
                    .then(literal("status").executes {
                        val cfg = ModConfig.INSTANCE
                        Chat.reply("Running: ${TaskEngine.activeName} ${TaskEngine.activeStatus}")
                        Chat.reply("Auto Spawner Loop: ${if (cfg.autoSpawnerEnabled) "ENABLED (${AutoSpawnerRoutine.formattedRemainingTime})" else "DISABLED"} [${cfg.autoSpawnerMinMinutes}m - ${cfg.autoSpawnerMaxMinutes}m]")
                        Chat.reply("Orders items: ${cfg.ordersItems.joinToString(", ")}")
                        Chat.reply("Spawner allow: ${cfg.spawnerAllowItems.joinToString(", ")}")
                        Chat.reply(
                            "Spawner block: ${cfg.spawnerBlockItems.joinToString(", ")} " +
                                "-> ${cfg.spawnerBlockAction}"
                        )
                        1
                    })
                    .then(
                        literal("delay").then(
                            argument("ticks", IntegerArgumentType.integer(1, 100)).executes { c ->
                                val t = IntegerArgumentType.getInteger(c, "ticks")
                                ModConfig.INSTANCE.clickDelayTicks = t
                                ModConfig.INSTANCE.save()
                                Chat.reply("Click delay = $t ticks.")
                                1
                            }
                        )
                    )
                    .then(
                        literal("block")
                            .then(literal("list").executes {
                                Chat.reply(
                                    "Blocked: " +
                                        ModConfig.INSTANCE.spawnerBlockItems.joinToString(", ")
                                )
                                1
                            })
                            .then(
                                literal("add").then(
                                    argument("item", StringArgumentType.greedyString())
                                        .executes { c ->
                                            val v = StringArgumentType.getString(c, "item").trim()
                                            val cfg = ModConfig.INSTANCE
                                            if (!cfg.spawnerBlockItems.contains(v)) {
                                                cfg.spawnerBlockItems.add(v)
                                                cfg.save()
                                            }
                                            Chat.reply("Blocked '$v'.")
                                            1
                                        }
                                )
                            )
                            .then(
                                literal("del").then(
                                    argument("item", StringArgumentType.greedyString())
                                        .executes { c ->
                                            val v = StringArgumentType.getString(c, "item").trim()
                                            val cfg = ModConfig.INSTANCE
                                            cfg.spawnerBlockItems.remove(v)
                                            cfg.save()
                                            Chat.reply("Unblocked '$v'.")
                                            1
                                        }
                                )
                            )
                    )
            )
        }
    }
}
