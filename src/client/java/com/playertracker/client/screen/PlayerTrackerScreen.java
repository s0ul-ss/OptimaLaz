package com.playertracker.client.screen;

import com.playertracker.client.PlayerTrackerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class PlayerTrackerScreen extends Screen {

    private final Screen parent;
    private List<String> playerNames = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 52;
    private static final int PADDING = 16;
    private static final int AVATAR_SIZE = 36;
    private int listTop;
    private int listBottom;

    public PlayerTrackerScreen(Screen parent) {
        super(Text.translatable("playertracker.menu.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        playerNames = PlayerTrackerClient.trackerManager.getAllOnlinePlayers(client);

        listTop = 60;
        listBottom = this.height - 50;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("playertracker.menu.close"),
                btn -> close()
        ).dimensions(this.width / 2 - 60, this.height - 40, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        context.fill(0, 0, this.width, this.height, 0xCC000000);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("playertracker.menu.title"),
                this.width / 2, 20, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer,
                Text.literal("Players online: " + playerNames.size()),
                PADDING, 42, 0xAAAAAA);

        context.enableScissor(0, listTop, this.width, listBottom);

        if (playerNames.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("playertracker.menu.no_players"),
                    this.width / 2, listTop + 20, 0x888888);
        } else {
            int y = listTop - scrollOffset;
            for (int i = 0; i < playerNames.size(); i++) {
                String name = playerNames.get(i);
                if (y + ROW_HEIGHT >= listTop && y <= listBottom) {
                    renderPlayerRow(context, name, y, mouseX, mouseY);
                }
                y += ROW_HEIGHT + 4;
            }
        }

        context.disableScissor();

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPlayerRow(DrawContext context, String name, int y, int mouseX, int mouseY) {
        int rowX = PADDING;
        int rowWidth = this.width - PADDING * 2;

        boolean hovered = mouseX >= rowX && mouseX <= rowX + rowWidth &&
                mouseY >= y && mouseY <= y + ROW_HEIGHT;

        context.fill(rowX, y, rowX + rowWidth, y + ROW_HEIGHT,
                hovered ? 0x55FFFFFF : 0x33FFFFFF);

        renderPlayerHead(context, name, rowX + 8, y + 8, AVATAR_SIZE);

        int textX = rowX + 8 + AVATAR_SIZE + 10;
        context.drawTextWithShadow(textRenderer, Text.literal(name),
                textX, y + 8, 0xFFFFFF);

        boolean tracked = PlayerTrackerClient.trackerManager.isTracked(name);

        if (tracked) {
            var tracked_data = PlayerTrackerClient.trackerManager.getTrackedPlayers().get(name);
            if (tracked_data != null && tracked_data.isVisible()) {
                String coords = String.format("X: %.0f  Y: %.0f  Z: %.0f",
                        tracked_data.getX(), tracked_data.getY(), tracked_data.getZ());
                String dim = getDimLabel(tracked_data.getDimension());
                context.drawTextWithShadow(textRenderer,
                        Text.literal(coords + (dim.isEmpty() ? "" : "  " + dim)),
                        textX, y + 22, 0x88FF88);
            } else if (tracked_data != null && tracked_data.isOnline()) {
                context.drawTextWithShadow(textRenderer,
                        Text.literal("Tracking (loading position...)"),
                        textX, y + 22, 0xFFAA00);
            } else {
                context.drawTextWithShadow(textRenderer,
                        Text.literal("Offline"),
                        textX, y + 22, 0xFF4444);
            }
        }

        String btnLabel = tracked ?
                Text.translatable("playertracker.menu.untrack").getString() :
                Text.translatable("playertracker.menu.track").getString();

        int btnW = 100;
        int btnX = rowX + rowWidth - btnW - 8;
        int btnY = y + ROW_HEIGHT / 2 - 10;

        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW &&
                mouseY >= btnY && mouseY <= btnY + 20;

        context.fill(btnX, btnY, btnX + btnW, btnY + 20,
                tracked ? (btnHovered ? 0xAAFF2222 : 0x88FF2222) :
                        (btnHovered ? 0xAA2222FF : 0x882222FF));

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(btnLabel),
                btnX + btnW / 2, btnY + 6, 0xFFFFFF);
    }

    private void renderPlayerHead(DrawContext context, String playerName, int x, int y, int size) {
        if (client == null || client.getNetworkHandler() == null) return;

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerName);
        if (entry == null) {
            context.fill(x, y, x + size, y + size, 0xFF888888);
            return;
        }

        try {
            Identifier skinTexture = entry.getSkinTextures().texture();
            int u = 8, v = 8, regionSize = 8;
            int totalTexW = 64, totalTexH = 64;
            context.drawTexture(skinTexture, x, y, size, size,
                    (float)(u * totalTexW / 64), (float)(v * totalTexH / 64),
                    regionSize * totalTexW / 64, regionSize * totalTexH / 64,
                    totalTexW, totalTexH);

            int ou = 40, ov = 8;
            context.drawTexture(skinTexture, x - 1, y - 1, size + 2, size + 2,
                    (float)(ou * totalTexW / 64), (float)(ov * totalTexH / 64),
                    regionSize * totalTexW / 64, regionSize * totalTexH / 64,
                    totalTexW, totalTexH);
        } catch (Exception e) {
            context.fill(x, y, x + size, y + size, 0xFF888888);
        }
    }

    private String getDimLabel(String dim) {
        return switch (dim) {
            case "nether" -> Text.translatable("playertracker.dimension.nether").getString();
            case "end" -> Text.translatable("playertracker.dimension.end").getString();
            default -> "";
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int y = listTop - scrollOffset;
            for (String name : playerNames) {
                if (mouseY >= y && mouseY <= y + ROW_HEIGHT) {
                    int rowX = PADDING;
                    int rowWidth = this.width - PADDING * 2;
                    int btnW = 100;
                    int btnX = rowX + rowWidth - btnW - 8;
                    int btnY = y + ROW_HEIGHT / 2 - 10;

                    if (mouseX >= btnX && mouseX <= btnX + btnW &&
                            mouseY >= btnY && mouseY <= btnY + 20) {
                        if (PlayerTrackerClient.trackerManager.isTracked(name)) {
                            PlayerTrackerClient.trackerManager.removeTracker(name);
                        } else {
                            PlayerTrackerClient.trackerManager.addTracker(name);
                        }
                        return true;
                    }
                }
                y += ROW_HEIGHT + 4;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalHeight = playerNames.size() * (ROW_HEIGHT + 4);
        int visibleHeight = listBottom - listTop;
        int maxScroll = Math.max(0, totalHeight - visibleHeight);

        scrollOffset -= (int)(verticalAmount * 12);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
