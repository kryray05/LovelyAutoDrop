# LovelyAutoDrop

> **Client-Side Automation & Loot Management Mod for Minecraft 1.21.11**  
> *Engineered for high-efficiency SMP order deliveries and spawner farming (Skeleton & Blaze) with built-in humanization and anti-kick safety nets.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?style=for-the-badge&logo=minecraft)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue?style=for-the-badge&logo=fabric)
![Lunar Client](https://img.shields.io/badge/Compatible-Lunar%20Client-00D2FF?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.4.10-purple?style=for-the-badge&logo=kotlin)
![Side](https://img.shields.io/badge/Side-Client--Only-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

---

## Overview

**LovelyAutoDrop** is an ultra-fast, highly configurable client-side Fabric mod designed for Minecraft 1.21.11. It automates container loot collection and server economy order hand-ins (`/orders`).

Built natively with **Fabric** and **Kotlin**, **LovelyAutoDrop** runs 100% client-side with **zero server-side plugins** and zero Mixins required, maintaining full compatibility with both **Vanilla Fabric** and **Lunar Client**.

### Key Highlights
- **Automated `/orders` Hand-in:** Multi-stage GUI navigation that automatically locates, claims, and shift-clicks matching inventory items into order screens.
- **Spawner Loot Collector (Skeleton & Blaze):** Supports container loot extraction (GUI mode) and direct world item dropping (Inventory mode).
- **Blocked Item Guard System:** Scans container pages *before* sending click packets. If unwanted items (e.g., `minecraft:arrow`, `minecraft:glowstone_dust`) appear, it immediately executes your configured rule (`STOP`, `SKIP`, or `PAUSE`).
- **Humanized Click Engine:** Configurable click delays, randomized jitter ticks, and settlement timing to emulate organic player clicks and prevent server anti-cheat kicks.
- **In-Game GUI & Command System:** Instant configuration screen accessible via keybind or `/lad`, live status HUD, and real-time chat feedback.

---

## Key Features

### 1. Multi-Stage Order Delivery (`/orders`)
Triggered via keybind (**`O`**) or `/lad orders`:
1. Sends the configured order command (default: `/orders`).
2. Automatically navigates multi-page server GUIs:
   - **Main Orders Menu:** Clicks *"ORDER CỦA BẠN"* button or direct order item.
   - **Order Selection:** Matches target order items (e.g., `bone`, `blaze_rod`).
   - **Edit Order Screen:** Clicks *"NHẬN"* / *"GIAO HÀNG"* button.
   - **Collect / Deliver Screen:** Clicks *"DROP ALL"* / *"GIAO TẤT CẢ"*, shift-clicks inventory items, and navigates next-page arrows (`>`) until all pages are completed.
3. Supports full diacritic and formatting-insensitive title matching (supports both English and Vietnamese server setups).
4. Auto-closes GUI upon completion when `ordersCloseWhenDone` is enabled.

### 2. Spawner Loot Collector & Automated Loop
Triggered via keybind (**`K`**) or `/lad spawner`:
- **GUI Mode:** Automates clicking inside open spawner chest or container screens (e.g., clicking *"KHO CHỨA"*, auto-triggering *"SELL ALL"* on garbage drops, or collecting target loot).
- **Inventory Mode:** Drops allowed items directly from player inventory into the world while maintaining a customizable reserve (`spawnerKeepAmount`).
- **Automated Spawner Loop Routine:** Triggered via keybind (**`J`**), `/lad loop`, or the in-game config panel (`Auto Loop` tab). Automatically waits for a configurable interval (30 to 60 minutes default), right-clicks the spawner block directly in front of the player, and executes the drop & sell routine repeatedly on a timer.

#### The Blocked Item Guard System
Before **every single click**, the mod scans all visible container slots. If a blocked item is detected, the mod executes your specified `spawnerBlockAction`:

| Action | Behaviour |
| :--- | :--- |
| `STOP` *(default)* | Immediately halts the task — safest option to prevent unwanted loot clogging or packet spam. |
| `SKIP` | Ignores blocked slots and continues extracting/dropping allowed slots. |
| `PAUSE` | Temporarily pauses clicking until the container screen updates, then automatically resumes. |

### 3. In-Game Config & Real-Time HUD Overlay
- **In-Game GUI:** Modify all settings live without restarting Minecraft, including dedicated tabs for General, Orders, Spawner, and Auto Loop settings.
- **HUD Overlay:** Renders a clean status box on-screen showing active task status, click counters, action notes, and live countdown timer when Auto Loop is active.
- **Chat Feedback:** Prints color-coded status messages when tasks start, finish, pause, or hit safety guards.

---

## Keybinds & Controls

All keybinds are registered under **Options → Controls → LovelyAutoDrop** and can be customized in-game:

| Keybind | Default Key | Action |
| :--- | :--- | :--- |
| **Open Config** | `P` | Opens the in-game GUI configuration screen |
| **Deliver Orders** | `O` | Triggers a single `/orders` delivery cycle |
| **Toggle Spawner** | `K` | Toggles the manual spawner loot collection task |
| **Toggle Auto Loop** | `J` | Toggles the 30-60m automated spawner right-click & loot loop |
| **Panic Stop** | `\` *(Backslash)* | Instantly halts all active automation tasks |

---

## Command Reference (`/lad`)

All subcommands run completely client-side:

```shell
/lad                    # Open the in-game configuration GUI
/lad orders             # Run the order delivery task once
/lad spawner            # Toggle the manual spawner task
/lad loop               # Toggle the automatic spawner right-click & loot timer loop
/lad autospawner        # Alias for /lad loop
/lad stop               # Emergency stop any running task
/lad status             # Output live task state, auto loop countdown, active filters
/lad reload             # Reload config from .minecraft/config/lovelyautodrop.json
/lad delay <ticks>      # Adjust click delay on the fly (1 tick = 50ms)
/lad block list         # List currently blocked items
/lad block add <item>   # Add an item pattern to the blocked items list
/lad block del <item>   # Remove an item pattern from the blocked items list
```

---

## Item Pattern Matching Syntax

Flexible pattern matching rules apply across all item filter lists (`ordersItems`, `spawnerAllowItems`, `spawnerBlockItems`, `spawnerSellTriggerItems`):

| Syntax | Match Rule | Example |
| :--- | :--- | :--- |
| **Exact Registry ID** | `namespace:item` | `minecraft:bone` |
| **Implicit Namespace** | `item` | `bone` (resolves to `minecraft:bone`) |
| **Wildcard Match** | `*pattern*` | `*bone*` (matches `minecraft:bone`, `minecraft:bone_meal`, etc.) |
| **Display Name Match**| `@Display Name` | `@Ancient Bone` (matches stack display name, formatting & accent-insensitive) |

---

## Safety & Anti-Detection Features

- **Single-Task Lock:** Prevents running multiple tasks simultaneously to avoid click packet desync or server kicks.
- **Damage Panic Stop:** Halts automation instantly if your player takes damage (`stopOnDamage`).
- **Screen Change Protection:** Aborts active tasks if an unexpected GUI screen opens (`stopOnScreenChange`).
- **Hotbar Slot 9 Protection:** Protects slot 9 (index 8) so pickaxes, weapons, or food items are never accidentally handed in or dropped.
- **Custom Item Guard:** Skips renamed items or stacks with lore/custom components (`ordersSkipNamedItems`, `spawnerSkipNamedItems`) to protect rare equipment.
- **Vanilla Packet Normalization:** Standard `SlotActionType` packets routed through native interaction handlers to match legitimate player mouse clicks.

---

## Configuration File (`lovelyautodrop.json`)

The config file is located at `.minecraft/config/lovelyautodrop.json` and automatically updates whenever settings are saved in-game.

<details>
<summary><b>Click to expand default configuration JSON</b></summary>

```json
{
  "masterEnabled": true,
  "chatFeedback": true,
  "hudEnabled": true,
  "hudX": 4,
  "hudY": 4,
  "clickDelayTicks": 1,
  "jitterTicks": 0,
  "settleTicks": 0,
  "stopOnDamage": true,
  "stopOnScreenChange": true,
  "maxClicksPerRun": 0,
  "taskTimeoutSeconds": 200,
  "guiIdleTimeoutTicks": 100,
  "ordersEnabled": true,
  "ordersCommand": "orders",
  "ordersOpenWaitTicks": 40,
  "ordersItems": [
    "minecraft:bone",
    "minecraft:blaze_rod"
  ],
  "ordersTitleMatch": [
    "order",
    "orders",
    "deliver",
    "giao",
    "collect"
  ],
  "ordersUseShiftClick": true,
  "ordersCloseWhenDone": true,
  "ordersProtectLastHotbarSlot": true,
  "ordersSkipNamedItems": true,
  "spawnerEnabled": true,
  "spawnerGuiMode": true,
  "spawnerInventoryMode": false,
  "spawnerTitleMatch": [
    "spawner",
    "skeleton",
    "blaze",
    "loot",
    "lồng sinh sản",
    "kho chứa",
    "storage"
  ],
  "spawnerAllowItems": [
    "minecraft:bone",
    "minecraft:blaze_rod"
  ],
  "spawnerBlockItems": [
    "minecraft:arrow",
    "minecraft:glowstone_dust"
  ],
  "spawnerBlockAction": "STOP",
  "spawnerCloseWhenDone": false,
  "spawnerKeepAmount": 0,
  "spawnerProtectLastHotbarSlot": true,
  "spawnerSkipNamedItems": true
}
```

</details>

---

## Installation Guide

### Option A: Vanilla Fabric
1. Install **Fabric Loader** (version `0.19.3` or newer) for **Minecraft 1.21.11**.
2. Download and place the following `.jar` files into `.minecraft/mods/`:
   - `lovelyautodrop-1.0.0.jar`
   - [Fabric API](https://modrinth.com/mod/fabric-api) (`0.141.5+1.21.11` or compatible)
   - [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) (`1.13.13+` or compatible)
3. Launch Minecraft.

### Option B: Lunar Client
1. Open Lunar Client launcher, create/select a **1.21.11** profile with the **Fabric** mod loader selected.
2. Go to **Version Settings → Mods → Open Mods Folder**.
3. Copy all **3 JAR files** (`lovelyautodrop`, `fabric-api`, `fabric-language-kotlin`) into the mods folder.
4. Launch Lunar Client.
5. *Sodium Conflict Fix:* If Lunar reports a crash or conflict with Sodium, upgrade or replace Lunar's bundled Sodium version so it is **0.8.13+mc1.21.11** or newer (older Sodium `0.8.2` builds break when loaded alongside Reese's Sodium Options / Sodium Extra).

---

## Building from Source

### Prerequisites
- **JDK 21** installed and configured in your environment (`JAVA_HOME`).

### Build Steps

```bash
# Clone the repository
git clone https://github.com/LovelyMod/LovelyAutoDrop.git
cd LovelyAutoDrop

# Build the mod executable JAR
./gradlew build
```

Upon successful compilation, the built mod artifact will be located at:
```
build/libs/lovelyautodrop-1.0.0.jar
```

---

## License & Disclaimer

- **License:** Distributed under the [MIT License](LICENSE).
- **Disclaimer:** Automation utilities may be subject to specific server rules on public SMP servers. Always consult server guidelines before using automated delivery features.
