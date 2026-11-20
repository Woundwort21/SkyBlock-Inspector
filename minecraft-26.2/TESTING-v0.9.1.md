# v0.9.1 testing checklist

Check each item Pass / Issue / Not Tested:

- [ ] 26.3 source compiles and loads in a matching Fabric 26.3 profile.
- [ ] 26.2 source compiles and loads in a matching Fabric 26.2 profile (if available).
- [ ] F10 cycles mining modes correctly.
- [ ] F10 status remains visible for approximately eight seconds.
- [ ] F10 status appears even when not looking at a block.
- [ ] F8 allows dragging the mining status **independently** of the inspector tooltip.
- [ ] Mining-status position is retained after restarting Minecraft.
- [ ] Crosshair tooltip positioning, 25–150% scaling and opacity still work.
- [ ] Automatic Dwarven Mines identification still works.
- [ ] Garden crops / Wild Rose, Sam / Duke, Mithril / Titanium still work.
- [ ] No crashes or significant FPS loss.
- [ ] Test alongside one other HUD mod and record its full name, Minecraft version and the exact problem.
- [ ] Verify clean CurseForge instance with correct version of Fabric API.

Keep the previous v0.9.0 RC1 JAR as a backup. Different Minecraft versions require separately compiled JARs. Gemstone/Crystal Hollows detection remains experimental.
