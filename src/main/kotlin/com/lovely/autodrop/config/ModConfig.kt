package com.lovely.autodrop.config

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Everything in LovelyAutoDrop is configurable in-game.
 *
 * The file lives at  .minecraft/config/lovelyautodrop.json  and is written
 * every time you change a value in the config screen, so a crash never loses
 * your setup.
 */
data class ModConfig(

    // ----------------------------------------------------------------- global
    /** Master switch. When false nothing in the mod ever clicks. */
    var masterEnabled: Boolean = true,

    /** Print a chat line whenever a task starts / stops / aborts. */
    var chatFeedback: Boolean = true,

    /** Draw the small HUD status box while a task is running. */
    var hudEnabled: Boolean = true,

    var hudX: Int = 4,
    var hudY: Int = 4,

    // ------------------------------------------------------------- humanising
    /**
     * Base delay between two clicks, in game ticks (20 ticks = 1 second).
     * Set to 1 tick for fast spam clicking (20 clicks/sec).
     */
    var clickDelayTicks: Int = 1,

    /**
     * Random extra delay, 0..jitterTicks, added on top of [clickDelayTicks].
     * Set to 0 for maximum speed without jitter.
     */
    var jitterTicks: Int = 0,

    /** Extra pause after the GUI content changes before clicking again. */
    var settleTicks: Int = 0,

    // ------------------------------------------------------------ safety net
    /** Abort the running task if the player takes damage. */
    var stopOnDamage: Boolean = true,

    /** Abort if a player-sent chat message / command is detected from you. */
    var stopOnScreenChange: Boolean = true,

    /** Hard cap: abort a task after this many total clicks. 0 = unlimited. */
    var maxClicksPerRun: Int = 0,

    /** Abort the task if it runs for longer than this many seconds (0 = disabled). */
    var taskTimeoutSeconds: Int = 200,

    /** Abort / retry if the GUI does not respond for this many ticks. */
    var guiIdleTimeoutTicks: Int = 100,

    // -------------------------------------------------------------- /orders
    var ordersEnabled: Boolean = true,

    /** Command used to open the order list. Sent without the leading slash. */
    var ordersCommand: String = "orders",

    /** Ticks to wait for the /orders GUI to appear after sending the command. */
    var ordersOpenWaitTicks: Int = 40,

    /**
     * Items the order-delivery task is allowed to hand in.
     * Matched against the registry id of the stack in your inventory.
     */
    var ordersItems: MutableList<String> = mutableListOf(
        "minecraft:bone",
        "minecraft:blaze_rod",
    ),

    /**
     * Text that identifies the order GUI title (case-insensitive, substring).
     * The screenshot shows "Order của bạn", so both EN + VI are covered.
     */
    var ordersTitleMatch: MutableList<String> = mutableListOf(
        "order",
        "orders",
        "deliver",
        "giao",
        "collect",
    ),

    /** Keywords to match the 'ORDER CỦA BẠN' button in main order menu. */
    var ordersYourOrdersButtonNames: MutableList<String> = mutableListOf(
        "order của bạn",
        "order cua ban",
        "your orders",
        "your order",
        "đơn hàng của bạn",
        "don hang cua ban",
        "đơn hàng",
        "don hang",
        "tất cả đơn hàng",
        "tat ca don hang",
        "danh sách đơn hàng",
        "danh sach don hang",
        "giao hàng",
        "giao hang",
        "nhận/giao hàng",
        "nhan/giao hang",
    ),

    /** Keywords to match the 'NHẬN' / 'GIAO HÀNG' button in edit order menu. */
    var ordersClaimButtonNames: MutableList<String> = mutableListOf(
        "nhận",
        "nhan",
        "claim",
        "collect",
        "deliver",
        "giao",
        "giao hàng",
        "giao hang",
        "giao đơn",
        "giao don",
        "nhận đơn",
        "nhan don",
        "giao vật phẩm",
        "giao vat pham",
        "nhận vật phẩm",
        "nhan vat pham",
        "bấm để nhận",
        "bam de nhan",
        "bấm để giao",
        "bam de giao",
        "nộp",
        "nop",
        "trả",
        "tra",
        "xác nhận",
        "xac nhan",
        "thực hiện",
        "thuc hien",
        "chấp nhận",
        "chap nhan",
        "tiến hành",
        "tien hanh",
        "hoàn thành",
        "hoan thanh",
    ),

    /** Keywords to match the 'DROP ALL' button in collect items menu. */
    var ordersDropAllButtonNames: MutableList<String> = mutableListOf(
        "drop all",
        "drop_all",
        "deliver all",
        "deliver_all",
        "thả tất cả",
        "tha tat ca",
        "giao tất cả",
        "giao tat ca",
        "giao vật phẩm",
        "giao vat pham",
        "bấm để drop",
        "bấm để giao",
        "bam de giao",
        "drop hết",
        "giao hết",
    ),

    /** Keywords to match page navigation (Next Page) in collect items menu. */
    var ordersNextPageButtonNames: MutableList<String> = mutableListOf(
        "next",
        "trang",
        "tiếp",
        "tiep",
        "sau",
        "forward",
        ">",
    ),

    /**
     * How to move the items into the order.
     * QUICK_MOVE = shift-click (what almost every order plugin expects).
     */
    var ordersUseShiftClick: Boolean = true,

    /** Close the GUI automatically once there is nothing left to deliver. */
    var ordersCloseWhenDone: Boolean = true,

    /** Keep the hotbar slot 9 (index 8) untouched — handy for a pickaxe/food. */
    var ordersProtectLastHotbarSlot: Boolean = true,

    /** Never deliver a stack that is renamed / has custom lore (rare drops). */
    var ordersSkipNamedItems: Boolean = true,

    // -------------------------------------------------------------- spawner
    var spawnerEnabled: Boolean = true,

    /**
     * GUI mode: click loot slots inside an open spawner-loot screen.
     * Inventory mode: drop matching stacks straight out of your inventory.
     * Both can be on at once.
     */
    var spawnerGuiMode: Boolean = true,
    var spawnerInventoryMode: Boolean = false,

    /** Titles that mark a spawner-loot screen. */
    var spawnerTitleMatch: MutableList<String> = mutableListOf(
        "spawner",
        "skeleton",
        "blaze",
        "loot",
        "lồng sinh sản",
        "kho chứa",
        "storage",
    ),

    /** Keywords to match the 'KHO CHỨA' button in the main spawner menu. */
    var spawnerStorageButtonNames: MutableList<String> = mutableListOf(
        "kho chứa",
        "kho chua",
        "storage",
    ),

    /** Keywords to match the 'Drop Loot' button in spawner storage menu. */
    var spawnerDropLootButtonNames: MutableList<String> = mutableListOf(
        "drop loot",
        "drop_loot",
        "thả vật phẩm",
        "drop all",
        "thả tất cả",
        "xả kho",
        "xả đồ",
        "thả đồ",
        "drop",
        "click to drop",
    ),

    /** Keywords to match the 'SELL ALL' button in spawner storage menu. */
    var spawnerSellAllButtonNames: MutableList<String> = mutableListOf(
        "sell all",
        "sell_all",
        "bán tất cả",
        "bán đồ",
        "bán rác",
        "bán hết",
        "sell",
        "click to sell",
    ),

    /** Keywords to match 'GO BACK' or navigation buttons in spawner storage menu. */
    var spawnerBackButtonNames: MutableList<String> = mutableListOf(
        "quay lại",
        "quay lai",
        "trở về",
        "tro ve",
        "go back",
        "back",
        "trở lại",
        "tro lai",
        "exit",
        "close",
        "thoát",
    ),

    /** Keywords to match page navigation buttons in spawner storage menu (e.g. Next Page / Prev Page). */
    var spawnerPageButtonNames: MutableList<String> = mutableListOf(
        "trang",
        "page",
        "next",
        "prev",
        "previous",
        "tiếp",
        "tiep",
        "sau",
        "trước",
        "truoc",
        "forward",
        "backward",
        ">",
        "<",
        "->",
        "<-",
    ),

    /** Item IDs or wildcards that trigger clicking 'SELL ALL' instead of 'Drop Loot'. */
    var spawnerSellTriggerItems: MutableList<String> = mutableListOf(
        "minecraft:arrow",
        "arrow",
        "minecraft:glowstone_dust",
        "glowstone_dust",
        "dust",
    ),

    /** Items we WANT to collect / drop from the spawner screen. */
    var spawnerAllowItems: MutableList<String> = mutableListOf(
        "minecraft:bone",
        "bone",
        "minecraft:blaze_rod",
        "blaze_rod",
        "blaze",
        "rod",
        "rotten_flesh",
        "flesh",
        "gunpowder",
        "string",
        "spider_eye",
        "ender_pearl",
        "pearl",
        "iron_ingot",
        "gold_nugget",
        "gold_ingot",
    ),

    /**
     * THE IMPORTANT ONE.
     * If any of these show up on the current page, the task stops clicking.
     * Arrow + glowstone dust are the secondary skeleton/blaze drops you do
     * not want to burn clicks on.
     */
    var spawnerBlockItems: MutableList<String> = mutableListOf(
        "minecraft:arrow",
        "minecraft:glowstone_dust",
    ),

    /**
     * What a blocked item does:
     *  STOP  - halt the whole task immediately (your requested behaviour)
     *  SKIP  - leave that slot alone but keep working the other slots
     *  PAUSE - stop clicking, wait for the page to change, then resume
     */
    var spawnerBlockAction: BlockAction = BlockAction.STOP,

    /** Also refuse to click a slot whose *name/lore* contains these words. */
    var spawnerBlockKeywords: MutableList<String> = mutableListOf(),

    /** Close the spawner GUI when the task stops. */
    var spawnerCloseWhenDone: Boolean = false,

    /** In inventory mode, keep this many of each allowed item. */
    var spawnerKeepAmount: Int = 0,

    /** Keep the hotbar slot 9 (index 8) untouched in spawner mode. */
    var spawnerProtectLastHotbarSlot: Boolean = true,

    /** Never drop or collect a stack that is renamed / has custom lore. */
    var spawnerSkipNamedItems: Boolean = true,

    // -------------------------------------------------- auto spawner timer loop
    /** Master toggle for periodic automatic spawner right-click + drop/sell loop. */
    var autoSpawnerEnabled: Boolean = false,

    /** Minimum interval delay in minutes (default 15m). */
    var autoSpawnerMinMinutes: Int = 15,

    /** Maximum interval delay in minutes (default 30m). */
    var autoSpawnerMaxMinutes: Int = 30,

    /** Automatically right-click the spawner block directly in front of player before running drop/sell. */
    var autoSpawnerInteractBeforeLoot: Boolean = true,

    /** Automatically parse spawner stats from GUI tooltip and adjust loop timer interval. */
    var autoSpawnerAutoAdjust: Boolean = true,

    /** Target storage capacity item count used to calculate optimal fill time (default 320 items = 5 slots * 64). */
    var autoSpawnerTargetItems: Int = 320,
) {

    enum class BlockAction { STOP, SKIP, PAUSE }

    fun save() {
        try {
            Files.createDirectories(PATH.parent)
            Files.newBufferedWriter(PATH).use { GSON.toJson(this, it) }
        } catch (e: Exception) {
            System.err.println("[LovelyAutoDrop] Failed to save config: ${e.message}")
        }
    }

    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().create()

        private val PATH: Path
            get() = FabricLoader.getInstance().configDir.resolve("lovelyautodrop.json")

        @Volatile
        private var instance: ModConfig? = null

        val INSTANCE: ModConfig
            get() = instance ?: synchronized(this) {
                instance ?: load().also { instance = it }
            }

        private fun load(): ModConfig {
            return try {
                if (Files.exists(PATH)) {
                    Files.newBufferedReader(PATH).use {
                        GSON.fromJson(it, ModConfig::class.java) ?: ModConfig()
                    }
                } else {
                    ModConfig().also { it.save() }
                }
            } catch (e: Exception) {
                System.err.println("[LovelyAutoDrop] Bad config, using defaults: ${e.message}")
                ModConfig()
            }
        }

        /** Re-read from disk (used by the /lad reload command). */
        fun reload() {
            synchronized(this) { instance = load() }
        }
    }
}
