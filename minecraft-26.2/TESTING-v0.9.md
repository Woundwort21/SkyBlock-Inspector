# SkyBlock Inspector v0.9.0-rc1 — QA Checklist

Mark each item **Pass / Issue / Not tested**. Attach screenshots for any issue.

## Build and clean install
- [ ] Gradle build succeeds (`build/libs` contains the non-sources JAR).
- [ ] New CurseForge Fabric 26.3 profile + Fabric API loads the mod without previous configuration.
- [ ] Project icon appears in Fabric-compatible mod viewers (where available).
- [ ] No duplicates of older SkyBlock Inspector jars are installed.

## Upgrade and settings recovery
- [ ] Upgrade from working v0.8 keeps previous HUD position, scale, opacity and toggles.
- [ ] F8 editor dragging and 25–150% scale slider still work.
- [ ] F9 settings opens; toggles, opacity and key rebindings still work.
- [ ] Settings persist through restart; F10 automatic mining mode still works.
- [ ] Corrupt *one* property (test in a backed-up copy only, e.g. `background_opacity=invalid`) and restart: other settings stay intact and no crash occurs.
- [ ] Restore original config after corruption test.

## Garden and NPCs
- [ ] Garden crops, Wild Rose label, icons and growth bars work.
- [ ] Sam and Duke display correct NPC names; no random entity usernames.
- [ ] Other nearby NPCs remain correctly identified.

## Mining
- [ ] Dwarven Mines automatically recognized; Mithril and Titanium labels/Breaking Power work.
- [ ] Ordinary blocks outside mining regions are not misidentified.
- [ ] F10 manual and Off modes still function.
- [ ] Crystal Hollows/gemstones: **not tested / experimental** unless access is available.

## Modpack compatibility and performance
- [ ] Install alongside other HUD mods (e.g., Jade, SkyHanni if compatible): no crash or major conflicts.
- [ ] F8/F9/F10 conflicts can be remapped in Controls.
- [ ] No significant FPS drop or tooltip flicker.
- [ ] No errors on switching worlds/servers or resizing window.

## Release checks
- [ ] README and changelog match the final JAR.
- [ ] Screenshot(s) represent actual gameplay; no simulated UI presented as gameplay.
- [ ] Confirm author display name, project URL, license and dependency list.
- [ ] Hypixel rules reviewed; no promise of guaranteed approval.
- [ ] CurseForge project marked Beta until public/independent testing is complete.
