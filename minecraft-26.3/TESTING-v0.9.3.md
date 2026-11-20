# SkyBlock Inspector v0.9.3 — Minecraft 26.3 mouse input hotfix

Minecraft 26.3 uses SDL mouse button IDs: left=1, right=3. The 26.2 project uses its existing input handling and is intentionally unchanged.

## Check before replacing your stable JAR

- [ ] Gradle build successful for 26.3
- [ ] F9 settings controls respond to **left-click**
- [ ] Settings do not respond to right-click
- [ ] Opacity slider responds to left-click and drag
- [ ] F8 main HUD responds to left-click and drag
- [ ] F8 mining status responds to left-click and drag
- [ ] Size slider responds to left-click
- [ ] Both HUD positions and settings persist after restarting
- [ ] NPC, Garden, Mining HUD and F10 controls still work

Build is not verified here. Keep your known-good 26.3 v0.9.1 JAR as backup. No server-side changes.
