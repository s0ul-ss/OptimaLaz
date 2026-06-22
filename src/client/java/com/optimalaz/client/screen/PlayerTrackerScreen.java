package com.optimalaz.client.screen;

import com.optimalaz.client.OptimaLazClient;
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
    private int listTop, listBottom;

    public PlayerTrackerScreen(Screen parent) {
        super(Text.translatable("optimalaz.tracker.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        playerNames = OptimaLazClient.trackerManager.getAllOnlinePlayers(client);
        listTop = 60;
        listBottom = this.height - 50;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("optimalaz.tracker.close"), btn -> close()
        ).dimensions(this.width / 2 - 60, this.height - 40, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("optimalaz.tracker.title"), this.width / 2, 20, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Players online: " + playerNames.size()), PADDING, 42, 0xAAAAAA);

        context.enableScissor(0, listTop, this.width, listBottom);
        if (playerNames.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("optimalaz.tracker.no_players"), this.width / 2, listTop + 20, 0x888888);
        } else {
            int y = listTop - scrollOffset;
            for (String name : playerNames) {
                if (y + ROW_HEIGHT >= listTop && y <= listBottom) renderPlayerRow(context, name, y, mouseX, mouseY);
                y += ROW_HEIGHT + 4;
            }
        }
        context.disableScissor();
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPlayerRow(DrawContext context, String name, int y, int mouseX, int mouseY) {
        int rowX = PADDING, rowW = this.width - PADDING * 2;
        boolean hovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        context.fill(rowX, y, rowX + rowW, y + ROW_HEIGHT, hovered ? 0x55FFFFFF : 0x33FFFFFF);

        renderPlayerHead(context, name, rowX + 8, y + 8, AVATAR_SIZE);

        int textX = rowX + 8 + AVATAR_SIZE + 10;
        context.drawTextWithShadow(textRenderer, Text.literal(name), textX, y + 8, 0xFFFFFF);

        boolean tracked = OptimaLazClient.trackerManager.isTracked(name);
        if (tracked) {
            var td = OptimaLazClient.trackerManager.getTrackedPlayers().get(name);
            if (td != null && td.isVisible()) {
                String dimLabel = switch (td.getDimension()) {
                    case "nether" -> "  [NETHER]";
                    case "end" -> "  [END]";
                    default -> "";
                };
                context.drawTextWithShadow(textRenderer, Text.literal(
                        String.format("X:%.0f Y:%.0f Z:%.0f%s", td.getX(), td.getY(), td.getZ(), dimLabel)
                ), textX, y + 22, 0x88FF88);
            } else if (td != null && td.isOnline()) {
                context.drawTextWithShadow(textRenderer, Text.literal("Tracking..."), textX, y + 22, 0xFFAA00);
            }
        }

        int btnW = 100, btnX = rowX + rowW - btnW - 8, btnY = y + ROW_HEIGHT / 2 - 10;
        boolean btnH = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 20;
        String btnLabel = tracked ? Text.translatable("optimalaz.tracker.untrack").getString() : Text.translatable("optimalaz.tracker.track").getString();
        context.fill(btnX, btnY, btnX + btnW, btnY + 20, tracked ? (btnH ? 0xAAFF2222 : 0x88FF2222) : (btnH ? 0xAA2222FF : 0x882222FF));
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(btnLabel), btnX + btnW / 2, btnY + 6, 0xFFFFFF);
    }

    private void renderPlayerHead(DrawContext context, String playerName, int x, int y, int size) {
        if (client == null || client.getNetworkHandler() == null) return;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerName);
        if (entry == null) { context.fill(x, y, x + size, y + size, 0xFF888888); return; }
        try {
            Identifier skin = entry.getSkinTextures().texture();
            context.drawTexture(skin, x, y, size, size, 8f, 8f, 8, 8, 64, 64);
            context.drawTexture(skin, x - 1, y - 1, size + 2, size + 2, 40f, 8f, 8, 8, 64, 64);
        } catch (Exception e) {
            context.fill(x, y, x + size, y + size, 0xFF888888);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int y = listTop - scrollOffset;
            for (String name : playerNames) {
                if (mouseY >= y && mouseY <= y + ROW_HEIGHT) {
                    int rowX = PADDING, rowW = this.width - PADDING * 2;
                    int btnW = 100, btnX = rowX + rowW - btnW - 8, btnY = y + ROW_HEIGHT / 2 - 10;
                    if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 20) {
                        if (OptimaLazClient.trackerManager.isTracked(name)) OptimaLazClient.trackerManager.removeTracker(name);
                        else OptimaLazClient.trackerManager.addTracker(name);
                        return true;
                    }
                }
                y += ROW_HEIGHT + 4;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        int total = playerNames.size() * (ROW_HEIGHT + 4);
        int visible = listBottom - listTop;
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int)(v * 12), Math.max(0, total - visible)));
        return true;
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }
    @Override
    public boolean shouldPause() { return false; }
}
