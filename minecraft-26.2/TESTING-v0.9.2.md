# SkyBlock Inspector v0.9.2 — Mouse input hotfix

Target Minecraft: 26.2 (Fabric).

## Changes
- Settings toggles, background opacity slider, settings navigation buttons now use **left-click** (mouse button 0).
- F8 HUD drag, mining status drag, and HUD scale slider now use **left-click**.
- Right-click no longer activates those controls.
- Drag release only completes on the left-button release.
- All existing config keys and values are kept unchanged.

## Test checklist
- [ ] Gradle build succeeds and mod launches in 26.2.
- [ ] F9 settings switches toggle on **left-click**; right-click does not toggle.
- [ ] F9 opacity slider moves with **left-button drag**.
- [ ] F9 buttons open the HUD editor and vanilla keybind settings with left-click.
- [ ] F8 HUD preview moves with **left-button drag**.
- [ ] F8 mining-status preview moves independently with left-button drag.
- [ ] F8 HUD scale slider moves with left-button drag.
- [ ] HUD and mining-status positions persist after restarting Minecraft.
- [ ] Opacity, toggles, and scale persist after restarting.
- [ ] F10 mining notification and automatic mining detection still work.
- [ ] Garden crops, Wild Rose, NPC names, Mithril and Titanium still display correctly.
- [ ] No crashes, lag or conflicts with other installed SkyBlock mods.

**Build status:** Source-level patch prepared, not compiled or tested here. Build and test in IntelliJ before sharing publicly.
