package com.lovely.autodrop.gui

import com.lovely.autodrop.config.ModConfig
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Modern, self-contained config UI. No ModMenu or external GUI dependencies needed.
 * Works seamlessly on Vanilla Fabric and Lunar Client.
 */
class ConfigScreen(private val parent: Screen?) : Screen(Text.literal("LovelyAutoDrop")) {

    private enum class Tab(val label: String) {
        GENERAL("General"), ORDERS("Orders"), SPAWNER("Spawner")
    }

    private var tab = Tab.GENERAL
    private val cfg get() = ModConfig.INSTANCE

    private var ordersItemsField: TextFieldWidget? = null
    private var ordersTitleField: TextFieldWidget? = null
    private var ordersCommandField: TextFieldWidget? = null
    private var spawnerAllowField: TextFieldWidget? = null
    private var spawnerBlockField: TextFieldWidget? = null
    private var spawnerTitleField: TextFieldWidget? = null
    private var spawnerStorageField: TextFieldWidget? = null
    private var spawnerDropLootField: TextFieldWidget? = null
    private var spawnerSellAllField: TextFieldWidget? = null
    private var spawnerSellTriggerField: TextFieldWidget? = null
    private var spawnerBackField: TextFieldWidget? = null
    private var spawnerPageField: TextFieldWidget? = null

    private val cardWidth = 320
    private val cardHeight = 250
    private val left get() = width / 2 - cardWidth / 2 + 10
    private val colW = 145
    private val rowH = 20

    private val captions = mutableListOf<Caption>()

    private data class Caption(val x: Int, val y: Int, val text: String)

