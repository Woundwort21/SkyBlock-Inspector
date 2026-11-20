# SkyBlock Inspector — v0.9.0-rc1

Lightweight **client-side Fabric mod** for Minecraft **26.3**. Shows a compact compact crosshair tooltip for blocks, Garden crops, nearby NPC names and possible mining ores on Hypixel SkyBlock.

## Features

- Vanilla block names and item icons; growing crop percentages and bars **only when the client exposes an age property**.
- SkyBlock crop labels including a **possible** Wild Rose visual match.
- Hypixel NPC nameplate matching using nearby server-visible custom-named entities. Confirmed by the project tester with Sam and Duke.
- Dwarven Mines Mithril/Titanium visual candidates and Breaking Power; experimental gemstone candidates.
- Automatic mining-region recognition using scoreboard/TAB text, with F10 manual overrides.
- F8 draggable, scalable (25–150%) overlay; F9 settings including display toggles, opacity and native Minecraft keybinding screen.
- F7 optional NPC diagnostics; files may include nearby player usernames, review before sharing.
- Only reads data already available to the client; no custom packets, automation or movement changes.

## Controls

| Default key | Action |
|---|---|
| F7 | Save NPC diagnostics |
| F8 | Position/resize HUD |
| F9 | Open settings |
| F10 | Cycle mining area mode |

Rebind controls through Minecraft's keybind settings, accessible from the mod's settings screen. The HUD is visible only when connected to Hypixel domains; that is **not** a guarantee the player is in SkyBlock.

## Install in CurseForge

1. Create/select a **Fabric Minecraft 26.3** profile.
2. Install compatible **Fabric API** into that profile.
3. Add the built `skyblock-inspector-0.9.0-rc1.jar` to the profile's `mods` directory. **Do not install the `-sources.jar`.**
4. Remove older SkyBlock Inspector JARs; use only one version.
5. Launch Minecraft. Press F9 for settings and F8 for HUD layout.

No NEU, SkyHanni or Enhanced SkyRecipes dependency is required.

## Build from source

Open this directory containing `build.gradle` in IntelliJ IDEA, select the required Java toolchain (Java 25) and run the Gradle `build` task. The project does **not** bundle Gradle wrapper files, so IntelliJ requires its configured Gradle installation. The built JAR is in `build/libs/`.

**Build status:** The v0.9 release candidate has not been compiled or play-tested in this environment. Please test using `TESTING-v0.9.md` before any public release.

## Settings and backups

Settings file: `config/skyblock-inspector.properties` in the CurseForge profile. Existing v0.8 settings are preserved; malformed individual values now fall back independently to defaults. Minecraft keybinds remain in the profile's `options.txt`. Back up these files and the working v0.8 JAR before installing RC1.

## Limitations & safety

- **Gemstones/Crystal Hollows/Glacite mapping has not been validated by project testing.** Treat it as experimental.
- Mining and Wild Rose identification is based on visible block shapes, colours and locations; it is **not a definitive SkyBlock item ID**.
- Minecraft growth age may not represent Hypixel's special growth mechanics.
- Hypixel does not endorse this mod; use any client modification at your own risk. See <https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications>.
- Project is licensed under MIT; retain the LICENSE notice when distributing source or derivatives. Creator should review authorship, original asset rights, and public metadata before publishing.

## Next steps before public release

Complete the test checklist, conduct clean-instance and modpack coexistence testing, add real gameplay screenshots, confirm CurseForge project author and links, and upload the compiled JAR to CurseForge as **Beta** if tests pass. No CurseForge project or upload has been created by this source package.


## v0.9.1 note
Press F10 to show an eight-second mining mode status panel. Open the F8 HUD editor to drag the status panel independently from the crosshair tooltip; both positions are saved. Two source profiles are supplied, one for Minecraft 26.3 and one for 26.2. Each profile needs its own Gradle build and matching Fabric API.
