package com.optimalaz.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

public class HealthBarRenderer {

    private static final float BAR_WIDTH = 1.0f;
    private static final float BAR_HEIGHT = 0.07f;
    private static final float RENDER_DISTANCE = 32.0;

    public void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();

        List<LivingEntity> entities = client.world.getEntitiesByClass(
            LivingEntity.class,
            new Box(camPos.x - RENDER_DISTANCE, camPos.y - RENDER_DISTANCE, camPos.z - RENDER_DISTANCE,
                    camPos.x + RENDER_DISTANCE, camPos.y + RENDER_DISTANCE, camPos.z + RENDER_DISTANCE),
            e -> e != client.player && !e.isInvisible() && e.isAlive()
        );

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (LivingEntity entity : entities) {
            double dx = entity.getX() - camPos.x;
            double dy = entity.getY() - camPos.y;
            double dz = entity.getZ() - camPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > RENDER_DISTANCE) continue;

            float hp = entity.getHealth();
            float maxHp = entity.getMaxHealth();
            float hpFraction = Math.max(0f, hp / maxHp);

            double labelY = entity.getY() + entity.getHeight() + 0.3;

            matrices.push();
            matrices.translate(
                entity.getX() - camPos.x,
                labelY - camPos.y,
                entity.getZ() - camPos.z
            );

            float yaw = camera.getYaw();
            float pitch = camera.getPitch();
            matrices.multiply(new Quaternionf().rotationYXZ(
                (float) Math.toRadians(yaw),
                (float) Math.toRadians(pitch),
                0
            ));

            float scale = 0.025f;
            matrices.scale(scale, -scale, scale);

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            renderHealthBar(immediate, matrix, hpFraction, hp, maxHp, dist);

            String label;
            if (entity instanceof PlayerEntity pe) {
                label = pe.getGameProfile().getName() + " §c" + Math.round(hp) + "§7/§c" + Math.round(maxHp);
            } else {
                String entityName = entity.getName().getString();
                label = entityName + " §c" + Math.round(hp) + "§7/§c" + Math.round(maxHp);
            }

            int labelW = client.textRenderer.getWidth(Text.literal(label).getString());

            client.textRenderer.drawWithOutline(
                Text.literal(label).asOrderedText(),
                -labelW / 2f, -20,
                0xFFFFFF, 0x000000,
                matrix,
                immediate,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
            );

            immediate.draw();
            matrices.pop();
        }
    }

    private void renderHealthBar(VertexConsumerProvider.Immediate immediate, Matrix4f matrix,
                                  float fraction, float hp, float maxHp, double dist) {
        MinecraftClient client = MinecraftClient.getInstance();
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        float scale = 0.025f;

        float w = BAR_WIDTH / scale;
        float h = BAR_HEIGHT / scale;
        float halfW = w / 2;

        VertexConsumer bg = immediate.getBuffer(RenderLayer.getGuiOverlay());
        fillQuad(bg, matrix, -halfW - 1, -h - 1, halfW + 1, h + 1, 0, 0, 0, 180, light);
        fillQuad(bg, matrix, -halfW, -h, halfW, h, 30, 30, 30, 200, light);

        int r, g, b;
        if (fraction > 0.5f) {
            float t = (fraction - 0.5f) * 2;
            r = (int)(255 * (1 - t)); g = 220; b = 0;
        } else {
            float t = fraction * 2;
            r = 220; g = (int)(180 * t); b = 0;
        }
        if (fraction < 0.25f) { r = 220; g = 30; b = 30; }

        fillQuad(bg, matrix, -halfW, -h, -halfW + w * fraction, h, r, g, b, 255, light);

        immediate.draw();
    }

    private void fillQuad(VertexConsumer c, Matrix4f m,
                          float x1, float y1, float x2, float y2,
                          int r, int g, int b, int a, int light) {
        c.vertex(m, x1, y1, 0).color(r, g, b, a).texture(0, 0).overlay(0).light(light).normal(0, 0, 1);
        c.vertex(m, x2, y1, 0).color(r, g, b, a).texture(1, 0).overlay(0).light(light).normal(0, 0, 1);
        c.vertex(m, x2, y2, 0).color(r, g, b, a).texture(1, 1).overlay(0).light(light).normal(0, 0, 1);
        c.vertex(m, x1, y2, 0).color(r, g, b, a).texture(0, 1).overlay(0).light(light).normal(0, 0, 1);
    }
}