    override fun init() {
        clearChildren()
        captions.clear()

        // Tab selection bar
        var tx = left
        for (t in Tab.entries) {
            val isSelected = t == tab
            val tabLabel = if (isSelected) "§b§l> ${t.label}" else "§7${t.label}"
            addDrawableChild(
                ButtonWidget.builder(Text.literal(tabLabel)) {
                    applyFields()
                    tab = t
                    init()
                }.dimensions(tx, 32, 98, 20).build()
            )
            tx += 102
        }

        when (tab) {
            Tab.GENERAL -> initGeneral()
            Tab.ORDERS -> initOrders()
            Tab.SPAWNER -> initSpawner()
        }

        // Footer buttons
        val btnW = 100
        val btnY = height - 28
        addDrawableChild(
            ButtonWidget.builder(Text.literal("§a§lSave & Close")) {
                applyFields()
                cfg.save()
                close()
            }.dimensions(width / 2 - btnW - 5, btnY, btnW, 20).build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("§cCancel")) {
                close()
            }.dimensions(width / 2 + 5, btnY, btnW, 20).build()
        )
    }

    // ------------------------------------------------------------------ tabs

    private fun initGeneral() {
        var y = 62
        toggle(left, y, "Master enabled", cfg.masterEnabled) {
            cfg.masterEnabled = !cfg.masterEnabled; refresh()
        }
        toggle(left + colW + 10, y, "Chat feedback", cfg.chatFeedback) {
            cfg.chatFeedback = !cfg.chatFeedback; refresh()
        }
        y += rowH
        toggle(left, y, "HUD overlay", cfg.hudEnabled) {
            cfg.hudEnabled = !cfg.hudEnabled; refresh()
        }
        toggle(left + colW + 10, y, "Stop on damage", cfg.stopOnDamage) {
            cfg.stopOnDamage = !cfg.stopOnDamage; refresh()
        }
        y += rowH
        toggle(left, y, "Stop on screen change", cfg.stopOnScreenChange) {
            cfg.stopOnScreenChange = !cfg.stopOnScreenChange; refresh()
        }
        y += rowH + 8

        slider(left, y, "Click delay", cfg.clickDelayTicks, 1, 40) {
            cfg.clickDelayTicks = it
        }
        slider(left + colW + 10, y, "Jitter ticks", cfg.jitterTicks, 0, 20) {
            cfg.jitterTicks = it
        }
        y += rowH
        slider(left, y, "Settle ticks", cfg.settleTicks, 0, 20) {
            cfg.settleTicks = it
        }
        slider(left + colW + 10, y, "Timeout ticks", cfg.timeoutTicks, 20, 600) {
            cfg.timeoutTicks = it
        }
        y += rowH
        slider(left, y, "Max clicks (0=off)", cfg.maxClicksPerRun, 0, 5000) {
            cfg.maxClicksPerRun = it
        }

        captions.add(Caption(left, y + rowH + 8, "§7Keybinds: 'P' = Config | Options → Controls → LovelyAutoDrop"))
    }

    private fun initOrders() {
        var y = 62
        toggle(left, y, "Orders enabled", cfg.ordersEnabled) {
            cfg.ordersEnabled = !cfg.ordersEnabled; refresh()
        }
        toggle(left + colW + 10, y, "Shift-click", cfg.ordersUseShiftClick) {
            cfg.ordersUseShiftClick = !cfg.ordersUseShiftClick; refresh()
        }
        y += rowH
        toggle(left, y, "Close when done", cfg.ordersCloseWhenDone) {
            cfg.ordersCloseWhenDone = !cfg.ordersCloseWhenDone; refresh()
        }
        toggle(left + colW + 10, y, "Skip named items", cfg.ordersSkipNamedItems) {
            cfg.ordersSkipNamedItems = !cfg.ordersSkipNamedItems; refresh()
        }
        y += rowH
        toggle(left, y, "Protect hotbar slot 9", cfg.ordersProtectLastHotbarSlot) {
            cfg.ordersProtectLastHotbarSlot = !cfg.ordersProtectLastHotbarSlot; refresh()
        }
        y += rowH + 6

        captions.add(Caption(left, y, "§7Order Command (without leading slash):"))
        ordersCommandField = field(left, y + 10, cfg.ordersCommand)
        y += rowH + 12

        captions.add(Caption(left, y, "§7Allowed Items to Deliver (comma separated):"))
        ordersItemsField = field(left, y + 10, cfg.ordersItems.joinToString(", "))
        y += rowH + 12

        captions.add(Caption(left, y, "§7Order GUI Title Match (case-insensitive):"))
        ordersTitleField = field(left, y + 10, cfg.ordersTitleMatch.joinToString(", "))
    }

    private fun initSpawner() {
        var y = 62
        toggle(left, y, "Spawner enabled", cfg.spawnerEnabled) {
            cfg.spawnerEnabled = !cfg.spawnerEnabled; refresh()
        }
        toggle(left + colW + 10, y, "GUI mode", cfg.spawnerGuiMode) {
            cfg.spawnerGuiMode = !cfg.spawnerGuiMode; refresh()
        }
        y += rowH
        toggle(left, y, "Inventory mode", cfg.spawnerInventoryMode) {
            cfg.spawnerInventoryMode = !cfg.spawnerInventoryMode; refresh()
        }
        toggle(left + colW + 10, y, "Close when done", cfg.spawnerCloseWhenDone) {
            cfg.spawnerCloseWhenDone = !cfg.spawnerCloseWhenDone; refresh()
        }
        y += rowH

        val actionColor = when (cfg.spawnerBlockAction) {
            ModConfig.BlockAction.STOP -> "§cSTOP"
            ModConfig.BlockAction.SKIP -> "§eSKIP"
            ModConfig.BlockAction.PAUSE -> "§bPAUSE"
        }
        addDrawableChild(
            ButtonWidget.builder(Text.literal("On blocked item: $actionColor")) {
                cfg.spawnerBlockAction = when (cfg.spawnerBlockAction) {
                    ModConfig.BlockAction.STOP -> ModConfig.BlockAction.SKIP
                    ModConfig.BlockAction.SKIP -> ModConfig.BlockAction.PAUSE
                    ModConfig.BlockAction.PAUSE -> ModConfig.BlockAction.STOP
                }
                refresh()
            }.dimensions(left, y, colW, 20).build()
        )

        slider(left + colW + 10, y, "Keep amount", cfg.spawnerKeepAmount, 0, 2304) {
            cfg.spawnerKeepAmount = it
        }
        y += rowH + 4

        captions.add(Caption(left, y, "§bStorage Button:"))
        captions.add(Caption(left + colW + 10, y, "§bDrop Loot Button:"))
        y += 10
        spawnerStorageField = fieldHalf(left, y, cfg.spawnerStorageButtonNames.joinToString(", "))
        spawnerDropLootField = fieldHalf(left + colW + 10, y, cfg.spawnerDropLootButtonNames.joinToString(", "))
        y += rowH + 2

        captions.add(Caption(left, y, "§eSELL ALL Button:"))
        captions.add(Caption(left + colW + 10, y, "§eSell Trigger Items (e.g. arrow):"))
        y += 10
        spawnerSellAllField = fieldHalf(left, y, cfg.spawnerSellAllButtonNames.joinToString(", "))
        spawnerSellTriggerField = fieldHalf(left + colW + 10, y, cfg.spawnerSellTriggerItems.joinToString(", "))
        y += rowH + 2

        captions.add(Caption(left, y, "§6Go Back Keywords:"))
        captions.add(Caption(left + colW + 10, y, "§6Page Nav Keywords:"))
        y += 10
        spawnerBackField = fieldHalf(left, y, cfg.spawnerBackButtonNames.joinToString(", "))
        spawnerPageField = fieldHalf(left + colW + 10, y, cfg.spawnerPageButtonNames.joinToString(", "))
        y += rowH + 2

        captions.add(Caption(left, y, "§aLoot Items to Collect:"))
        spawnerAllowField = field(left, y + 10, cfg.spawnerAllowItems.joinToString(", "))
        y += rowH + 8

        captions.add(Caption(left, y, "§cBLOCKED Items:"))
        spawnerBlockField = field(left, y + 10, cfg.spawnerBlockItems.joinToString(", "))
        y += rowH + 8

        captions.add(Caption(left, y, "§7Spawner GUI Title Match:"))
        spawnerTitleField = field(left, y + 10, cfg.spawnerTitleMatch.joinToString(", "))
    }

    // --------------------------------------------------------------- widgets

    private fun toggle(x: Int, y: Int, label: String, value: Boolean, onClick: () -> Unit) {
        val state = if (value) "§aON" else "§cOFF"
        addDrawableChild(
            ButtonWidget.builder(Text.literal("$label: $state")) { onClick() }
                .dimensions(x, y, colW, 20).build()
        )
    }

    private fun slider(
        x: Int, y: Int, label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit,
    ) {
        addDrawableChild(
            IntSlider(x, y, colW, 20, label, value, min, max, onChange)
        )
    }

    private fun field(x: Int, y: Int, value: String): TextFieldWidget {
        val f = TextFieldWidget(textRenderer, x, y, colW * 2 + 10, 18, Text.empty())
        f.setMaxLength(512)
        f.text = value
        addDrawableChild(f)
        return f
    }

    private fun fieldHalf(x: Int, y: Int, value: String): TextFieldWidget {
        val f = TextFieldWidget(textRenderer, x, y, colW, 18, Text.empty())
        f.setMaxLength(512)
        f.text = value
        addDrawableChild(f)
        return f
    }

    private fun refresh() {
        applyFields()
        cfg.save()
        init()
    }

    /** Copy text field values into config. */
    private fun applyFields() {
        ordersCommandField?.let { cfg.ordersCommand = it.text.trim().replace(Regex("^/+"), "") }
        ordersItemsField?.let { cfg.ordersItems = splitList(it.text) }
        ordersTitleField?.let { cfg.ordersTitleMatch = splitList(it.text) }
        spawnerAllowField?.let { cfg.spawnerAllowItems = splitList(it.text) }
        spawnerBlockField?.let { cfg.spawnerBlockItems = splitList(it.text) }
        spawnerTitleField?.let { cfg.spawnerTitleMatch = splitList(it.text) }
        spawnerStorageField?.let { cfg.spawnerStorageButtonNames = splitList(it.text) }
        spawnerDropLootField?.let { cfg.spawnerDropLootButtonNames = splitList(it.text) }
        spawnerSellAllField?.let { cfg.spawnerSellAllButtonNames = splitList(it.text) }
        spawnerSellTriggerField?.let { cfg.spawnerSellTriggerItems = splitList(it.text) }
        spawnerBackField?.let { cfg.spawnerBackButtonNames = splitList(it.text) }
        spawnerPageField?.let { cfg.spawnerPageButtonNames = splitList(it.text) }
    }

    private fun splitList(raw: String): MutableList<String> =
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()

    // ---------------------------------------------------------------- render

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Dark translucent overlay background
        ctx.fill(0, 0, width, height, 0x99000000.toInt())

        // Card Container background
        val cX = width / 2 - cardWidth / 2
        val cY = 10
        ctx.fill(cX, cY, cX + cardWidth, cY + cardHeight + 40, 0xE012121D.toInt())
        ctx.fill(cX, cY, cX + cardWidth, cY + 2, 0xFFBB86FC.toInt()) // top accent

        // Title Header
        ctx.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("§d§lLovelyAutoDrop §7Configuration"),
            width / 2, 16, 0xFFFFFF
        )

        super.render(ctx, mouseX, mouseY, delta)

        // Draw captions dynamically
        for (cap in captions) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(cap.text), cap.x, cap.y, 0xB0B0B0)
        }
    }

    override fun shouldPause(): Boolean = false

    override fun close() {
        applyFields()
        cfg.save()
        client?.setScreen(parent)
    }
}

