package com.skyblockinspector;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Comparator;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Read-only crosshair HUD: no packets, world changes, automations, or movement changes.
 * Hypixel-specific names are heuristic because a remote server can reuse vanilla blocks.
 */
public final class SkyBlockInspectorClient implements ClientModInitializer {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("skyblock-inspector.properties");
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("skyblockinspector", "controls"));

    private static int BG = 0xE1101010;
    private static final int BORDER_LIGHT = 0xFF5D6A79;
    private static final int BORDER_DARK = 0xFF1E252D;
    private static final int TITLE = 0xFFF5F7FA;
    private static final int SUBTITLE = 0xFFB9C4D0;
    private static final int ACCENT = 0xFF70C8FF;
    private static final int SUCCESS = 0xFF7DE69A;
    private static final int WARNING = 0xFFFFD37A;

    private static final Map<String, String> GEMSTONES = Map.of(
        "red", "Ruby",
        "orange", "Amber",
        "yellow", "Topaz",
        "lime", "Jade",
        "light_blue", "Sapphire",
        "purple", "Amethyst",
        "magenta", "Jasper",
        "white", "Opal"
    );

    private static final Map<String, String> CROP_NAMES = Map.ofEntries(
        Map.entry("wheat", "Wheat"),
        Map.entry("carrots", "Carrot"),
        Map.entry("potatoes", "Potato"),
        Map.entry("beetroots", "Beetroot"),
        Map.entry("nether_wart", "Nether Wart"),
        Map.entry("cocoa", "Cocoa Beans"),
        Map.entry("sweet_berry_bush", "Sweet Berries"),
        Map.entry("melon_stem", "Melon"),
        Map.entry("attached_melon_stem", "Melon"),
        Map.entry("pumpkin_stem", "Pumpkin"),
        Map.entry("attached_pumpkin_stem", "Pumpkin"),
        Map.entry("sugar_cane", "Sugar Cane"),
        Map.entry("cactus", "Cactus"),
        Map.entry("brown_mushroom", "Mushroom"),
        Map.entry("red_mushroom", "Mushroom")
    );

    private static KeyMapping editKey;
    private static KeyMapping inspectNpcKey;
    private static KeyMapping miningModeKey;
    private static KeyMapping settingsKey;
    private static boolean showHud = true;
    private static boolean showNpcs = true;
    private static boolean showBlocks = true;
    private static boolean showCropGrowth = true;
    private static boolean showMining = true;
    private static int backgroundOpacity = 88;
    // AUTO reads only server-visible scoreboard/tab-list data. Manual modes are overrides.
    private static MiningMode miningMode = MiningMode.AUTO;
    private static MiningArea detectedArea = MiningArea.OFF;
    private static int detectionTick = 0;
    private static boolean locationRecognized = false;

    private enum MiningMode {
        AUTO("Automatic"), OFF("Off"), DWARVEN("Dwarven Mines (manual)"),
        HOLLOWS("Crystal Hollows (manual)"), GLACITE("Glacite (manual)");
        final String label;
        MiningMode(String label) { this.label = label; }
        MiningMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private static MiningArea activeArea() {
        return switch (miningMode) {
            case AUTO -> detectedArea;
            case OFF -> MiningArea.OFF;
            case DWARVEN -> MiningArea.DWARVEN;
            case HOLLOWS -> MiningArea.HOLLOWS;
            case GLACITE -> MiningArea.GLACITE;
        };
    }

    private enum MiningArea {
        OFF("Off"), DWARVEN("Dwarven Mines"), HOLLOWS("Crystal Hollows"), GLACITE("Glacite Tunnels/Mineshafts");
        final String label;
        MiningArea(String label) { this.label = label; }
        MiningArea next() { return values()[(ordinal() + 1) % values().length]; }
    }
    private static double hudXPercent = 50;
    private static double hudYPercent = 55;
    private static double hudScale = 1.0;
    // An independently positioned temporary HUD, visible for eight seconds after F10.
    private static double miningStatusXPercent = 50;
    private static double miningStatusYPercent = 18;
    private static long miningStatusUntilNanos = 0;
    private static final long MINING_STATUS_DURATION_NANOS = 8_000_000_000L;

    private static String miningStatusText() {
        if (miningMode == MiningMode.AUTO) {
            return "Mining: Automatic - " + (locationRecognized ? detectedArea.label : "Unknown");
        }
        return "Mining: " + miningMode.label;
    }


    @Override
    public void onInitializeClient() {
        loadPosition();
        editKey = register("edit_hud", InputConstants.KEY_F8);
        inspectNpcKey = register("inspect_npc", InputConstants.KEY_F7);
        settingsKey = register("settings", InputConstants.KEY_F9);
        miningModeKey = register("mining_area", InputConstants.KEY_F10);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only refresh occasionally: no per-frame scanning or external requests.
            if (++detectionTick >= 20) {
                detectionTick = 0;
                updateDetectedArea(client);
            }
            while (miningModeKey.consumeClick()) {
                miningMode = miningMode.next();
                savePosition();
                miningStatusUntilNanos = System.nanoTime() + MINING_STATUS_DURATION_NANOS;

            }
            while (settingsKey.consumeClick()) {
                if (client.gui.screen() instanceof InspectorSettingsScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null && client.player != null) {
                    client.gui.setScreen(new InspectorSettingsScreen());
                }
            }
            while (inspectNpcKey.consumeClick()) {
                saveNpcDiagnostics(client);
            }
            while (editKey.consumeClick()) {
                if (client.gui.screen() instanceof HudEditorScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null && client.player != null) {
                    client.gui.setScreen(new HudEditorScreen());
                }
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("skyblockinspector", "tooltip"),
            (graphics, tickCounter) -> draw(graphics));
    }

    private static void draw(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!showHud || client.player == null || client.level == null || client.gui.screen() != null
            || client.gui.hud.isHidden() || client.hitResult == null) {
            return;
        }

        if (client.getCurrentServer() == null) {
            return;
        }
        String server = client.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        if (!(server.equals("hypixel.net") || server.endsWith(".hypixel.net") || server.equals("mc.hypixel.net"))) {
            return;
        }

        // Separate from the crosshair tooltip: visible even when looking into empty sky.
        drawMiningStatus(graphics, client);
        List<Line> lines = inspect(client);
        if (lines.isEmpty()) {
            return;
        }

        ItemStack icon = iconForTarget(client);
        int widest = 90;
        for (Line line : lines) {
            widest = Math.max(widest, client.font.width(line.text()));
        }
        int width = widest + 33;
        int growthPercent = getGrowthPercent(lines);
        int height = Math.max(26, lines.size() * 10 + 8) + (growthPercent >= 0 ? 8 : 0);

        int scaledWidth = (int) Math.ceil(width * hudScale);
        int scaledHeight = (int) Math.ceil(height * hudScale);
        int x = Math.clamp((int) Math.round(graphics.guiWidth() * hudXPercent / 100.0) - scaledWidth / 2,
            0, Math.max(0, graphics.guiWidth() - scaledWidth));
        int y = Math.clamp((int) Math.round(graphics.guiHeight() * hudYPercent / 100.0) - scaledHeight / 2,
            0, Math.max(0, graphics.guiHeight() - scaledHeight));

        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale((float) hudScale, (float) hudScale);
            drawTooltipBox(graphics, 0, 0, width, height);
            if (!icon.isEmpty()) {
                graphics.item(client.player, icon, 6, 5, 0);
            }
            for (int index = 0; index < lines.size(); index++) {
                Line line = lines.get(index);
                graphics.text(client.font, line.text(), 27, 4 + index * 10, line.color(), false);
            }
            if (growthPercent >= 0) {
                int barX = 27;
                int barY = height - 8;
                int barWidth = width - barX - 6;
                graphics.fill(barX, barY, barX + barWidth, barY + 4, BORDER_DARK);
                int filledWidth = (int) Math.round(barWidth * growthPercent / 100.0);
                if (filledWidth > 0) {
                    graphics.fill(barX, barY, barX + filledWidth, barY + 4,
                        growthPercent == 100 ? SUCCESS : ACCENT);
                }
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static void drawMiningStatus(GuiGraphicsExtractor g, Minecraft client) {
        if (System.nanoTime() >= miningStatusUntilNanos) return;
        String message = miningStatusText();
        int w = client.font.width(message) + 14;
        int h = 19;
        int x = Math.clamp((int)Math.round(g.guiWidth() * miningStatusXPercent / 100.0) - w / 2,
            0, Math.max(0, g.guiWidth() - w));
        int y = Math.clamp((int)Math.round(g.guiHeight() * miningStatusYPercent / 100.0) - h / 2,
            0, Math.max(0, g.guiHeight() - h));
        drawTooltipBox(g, x, y, w, h);
        g.text(client.font, message, x + 7, y + 5, ACCENT, false);
    }

    /** Draw vanilla item icons; no extra textures, libraries or network requests. */
    private static ItemStack iconForTarget(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit instanceof EntityHitResult) {
            return new ItemStack(Items.PLAYER_HEAD);
        }
        if (hit instanceof BlockHitResult blockHit && client.level != null) {
            return new ItemStack(client.level.getBlockState(blockHit.getBlockPos()).getBlock().asItem());
        }
        return ItemStack.EMPTY;
    }

    private static int getGrowthPercent(List<Line> lines) {
        for (Line line : lines) {
            String text = line.text();
            if (text.startsWith("Growth: ") && text.endsWith("%")) {
                try {
                    return Math.clamp(Integer.parseInt(text.substring(8, text.length() - 1)), 0, 100);
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static void drawTooltipBox(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, (backgroundOpacity * 255 / 100 << 24) | 0x101010);
        graphics.fill(x, y, x + width, y + 1, BORDER_LIGHT);
        graphics.fill(x, y, x + 1, y + height, BORDER_LIGHT);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_DARK);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);
    }

    private static KeyMapping register(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.skyblockinspector." + name,
                key,
                CATEGORY
            )
        );
    }

    /** Read each setting independently so one bad/older value cannot reset other preferences. */
    private static void loadPosition() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
        } catch (Exception ignored) {
            return; // Preserve defaults if the file cannot be read.
        }

        hudXPercent = settingDouble(properties, "hud_x_percent", 50, 0, 100);
        hudYPercent = settingDouble(properties, "hud_y_percent", 55, 0, 100);
        hudScale = settingDouble(properties, "hud_scale", 1.0, 0.25, 1.5);
        miningStatusXPercent = settingDouble(properties, "mining_status_x_percent", 50, 0, 100);
        miningStatusYPercent = settingDouble(properties, "mining_status_y_percent", 18, 0, 100);
        showHud = settingBoolean(properties, "show_hud", true);
        showNpcs = settingBoolean(properties, "show_npcs", true);
        showBlocks = settingBoolean(properties, "show_blocks", true);
        showCropGrowth = settingBoolean(properties, "show_crop_growth", true);
        showMining = settingBoolean(properties, "show_mining", true);
        backgroundOpacity = (int) settingDouble(properties, "background_opacity", 88, 0, 100);
        try {
            miningMode = MiningMode.valueOf(properties.getProperty("mining_mode", "AUTO"));
        } catch (IllegalArgumentException ignored) {
            miningMode = MiningMode.AUTO;
        }
    }

    private static double settingDouble(Properties properties, String name, double defaultValue,
                                        double minimum, double maximum) {
        try {
            double value = Double.parseDouble(properties.getProperty(name, Double.toString(defaultValue)));
            return Double.isFinite(value) ? Math.clamp(value, minimum, maximum) : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static boolean settingBoolean(Properties properties, String name, boolean defaultValue) {
        String value = properties.getProperty(name);
        if (value == null) return defaultValue;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return defaultValue;
    }

    private static void savePosition() {
        Properties properties = new Properties();
        properties.setProperty("hud_x_percent", Double.toString(hudXPercent));
        properties.setProperty("hud_y_percent", Double.toString(hudYPercent));
        properties.setProperty("hud_scale", Double.toString(hudScale));
        properties.setProperty("mining_status_x_percent", Double.toString(miningStatusXPercent));
        properties.setProperty("mining_status_y_percent", Double.toString(miningStatusYPercent));
        properties.setProperty("mining_mode", miningMode.name());
        properties.setProperty("show_hud", Boolean.toString(showHud));
        properties.setProperty("show_npcs", Boolean.toString(showNpcs));
        properties.setProperty("show_blocks", Boolean.toString(showBlocks));
        properties.setProperty("show_crop_growth", Boolean.toString(showCropGrowth));
        properties.setProperty("show_mining", Boolean.toString(showMining));
        properties.setProperty("background_opacity", Integer.toString(backgroundOpacity));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output, "SkyBlock Inspector client preferences (v0.9)");
            }
        } catch (Exception ignored) {
            // Configuration remains usable for this session.
        }
    }

    /**
     * Scan only the client's existing Hypixel scoreboard and tab-list lines.
     * If the island is not explicitly identifiable, automatic mode shows no mining guess.
     * In particular, matching an ore-looking block alone never forces an area.
     */
    private static void updateDetectedArea(Minecraft client) {
        detectedArea = MiningArea.OFF;
        locationRecognized = false;
        if (client.player == null || client.level == null || client.getCurrentServer() == null) return;
        String host = client.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        if (!(host.equals("hypixel.net") || host.endsWith(".hypixel.net"))) return;

        List<String> lines = new ArrayList<>();
        Scoreboard board = client.level.getScoreboard();
        Objective sidebar = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar != null) {
            for (PlayerScoreEntry entry : board.listPlayerScores(sidebar)) {
                if (entry.display() != null) lines.add(entry.display().getString());
                PlayerTeam team = board.getPlayersTeam(entry.owner());
                if (team != null) {
                    // Hypixel commonly encodes sidebar text as team prefix + fake entry + suffix.
                    lines.add(team.getPlayerPrefix().getString() + entry.owner() + team.getPlayerSuffix().getString());
                } else {
                    lines.add(entry.owner());
                }
            }
        }
        // Tab often contains an explicit island marker when scoreboard only shows sub-areas.
        if (client.getConnection() != null) {
            for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
                Component label = info.getTabListDisplayName();
                if (label != null) lines.add(label.getString());
            }
        }

        // Prefer explicit location lines, rather than an arbitrary occurrence of a mining
        // name in a quest, advertisement or chat message.
        MiningArea found = MiningArea.OFF;
        for (String line : lines) {
            String normalized = line.replaceAll("§[0-9a-fk-or]", "").trim().toLowerCase(Locale.ROOT);
            if (normalized.length() > 90) continue;
            boolean looksLocation = normalized.contains("⏣") || normalized.contains("area:")
                || normalized.contains("location:") || normalized.startsWith("dwarven mines")
                || normalized.startsWith("crystal hollows") || normalized.startsWith("glacite tunnels")
                || normalized.startsWith("glacite mineshaft");
            if (!looksLocation) continue;
            MiningArea candidate = MiningArea.OFF;
            if (normalized.contains("glacite tunnels") || normalized.contains("glacite mineshaft")) {
                candidate = MiningArea.GLACITE;
            } else if (normalized.contains("crystal hollows")) {
                candidate = MiningArea.HOLLOWS;
            } else if (normalized.contains("dwarven mines")) {
                candidate = MiningArea.DWARVEN;
            } else if (normalized.contains("⏣") && isDwarvenSubarea(normalized)) {
                candidate = MiningArea.DWARVEN;
            }
            if (candidate != MiningArea.OFF) {
                if (found != MiningArea.OFF && found != candidate) return; // ambiguous: fail closed
                found = candidate;
            }
        }
        detectedArea = found;
        locationRecognized = found != MiningArea.OFF;
    }

    // Conservative set of known Dwarven Mines sub-areas. No coordinate guessing.
    private static boolean isDwarvenSubarea(String line) {
        return line.contains("dwarven village") || line.contains("royal mines")
            || line.contains("upper mines") || line.contains("rampart's quarry")
            || line.contains("rampart quarry") || line.contains("far reserve")
            || line.contains("goblin burrows") || line.contains("great ice wall")
            || line.contains("cliffside veins") || line.contains("divan's gateway")
            || line.contains("palace bridge") || line.contains("lava springs")
            || line.contains("forge basin") || line.contains("the forge")
            || line.contains("royal palace") || line.contains("hanging court")
            || line.contains("aristocrat passage");
    }

    /** Drag the HUD preview or adjust its scale using the on-screen slider. */
    /** Simple mouse-driven settings. Keybindings use Minecraft's own editor and persistence. */
    private static final class InspectorSettingsScreen extends Screen {
        private static final int BOX_W = 270;
        private static final int ROW_H = 22;
        private boolean draggingOpacity;

        private InspectorSettingsScreen() {
            super(Component.literal("SkyBlock Inspector Settings"));
        }

        private int left() { return (width - BOX_W) / 2; }
        private int top() { return Math.max(12, (height - 286) / 2); }

        private void row(GuiGraphicsExtractor g, int y, String name, boolean value, boolean hover) {
            int x = left();
            g.fill(x + 7, y, x + BOX_W - 7, y + ROW_H - 2, hover ? 0x80364C60 : 0x50272D34);
            g.text(font, name, x + 15, y + 6, TITLE, false);
            g.text(font, value ? "ON" : "OFF", x + BOX_W - 46, y + 6, value ? SUCCESS : WARNING, false);
        }

        private boolean inRow(double mx, double my, int row) {
            int y = top() + 31 + row * ROW_H;
            return mx >= left() + 7 && mx < left() + BOX_W - 7 && my >= y && my < y + ROW_H - 2;
        }

        private void updateOpacity(double mx) {
            backgroundOpacity = Math.clamp((int) Math.round((mx - left() - 20) * 100.0 / (BOX_W - 40)), 0, 100);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            super.extractRenderState(g, mouseX, mouseY, delta);
            int x = left(), y = top();
            g.fill(x, y, x + BOX_W, y + 286, 0xF0141920);
            g.fill(x, y, x + BOX_W, y + 1, BORDER_LIGHT);
            g.text(font, "SkyBlock Inspector", x + 14, y + 11, TITLE, false);
            g.text(font, "SETTINGS", x + BOX_W - 72, y + 11, ACCENT, false);
            row(g, y + 31, "Enable HUD", showHud, inRow(mouseX, mouseY, 0));
            row(g, y + 53, "Show NPCs", showNpcs, inRow(mouseX, mouseY, 1));
            row(g, y + 75, "Show ordinary blocks", showBlocks, inRow(mouseX, mouseY, 2));
            row(g, y + 97, "Crop growth information", showCropGrowth, inRow(mouseX, mouseY, 3));
            row(g, y + 119, "Mining information", showMining, inRow(mouseX, mouseY, 4));
            g.text(font, "Background opacity: " + backgroundOpacity + "%", x + 15, y + 148, SUBTITLE, false);
            int sliderX = x + 20, sliderY = y + 169, sliderWidth = BOX_W - 40;
            int knob = sliderX + (int) Math.round(backgroundOpacity / 100.0 * sliderWidth);
            g.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 5, BORDER_LIGHT);
            g.fill(sliderX, sliderY, knob, sliderY + 5, ACCENT);
            g.fill(knob - 3, sliderY - 3, knob + 3, sliderY + 8, TITLE);
            button(g, y + 191, "Edit HUD position / size");
            button(g, y + 219, "Change keybinds (Minecraft Controls)");
            g.text(font, "F9: settings   |   F8: HUD   |   F10: mining", x + 12, y + 253, SUBTITLE, false);
            g.text(font, "Esc: save and close", x + 12, y + 267, SUBTITLE, false);
        }

        private void button(GuiGraphicsExtractor g, int y, String label) {
            int x = left();
            g.fill(x + 7, y, x + BOX_W - 7, y + 22, 0xFF354253);
            g.text(font, label, x + 15, y + 7, TITLE, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 1) return super.mouseClicked(event, doubleClick);
            double mx = event.x(), my = event.y();
            for (int i = 0; i < 5; i++) {
                if (!inRow(mx, my, i)) continue;
                switch (i) {
                    case 0 -> showHud = !showHud;
                    case 1 -> showNpcs = !showNpcs;
                    case 2 -> showBlocks = !showBlocks;
                    case 3 -> showCropGrowth = !showCropGrowth;
                    case 4 -> showMining = !showMining;
                    default -> { }
                }
                savePosition();
                return true;
            }
            if (mx >= left() + 10 && mx <= left() + BOX_W - 10
                    && my >= top() + 161 && my <= top() + 182) {
                draggingOpacity = true;
                updateOpacity(mx);
                savePosition();
                return true;
            }
            if (mx >= left() + 7 && mx < left() + BOX_W - 7) {
                if (my >= top() + 191 && my < top() + 213) {
                    savePosition();
                    minecraft.gui.setScreen(new HudEditorScreen());
                    return true;
                }
                if (my >= top() + 219 && my < top() + 241) {
                    savePosition();
                    minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options));
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            if (draggingOpacity) {
                updateOpacity(event.x());
                return true;
            }
            return super.mouseDragged(event, dx, dy);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (event.button() == 1 && draggingOpacity) {
                draggingOpacity = false;
                savePosition();
                return true;
            }
            return super.mouseReleased(event);
        }

        @Override
        public void onClose() {
            savePosition();
            super.onClose();
        }

        @Override
        public boolean isPauseScreen() { return false; }
    }

    private static final class HudEditorScreen extends Screen {
        private static final int PANEL_WIDTH = 185;
        private static final int PANEL_HEIGHT = 41;
        private static final double MIN_SCALE = 0.25;
        private static final double MAX_SCALE = 1.50;
        private static final int SLIDER_WIDTH = 150;
        private static final int SLIDER_Y = 43;
        private boolean dragging;
        private boolean draggingSlider;
        private boolean draggingMiningStatus;
        private double miningGrabOffsetX;
        private double miningGrabOffsetY;
        private double grabOffsetX;
        private double grabOffsetY;

        private HudEditorScreen() {
            super(Component.literal("SkyBlock Inspector HUD Editor"));
        }

        private String miningPreviewText() { return "Mining: Automatic - Dwarven Mines"; }
        private int miningPreviewWidth() { return font.width(miningPreviewText()) + 14; }
        private int miningPreviewHeight() { return 19; }
        private int miningPreviewX() {
            return Math.clamp((int)Math.round(width * miningStatusXPercent / 100.0) - miningPreviewWidth() / 2,
                0, Math.max(0, width - miningPreviewWidth()));
        }
        private int miningPreviewY() {
            return Math.clamp((int)Math.round(height * miningStatusYPercent / 100.0) - miningPreviewHeight() / 2,
                0, Math.max(0, height - miningPreviewHeight()));
        }

        private int sliderLeft() {
            return Math.max(8, (width - SLIDER_WIDTH) / 2);
        }

        private void updateScaleFromMouse(double mouseX) {
            double fraction = Math.clamp((mouseX - sliderLeft()) / SLIDER_WIDTH, 0.0, 1.0);
            double newScale = MIN_SCALE + fraction * (MAX_SCALE - MIN_SCALE);
            // Steps of 5% (25%, 30%, 35%, ... 150%).
            hudScale = Math.clamp(Math.round(newScale * 20.0) / 20.0, MIN_SCALE, MAX_SCALE);
        }

        private int panelWidth() {
            return (int) Math.ceil(PANEL_WIDTH * hudScale);
        }

        private int panelHeight() {
            return (int) Math.ceil(PANEL_HEIGHT * hudScale);
        }

        private int panelX() {
            return Math.clamp((int) Math.round(width * hudXPercent / 100.0) - panelWidth() / 2,
                0, Math.max(0, width - panelWidth()));
        }

        private int panelY() {
            return Math.clamp((int) Math.round(height * hudYPercent / 100.0) - panelHeight() / 2,
                0, Math.max(0, height - panelHeight()));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            int x = panelX();
            int y = panelY();
            boolean hover = mouseX >= x && mouseX < x + panelWidth()
                && mouseY >= y && mouseY < y + panelHeight();

            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(x, y);
                graphics.pose().scale((float) hudScale, (float) hudScale);
                drawTooltipBox(graphics, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
                if (hover || dragging) {
                    graphics.fill(1, 1, PANEL_WIDTH - 1, PANEL_HEIGHT - 1, 0x401C87C9);
                }
                graphics.text(font, "SkyBlock Inspector", 7, 5, TITLE, false);
                graphics.text(font, "Drag to reposition", 7, 18, ACCENT, false);
                graphics.text(font, "Size: " + (int) Math.round(hudScale * 100) + "%", 7, 29, SUBTITLE, false);
            } finally {
                graphics.pose().popMatrix();
            }

            int statusX = miningPreviewX();
            int statusY = miningPreviewY();
            drawTooltipBox(graphics, statusX, statusY, miningPreviewWidth(), miningPreviewHeight());
            graphics.text(font, miningPreviewText(), statusX + 7, statusY + 5, ACCENT, false);
            graphics.text(font, "Drag either the tooltip or mining status", 8, height - 18, SUBTITLE, false);

            graphics.text(font, "HUD editor | Esc to close", Math.max(6, width / 2 - 72), 12, TITLE, false);
            String sizeLabel = "HUD size: " + (int) Math.round(hudScale * 100) + "%";
            graphics.text(font, sizeLabel, Math.max(6, width / 2 - font.width(sizeLabel) / 2), 27, SUBTITLE, false);

            int left = sliderLeft();
            int right = left + SLIDER_WIDTH;
            int knobX = left + (int) Math.round((hudScale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE) * SLIDER_WIDTH);
            graphics.fill(left, SLIDER_Y - 2, right, SLIDER_Y + 3, 0xFF394550);
            graphics.fill(left, SLIDER_Y - 2, knobX, SLIDER_Y + 3, ACCENT);
            graphics.fill(knobX - 3, SLIDER_Y - 6, knobX + 4, SLIDER_Y + 7, TITLE);
            graphics.text(font, "25%", left, SLIDER_Y + 11, SUBTITLE, false);
            graphics.text(font, "150%", right - font.width("150%"), SLIDER_Y + 11, SUBTITLE, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 1) {
                double mouseX = event.x();
                double mouseY = event.y();
                // Test slider before HUD preview to avoid accidental movement.
                if (mouseX >= sliderLeft() - 8 && mouseX <= sliderLeft() + SLIDER_WIDTH + 8
                    && mouseY >= SLIDER_Y - 9 && mouseY <= SLIDER_Y + 9) {
                    draggingSlider = true;
                    updateScaleFromMouse(mouseX);
                    return true;
                }
                if (mouseX >= miningPreviewX() && mouseX < miningPreviewX() + miningPreviewWidth()
                    && mouseY >= miningPreviewY() && mouseY < miningPreviewY() + miningPreviewHeight()) {
                    draggingMiningStatus = true;
                    miningGrabOffsetX = mouseX - miningPreviewX();
                    miningGrabOffsetY = mouseY - miningPreviewY();
                    return true;
                }
                if (mouseX >= panelX() && mouseX < panelX() + panelWidth()
                    && mouseY >= panelY() && mouseY < panelY() + panelHeight()) {
                    dragging = true;
                    grabOffsetX = mouseX - panelX();
                    grabOffsetY = mouseY - panelY();
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            if (draggingSlider) {
                updateScaleFromMouse(event.x());
                return true;
            }
            if (draggingMiningStatus) {
                double x = Math.clamp(event.x() - miningGrabOffsetX, 0, Math.max(0, width - miningPreviewWidth()));
                double y = Math.clamp(event.y() - miningGrabOffsetY, 0, Math.max(0, height - miningPreviewHeight()));
                miningStatusXPercent = Math.clamp((x + miningPreviewWidth() / 2.0) * 100.0 / width, 0, 100);
                miningStatusYPercent = Math.clamp((y + miningPreviewHeight() / 2.0) * 100.0 / height, 0, 100);
                return true;
            }
            if (dragging) {
                double x = Math.clamp(event.x() - grabOffsetX, 0, Math.max(0, width - panelWidth()));
                double y = Math.clamp(event.y() - grabOffsetY, 0, Math.max(0, height - panelHeight()));
                hudXPercent = Math.clamp((x + panelWidth() / 2.0) * 100.0 / width, 0, 100);
                hudYPercent = Math.clamp((y + panelHeight() / 2.0) * 100.0 / height, 0, 100);
                return true;
            }
            return super.mouseDragged(event, dx, dy);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (event.button() == 1 && (dragging || draggingSlider || draggingMiningStatus)) {
                dragging = false;
                draggingSlider = false;
                draggingMiningStatus = false;
                savePosition();
                return true;
            }
            return super.mouseReleased(event);
        }

        @Override
        public void onClose() {
            savePosition();
            super.onClose();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static List<Line> inspect(Minecraft client) {
        List<Line> lines = new ArrayList<>();
        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return lines;
        }

        if (hit instanceof EntityHitResult entityHit) {
            if (!showNpcs) return lines;
            Entity target = entityHit.getEntity();
            String nameFromLabel = findNearbyNameLabel(client, target);
            if (nameFromLabel != null) {
                lines.add(new Line(nameFromLabel, TITLE));
                lines.add(new Line("Hypixel NPC • nameplate", SUBTITLE));
            } else {
                // The actual NPC name may differ from the targeted fake player,
                // villager or armor stand. Never call the fallback a verified NPC name.
                String fallback = target.getName().getString().trim();
                if (looksGeneratedUsername(fallback)) {
                    lines.add(new Line("NPC (name unavailable)", TITLE));
                } else {
                    lines.add(new Line(fallback.isEmpty() ? "Unknown Entity" : fallback, TITLE));
                }
                lines.add(new Line("Entity name • unverified", SUBTITLE));
            }
            return lines;
        }

        if (!(hit instanceof BlockHitResult blockHit)) {
            return lines;
        }

        BlockState state = client.level.getBlockState(blockHit.getBlockPos());
        if (state.isAir()) {
            return lines;
        }

        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();

        // Wild Rose uses the vanilla Rose Bush appearance on Hypixel.
        // A block-state lookup alone cannot prove its SkyBlock item ID:
        // ordinary Rose Bush blocks can have the same vanilla block ID.
        // The tooltip therefore makes the identification explicitly tentative.
        if (id.equals("rose_bush")) {
            lines.add(new Line("Wild Rose (possible)", TITLE));
            lines.add(new Line("SkyBlock Garden crop • visual match", SUBTITLE));
            lines.add(new Line("SkyBlock ID: WILD_ROSE (unverified)", ACCENT));
            return lines;
        }

        String cropName = cropName(id);
        if (cropName != null) {
            lines.add(new Line(cropName, TITLE));
            lines.add(new Line("Hypixel Garden Crop", SUBTITLE));

            IntegerProperty age = ageProperty(state);
            if (age != null && showCropGrowth) {
                int current = state.getValue(age);
                int min = age.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0);
                int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                int percent = max == min ? 100 : (100 * (current - min)) / (max - min);
                lines.add(new Line("Growth: " + percent + "%", current >= max ? SUCCESS : ACCENT));
                lines.add(new Line(current >= max ? "Ready to harvest" : "Growing", current >= max ? SUCCESS : SUBTITLE));
            }
            return lines;
        }

        // Block IDs alone cannot prove a Hypixel ore. The area comes from
        // scoreboard/tab text in AUTO mode or an explicit F10 manual override.
        // All candidates remain marked as possible until confirmed in-game.
        MiningArea miningArea = activeArea();
        if (showMining && miningArea != MiningArea.OFF) {
            if (id.equals("polished_diorite") && miningArea != MiningArea.HOLLOWS) {
                addMiningMatch(lines, "Titanium", 5);
                return lines;
            }
            if (isMithrilBlock(id)) {
                addMiningMatch(lines, "Mithril", 4);
                return lines;
            }
            if (miningArea != MiningArea.DWARVEN) {
                String gem = gemstone(id);
                if (gem != null) {
                    addMiningMatch(lines, gem + " Gemstone", gemstoneBreakingPower(gem));
                    return lines;
                }
            }
        }

        if (!showBlocks) return lines;
        lines.add(new Line(readable(id), TITLE));
        lines.add(new Line("Minecraft Block", SUBTITLE));
        return lines;
    }

    /**
     * Hypixel can render a named NPC using a fake player, villager or armor stand,
     * with a separate armor-stand/text-display nameplate. Prefer a plausible label
     * above and horizontally aligned with the crosshair target.
     *
     * Search is local and read-only. If nothing reliable is found, do not guess
     * the NPC's name from an unrelated nearby entity.
     */
    private static String findNearbyNameLabel(Minecraft client, Entity target) {
        if (client.level == null) {
            return null;
        }
        String best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        // Hypixel's NPC nameplate armor stands may be at the NPC's feet,
        // not above the visual head. The NPC diagnostics confirmed:
        // Sam:  label y = target y, Duke: label y = target y + 0.10.
        for (Entity candidate : client.level.getEntitiesOfClass(
                Entity.class, target.getBoundingBox().inflate(1.25, 3.6, 1.25),
                entity -> entity != target && entity.hasCustomName())) {
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType()).getPath();
            if (!(candidate instanceof ArmorStand) && !type.equals("text_display")) {
                continue;
            }
            if (candidate.getCustomName() == null) {
                continue;
            }
            String label = candidate.getCustomName().getString().trim();
            if (!isLikelyNpcName(label)) {
                continue;
            }
            double dx = candidate.getX() - target.getX();
            double dz = candidate.getZ() - target.getZ();
            double horizontalSq = dx * dx + dz * dz;
            // Keep a tight association so adjacent NPCs' labels aren't mixed up.
            if (horizontalSq > 0.7 * 0.7) {
                continue;
            }
            double relativeY = candidate.getY() - target.getY();
            if (relativeY < -0.45 || relativeY > 2.6) {
                continue;
            }
            // Prefer labels that share the NPC's exact X/Z and base Y.
            // CLICK prompts get rejected by isLikelyNpcName().
            double score = horizontalSq * 12.0 + Math.abs(relativeY) * 0.35;
            if (score < bestScore) {
                bestScore = score;
                best = label;
            }
        }
        return best;
    }

    /**
     * Temporary diagnostics: press F9 while looking at an NPC, then inspect the
     * generated config file. No network packets or gameplay behaviour are changed.
     * The file may include nearby player names, so review it before sharing.
     */
    private static void saveNpcDiagnostics(Minecraft client) {
        if (client.level == null || client.player == null || client.hitResult == null) {
            return;
        }
        Path debugPath = FabricLoader.getInstance().getConfigDir()
            .resolve("skyblock-inspector-npc-debug.txt");
        try {
            StringBuilder report = new StringBuilder();
            report.append("\n=== SkyBlock Inspector NPC diagnostics ")
                .append(LocalDateTime.now()).append(" ===\n");
            HitResult hit = client.hitResult;
            report.append("Target type: ").append(hit.getType()).append('\n');
            if (hit instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();
                report.append("Target: ").append(describeEntity(target)).append('\n');
                report.append("Nearby entities (within 5 blocks of target):\n");
                client.level.getEntitiesOfClass(Entity.class,
                        target.getBoundingBox().inflate(5, 5, 5), candidate -> true)
                    .stream()
                    .sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(target)))
                    .limit(50)
                    .forEach(candidate -> report.append("  ")
                        .append(String.format(Locale.ROOT, "distance=%.2f ",
                            Math.sqrt(candidate.distanceToSqr(target))))
                        .append(describeEntity(candidate)).append('\n'));
            } else {
                report.append("Point the crosshair directly at the NPC's body and press F9.\n");
            }
            Files.createDirectories(debugPath.getParent());
            Files.writeString(debugPath, report.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("[SkyBlock Inspector] Failed to write NPC diagnostics: " + e);
        }
    }

    private static String describeEntity(Entity entity) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String custom = entity.getCustomName() == null
            ? "<none>" : entity.getCustomName().getString();
        return "type=" + type
            + " name=" + entity.getName().getString()
            + " customName=" + custom
            + " position=(" + String.format(Locale.ROOT, "%.2f,%.2f,%.2f",
                entity.getX(), entity.getY(), entity.getZ()) + ")";
    }

    private static boolean looksGeneratedUsername(String name) {
        // Hypixel's generated 8- to 16-character fake-player IDs commonly mix
        // lowercase letters and digits. A label-based name always takes priority.
        return name.matches("[a-z0-9]{8,16}") && name.matches(".*[0-9].*");
    }

    private static boolean isLikelyNpcName(String name) {
        String trimmed = name.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.length() < 2 || trimmed.length() > 40 || looksGeneratedUsername(trimmed)) {
            return false;
        }
        if (normalized.equals("click") || normalized.contains("click")
                || normalized.contains("❤") || normalized.contains("health")
                || normalized.contains("defense") || normalized.contains("level")
                || normalized.contains("mana") || normalized.contains("✦")
                || normalized.contains("✧") || normalized.contains("coins")) {
            return false;
        }
        // We only accept letters, spaces and common NPC-name punctuation;
        // discard numbers and decorated/stat lines.
        return trimmed.matches("[\\p{L}][\\p{L} .'-]*") && !trimmed.contains("  ");
    }

    private static IntegerProperty ageProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && property.getName().equals("age")) {
                return integer;
            }
        }
        return null;
    }

    private static void addMiningMatch(List<Line> lines, String name, int breakingPower) {
        lines.add(new Line("Possible " + name, TITLE));
        MiningArea area = activeArea();
        lines.add(new Line("Mining: " + area.label + (miningMode == MiningMode.AUTO ? " (auto)" : " (manual)"), SUBTITLE));
        lines.add(new Line("Breaking Power: " + breakingPower, WARNING));
        lines.add(new Line("Visual match • not verified", SUBTITLE));
    }

    private static int gemstoneBreakingPower(String name) {
        return switch (name) {
            case "Ruby" -> 6;
            case "Amber", "Sapphire", "Jade", "Amethyst", "Opal" -> 7;
            case "Topaz" -> 8;
            case "Jasper", "Onyx", "Aquamarine", "Citrine", "Peridot" -> 9;
            default -> 0;
        };
    }

    private static boolean isMithrilBlock(String id) {
        return switch (id) {
            case "cyan_terracotta", "gray_wool", "light_blue_wool",
                 "prismarine", "prismarine_bricks", "dark_prismarine" -> true;
            default -> false;
        };
    }

    private static String gemstone(String id) {
        for (var entry : GEMSTONES.entrySet()) {
            String color = entry.getKey();
            if (id.equals(color + "_stained_glass") || id.equals(color + "_stained_glass_pane")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String cropName(String id) {
        return CROP_NAMES.get(id);
    }

    private static String readable(String id) {
        StringBuilder result = new StringBuilder();
        for (String word : id.split("_")) {
            if (!word.isEmpty()) {
                if (!result.isEmpty()) {
                    result.append(' ');
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1));
            }
        }
        return result.toString();
    }

    private record Line(String text, int color) {}
}
