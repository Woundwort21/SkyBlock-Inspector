# SkyBlock Inspector

A lightweight, client-side Minecraft Fabric information overlay for Hypixel SkyBlock, developed by **Woundwort**.

## Supported versions

| Minecraft | SkyBlock Inspector source | Status |
| --- | --- | --- |
| 26.2 | [`minecraft-26.2/`](minecraft-26.2/) — v0.9.2 | Tested, public Beta on CurseForge |
| 26.3 | [`minecraft-26.3/`](minecraft-26.3/) — v0.9.3 | Tested, public Beta on CurseForge |

These are separate build targets. **Do not install both JARs in one instance.**

## Features

- Crop names and growth details, including Wild Rose
- Hypixel NPC nameplate identification
- Mithril and Titanium information and breaking-power requirements
- Automatic mining-area detection with manual controls
- Configurable HUD: drag-and-drop, opacity, size slider, keybinds

Crystal Hollows gemstone identification is experimental and has not been fully tested.

## Installation

Install **SkyBlock Inspector** through the CurseForge app, using the file for your Minecraft version. **Fabric Loader** and **Fabric API** are required.

## Building from source

1. Install a Java 25-compatible development environment and Gradle.
2. Open the `minecraft-26.2` or `minecraft-26.3` folder as a separate Gradle project in IntelliJ IDEA.
3. Run the Gradle `build` task and use the installable JAR in `build/libs` (not the sources JAR).

The packaged source projects do not include a Gradle wrapper. Build dependencies are declared in their Gradle files.

## License

MIT License — Copyright (c) 2026 Woundwort. See [`LICENSE`](LICENSE).

Third-party projects and assets are subject to their own terms. Hypixel SkyBlock is not affiliated with or endorsing this mod. Client-side mods are used at the player's own risk on multiplayer servers.

## Reporting bugs

Use **GitHub Issues** and include the Minecraft version, mod version, steps to reproduce, and logs if possible. Please remove private account or server information from logs before sharing.
