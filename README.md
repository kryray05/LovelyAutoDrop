# ⚡ LovelyAutoDrop

> **Client-Side Automation & Loot Management Mod for Minecraft 1.21.11**  
> *Engineered for high-efficiency SMP order deliveries and spawner farming (Skeleton & Blaze) with safety nets.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?style=for-the-badge&logo=minecraft)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue?style=for-the-badge&logo=fabric)
![Lunar Client](https://img.shields.io/badge/Compatible-Lunar%20Client-00D2FF?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin)
![Side](https://img.shields.io/badge/Side-Client--Only-orange?style=for-the-badge)

---

## 📖 Overview

**LovelyAutoDrop** is a client-side Minecraft mod designed to streamline container loot collecting and automated server economy order handing-in (`/orders`). Built natively with **Fabric** and **Kotlin**, it requires **zero server-side plugins** and zero Mixins, ensuring full compatibility with **Vanilla Fabric** and **Lunar Client**.

### Key Highlights
- 📦 **Automated `/orders` Hand-in:** Automatically opens, matches, shift-clicks, and delivers exact item stacks to order GUIs.
- 💀 **Spawner Looting (Skeleton & Blaze):** Supports container loot extraction & direct inventory item dropping.
- 🛡️ **Anti-Kick Safety Net:** Scanning system checks every GUI page *before* sending clicks. If blocked items (e.g. `minecraft:arrow`, `minecraft:glowstone_dust`) appear, it immediately stops or pauses.
- ⏱️ **Humanized Click Dynamics:** Custom click delays, randomized tick jitters, and server acknowledgment timing to keep click packets looking natural.
- ⚙️ **In-Game GUI & Commands:** Rebindable keys and full modular setup with `Right Shift` or `/lad`.

---

## ✨ Features

### 1. Automated Order Delivery (`/orders`)
Triggered via **`O`** key or `/lad orders`:
1. Sends the configured order command (default: `orders`).
2. Waits for the container GUI and validates the screen title.
3. Automatically shift-clicks matching items (e.g. `bone`, `blaze_rod`) out of your inventory.
4. Auto-closes the GUI upon completion and safely stops.
5. *Never re-opens or loops automatically — one press, one delivery run.*

### 2. Spawner Loot Collector (Skeleton / Blaze)
Toggleable via **`K`** key or `/lad spawner`:
- **GUI Mode:** Extracts allowed items from open spawner chest/GUI screens.
- **Inventory Mode:** Drops allowed items directly from your player inventory into the world while keeping a customizable reserve (`spawnerKeepAmount`).

#### The Blocked Item Guard System
Before **every single click**, the mod scans container slots. If a blocked item appears, the mod executes your configured `On blocked item` rule:

| Action | Behaviour |
| :--- | :--- |
| `STOP` *(default)* | Immediately halts the task — safest option to prevent unwanted loot buildup. |
| `SKIP` | Ignores blocked slots and continues extracting allowed slots. |
| `PAUSE` | Temporarily halts clicking until the container page updates, then resumes. |

---

## 🎮 Controls & Keybinds

| Keybind | Default Key | Action |
| :--- | :--- | :--- |
| **Open Config** | `Right Shift` | Opens the modern in-game configuration screen |
| **Deliver Orders** | `O` | Triggers a single `/orders` delivery cycle |
| **Toggle Spawner** | `K` | Toggles the spawner loot collection task |
| **Panic Stop** | `\` | Instantly halts all active automation tasks |

> *All keybinds can be rebound in **Options → Controls → LovelyAutoDrop**.*

---

## 💬 Command Reference (`/lad`)

All `/lad` subcommands run client-side:

```shell
/lad                    # Open the configuration screen
/lad orders             # Execute order delivery task
/lad spawner            # Toggle spawner loot task
/lad stop               # Emergency stop all active tasks
/lad status             # Output live task state and current item lists
/lad reload             # Reload config from .minecraft/config/lovelyautodrop.json
/lad delay <ticks>      # Set base click delay (1 tick = 50ms)
/lad block list         # List currently blocked items
/lad block add <item>   # Add an item to the blocked items list
/lad block del <item>   # Remove an item from the blocked items list
```

---

## 🎯 Item Pattern Syntax

Flexible pattern matching syntax supported across all item lists (`ordersItems`, `spawnerAllowItems`, `spawnerBlockItems`):

| Syntax | Match Pattern | Example |
| :--- | :--- | :--- |
| **Exact Registry ID** | `namespace:item` | `minecraft:bone` |
| **Implicit Namespace** | `item` | `bone` (automatically resolves to `minecraft:bone`) |
| **Wildcard Match** | `*pattern*` | `*bone*` (matches `minecraft:bone`, `minecraft:bone_meal`, etc.) |
| **Display Name Match**| `@Display Name` | `@Ancient Bone` (matches stack display name) |

---

## 🛡️ Safety & Anti-Detection Features

- **Single-Task Lock:** Only one task runs at a time to prevent double-clicking or packet desync kicks.
- **Damage Panic Stop:** Halts immediately if your player takes damage.
- **Screen Change Protection:** Aborts task if an unexpected GUI or screen opens.
- **Hotbar Slot 9 Protection:** Ensures your pickaxe/food in slot 9 is never handed in or dropped.
- **Custom Item Guard:** Skips renamed items or stacks with lore to protect rare/custom SMP items.
- **Server Packet Normalization:** Clicks go through standard client interaction handlers matching vanilla mouse input.

---

## 🛠️ Installation Guide

### Option A: Vanilla Fabric
1. Install **Fabric Loader** for Minecraft **1.21.11**.
2. Place the following `.jar` files into `.minecraft/mods/`:
   - `lovelyautodrop-1.0.0.jar`
   - [Fabric API](https://modrinth.com/mod/fabric-api) (`0.141.5+1.21.11` or compatible)
   - [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) (`1.13.13` or higher)

### Option B: Lunar Client
1. Open Lunar Client launcher, select **1.21.11**, and choose the **Fabric** loader option.
2. Open **Version Settings → Mods → Open Mods Folder**.
3. Copy all **3 JAR files** (`lovelyautodrop`, `fabric-api`, `fabric-language-kotlin`) into the mods folder.
4. Launch Lunar Client.

---

## 🔧 Building from Source

Requires **JDK 21**.

```bash
# Clone the repository
git clone https://github.com/LovelyMod/LovelyAutoDrop.git
cd LovelyAutoDrop

# Build the mod JAR
./gradlew build
```

The output build artifact will be located at:
`build/libs/lovelyautodrop-1.0.0.jar`

---

## 📄 License & Disclaimer

- **License:** MIT License
- **Disclaimer:** Automation features may violate specific server rules. Always review your server's policy on automation before use.
