<div align="center">

# 🛡️ Aegis

**Advanced Region Management System for Allay**

![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Version](https://img.shields.io/badge/Version-1.0.1-green.svg)
![Platform](https://img.shields.io/badge/Platform-Allay-orange.svg)

<br>

[![Русский](https://img.shields.io/badge/Язык-Русский-red?style=for-the-badge&logo=google-translate&logoColor=white)](README_RU.md)

</div>

---

**Aegis** is a powerful and lightweight region management plugin designed specifically for **Allay** servers. It provides an intuitive GUI and command-based system to protect your world from unauthorized changes.

### ✨ Features
- **Region Protection**: Prevent block breaking, placing, and interactions within defined areas.
- **GUI Driven**: User-friendly form menus for managing regions without memorizing complex commands.
- **Visual Selection**: Use a wand (Wooden Axe) to easily select region corners.
- **Multi-language Support**: Fully localized in English (`en_US`) and Russian (`ru_RU`).
- **Performance**: Optimized for Allay to ensure minimal impact on server performance.

### 🎮 Commands
| Command | Alias | Description |
|---------|-------|-------------|
| `/aegis menu` | `/rg menu` | Opens the main GUI menu. |
| `/aegis wand` | `/rg wand` | Gives you the region selection wand. |
| `/aegis create <name>` | `/rg create` | Creates a region with the selected points. |
| `/aegis delete <name>` | `/rg delete` | Deletes an existing region. |
| `/aegis info` | `/rg info` | Gives you the region info tool. |
| `/aegis flag <region> <flag> <value>` | `/rg flag` | Sets a flag for the region. |
| `/aegis addmember <region> <player>` | `/rg addmember` | Adds a member to the region. |
| `/aegis removemember <region> <player>` | `/rg removemember` | Removes a member from the region. |

### 🛠️ Usage
1.  **Get the Wand**: Run `/aegis wand`.
2.  **Select Area**: 
    -   **Left-click** a block to set Position 1.
    -   **Right-click** a block to set Position 2.
3.  **Create Region**: Run `/aegis create MyRegion` or use the menu.
4.  **Done!** Your area is now protected.

### 🚩 Flags
Aegis supports various flags to customize region protection:
- `build`: Allow/Deny placing or breaking blocks. (Default: `false` for non-members)
- `pvp`: Allow/Deny PvP combat.
- `entry`: Allow/Deny entry into the region.
- `exit`: Allow/Deny exiting the region.
- `chest-access`: Allow/Deny access to chests and containers.
- `use`: Allow/Deny interaction with blocks (buttons, levers, etc).
- `chat`: Allow/Deny chatting within the region.

### 🔐 Permissions
| Permission | Description |
|------------|-------------|
| `aegis.admin` | Full access to all Aegis commands and bypasses protection. |
| `aegis.flag.<name>` | Permission to manage a specific flag (e.g. `aegis.flag.pvp`). |

---

<div align="center">
    <br>
    <p>Created by <b>Miroshka</b> specifically for <b>Allay</b> with ❤️</p>
</div>
