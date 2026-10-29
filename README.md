# CloudVisuals

> A client-side **visuals combine** for Minecraft: several open-source visual mods merged into one, cleaned up, and controlled from a single menu.

![status](https://img.shields.io/badge/status-beta-orange)
![mc](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![loader](https://img.shields.io/badge/loader-Fabric-blue)
![license](https://img.shields.io/badge/license-GPL--3.0-blue)

CloudVisuals bundles the features of three separate visual mods into one Fabric mod. Everything is configured from one click-GUI opened with **Right Shift**. It is **client-side only** and works on vanilla servers.

---

## ⚠️ Beta

This is an early beta. Features are merged and the mod runs, but not every module has been tested in every situation. Expect rough edges and report what breaks. Development is active.

---

## What's inside

All of the following are modules in the Right Shift menu:

| Source mod | What it brings |
|------------|----------------|
| **[ZeroVisuals](https://github.com/mooncode2/ZeroVisual)** (core) | The click-GUI and HUD, ESP / hitboxes, TargetESP, jump circles, trails, player arrows, projectile trajectory, china hat, particles, item physics, custom fog & sky, night vision, view model, cosmetics, zoom and more |
| **[SimpleVisuals](https://github.com/Ouzzi/simplevisuals)** | Player Locator, Chat Heads, Effect Bars, Elytra Pitch, Speed Lines, Pickup Notifier, Damage Numbers, Biome Info, Item Tooltips, Map Tooltips, Renamed Textures, Death Coords, Anvil Colors |
| **[Motionsuite](https://github.com/0r3vy/Motionsuite)** | Motion Blur (frame-rate independent, capped at a safe strength) and Smooth Camera |

Ideas ported from **[LiquidBounce](https://github.com/CCBlueX/LiquidBounce)** (visual features only, rewritten for this codebase):

- **No Render** options: block-in-face, pumpkin, portal and spyglass overlays, hurt-cam shake, view bobbing
- **No Swing** (hides only your own swing animation), **Static FOV**, **Rear View** (hold a key for a third-person view)
- **HUD editor** (open chat and drag elements): 10px grid, magnetic snapping with guide lines, 3×3 anchor zones that keep elements in place when the window is resized, live coordinates; hold **Shift** to move freely

**Menu styles** (`Utils → Menu Style`): the classic **Zero** menu, a **Cheat Menu** with one draggable panel per category, or a **GTA**-style radial wheel.

Changes made during the merge:

- One menu for everything: SimpleVisuals and Motionsuite settings live in the Right Shift GUI instead of separate screens and chat commands.
- Privacy: the upstream client sent your nickname and server address to the ZeroVisuals author's server; that request is removed.
- Cosmetics fixed (they were drawn far away from the player because the camera offset was applied twice).
- Removed: kill aura and duplicated features, Motionsuite's VulkanMod path (the OpenGL motion blur remains), and classes the upstream ZeroVisuals source never published (music/lyrics HUD and a few UI helpers).
- Fixed: motion blur strength no longer depends on FPS, ghosting from old frames is cleared faster, an invalid OpenGL call that flooded the log every frame, and a synchronous GL debug callback that tanked FPS.

---

## Installation

1. Install **Fabric Loader** for Minecraft 1.21.11 and put **[Fabric API](https://modrinth.com/mod/fabric-api)** into your `mods` folder.
2. Download `cloudvisuals-<version>.jar` from **[Releases](../../releases)** and put it into `mods` as well. Cloth Config is bundled inside, nothing else is required.
3. Launch the game and press **Right Shift** to open the menu. All modules start disabled.

**Laptops with two GPUs:** if FPS is unexpectedly low, Minecraft is probably running on the integrated GPU. In Windows go to Settings → System → Display → Graphics, add your launcher's `javaw.exe` and set it to *High performance*.

---

## Building from source

You need **JDK 21**.

```powershell
git clone https://github.com/youturehhgames-ship-it/Cloud-Visuals
cd Cloud-Visuals
.\gradlew.bat build
```

The jar is written to `build\libs\cloudvisuals-<version>.jar`. Use the one **without** the `-sources` suffix.

---

## License

**GPL-3.0.** The core (ZeroVisuals) is GPL-3, and GPL requires the combined work to stay GPL-3. The other parts are compatible: SimpleVisuals is CC0 and Motionsuite is MIT. See [`LICENSE`](LICENSE).

## Credits

The original features were written by **mooncode2** (ZeroVisuals), **Ouzzi** (SimpleVisuals) and **0r3vy** (Motionsuite). Several visual features and the HUD editor behaviour are based on **[LiquidBounce](https://github.com/CCBlueX/LiquidBounce)** by CCBlueX (GPL-3.0). CloudVisuals merges, fixes and extends their work.
