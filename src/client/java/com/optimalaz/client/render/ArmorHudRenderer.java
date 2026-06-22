package com.optimalaz.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ArmorHudRenderer {

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.hudHidden) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int hotbarX = screenW / 2 - 91;
        int hotbarY = screenH - 22;

        EquipmentSlot[] slots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

        int startX = hotbarX - 22;
        int startY = hotbarY;

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = client.player.getEquippedStack(slots[i]);
            int x = startX;
            int y = startY - (i * 20);

            context.fill(x - 1, y - 1, x + 17, y + 17, 0x88000000);

            if (!stack.isEmpty()) {
                context.drawItem(stack, x, y);
                context.drawItemInSlot(client.textRenderer, stack, x, y);

                int durability = stack.getMaxDamage() - stack.getDamage();
                int maxDurability = stack.getMaxDamage();
                if (maxDurability > 0) {
                    float fraction = (float) durability / maxDurability;
                    int barW = (int)(16 * fraction);
                    int color = durabilityColor(fraction);
                    context.fill(x, y + 17, x + 16, y + 18, 0xFF000000);
                    context.fill(x, y + 17, x + barW, y + 18, color | 0xFF000000);
                }
            }
        }

        ItemStack offhand = client.player.getEquippedStack(EquipmentSlot.OFFHAND);
        if (!offhand.isEmpty()) {
            int ox = hotbarX + 182 + 6;
            int oy = hotbarY;
            context.fill(ox - 1, oy - 1, ox + 17, oy + 17, 0x88000000);
            context.drawItem(offhand, ox, oy);
            context.drawItemInSlot(client.textRenderer, offhand, ox, oy);
        }
    }

    private static int durabilityColor(float fraction) {
        if (fraction > 0.66f) return 0x00FF00;
        if (fraction > 0.33f) return 0xFFAA00;
        return 0xFF2222;
    }
}
