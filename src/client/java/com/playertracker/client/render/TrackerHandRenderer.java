package com.playertracker.client.render;

import com.playertracker.client.PlayerTrackerClient;
import com.playertracker.client.tracker.TrackedPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Map;

public class TrackerHandRenderer {

    public void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Map<String, TrackedPlayer> tracked = PlayerTrackerClient.trackerManager.getTrackedPlayers();
        if (tracked.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        for (Map.Entry<String, TrackedPlayer> entry : tracked.entrySet()) {
            TrackedPlayer tp = entry.getValue();
            if (!tp.isOnline()) continue;

            Vec3d targetPos;

            if (tp.isVisible()) {
                targetPos = new Vec3d(tp.getX(), tp.getY() + 1.0, tp.getZ());
            } else {
                targetPos = new Vec3d(tp.getX(), tp.getY() + 1.0, tp.getZ());
            }

            Vec3d playerPos = client.player.getEyePos();
            Vec3d dir = targetPos.subtract(playerPos);

            double dist = dir.length();
            if (dist < 0.01) continue;

            Vec3d normDir = dir.normalize();
            double handDistance = 2.5;
            Vec3d handPos = playerPos.add(normDir.multiply(handDistance));

            matrices.push();
            matrices.translate(
                    handPos.x - camPos.x,
                    handPos.y - camPos.y,
                    handPos.z - camPos.z
            );

            float yaw = (float) Math.atan2(-normDir.x, normDir.z);
            float pitch = (float) Math.asin(-normDir.y / normDir.length());

            matrices.multiply(new Quaternionf().rotationYXZ(yaw, pitch, 0));

            VertexConsumerProvider.Immediate immediate =
                    client.getBufferBuilders().getEntityVertexConsumers();

            renderRedHand(matrices, immediate, context.tickCounter().getTickDelta(false));

            immediate.draw();

            renderLabel(matrices, context, tp, dist);

            matrices.pop();
        }
    }

    private void renderRedHand(MatrixStack matrices, VertexConsumerProvider provider, float tickDelta) {
        VertexConsumer consumer = provider.getBuffer(RenderLayer.getSolid());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float r = 1.0f, g = 0.1f, b = 0.1f, a = 1.0f;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        float w = 0.06f;
        float palmLen = 0.3f;
        float fingerLen = 0.5f;
        float palmW = 0.15f;

        renderBox(consumer, matrix,
                -palmW / 2, -palmW / 2, 0,
                palmW / 2, palmW / 2, palmLen,
                r, g, b, a, light);

        renderBox(consumer, matrix,
                -w / 2, -w / 2, palmLen,
                w / 2, w / 2, palmLen + fingerLen,
                r, g, b, a, light);
    }

    private void renderBox(VertexConsumer consumer, Matrix4f matrix,
                           float x1, float y1, float z1,
                           float x2, float y2, float z2,
                           float r, float g, float b, float a, int light) {
        int normal = 0;

        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).texture(0, 0).overlay(normal).light(light).normal(0, 0, 1);
        consumer.vertex(matrix, x2, y1, z1).color(r, g, b, a).texture(1, 0).overlay(normal).light(light).normal(0, 0, 1);
        consumer.vertex(matrix, x2, y2, z1).color(r, g, b, a).texture(1, 1).overlay(normal).light(light).normal(0, 0, 1);
        consumer.vertex(matrix, x1, y2, z1).color(r, g, b, a).texture(0, 1).overlay(normal).light(light).normal(0, 0, 1);

        consumer.vertex(matrix, x1, y1, z2).color(r, g, b, a).texture(0, 0).overlay(normal).light(light).normal(0, 0, -1);
        consumer.vertex(matrix, x1, y2, z2).color(r, g, b, a).texture(0, 1).overlay(normal).light(light).normal(0, 0, -1);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).texture(1, 1).overlay(normal).light(light).normal(0, 0, -1);
        consumer.vertex(matrix, x2, y1, z2).color(r, g, b, a).texture(1, 0).overlay(normal).light(light).normal(0, 0, -1);

        consumer.vertex(matrix, x1, y2, z1).color(r, g, b, a).texture(0, 0).overlay(normal).light(light).normal(0, 1, 0);
        consumer.vertex(matrix, x2, y2, z1).color(r, g, b, a).texture(1, 0).overlay(normal).light(light).normal(0, 1, 0);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).texture(1, 1).overlay(normal).light(light).normal(0, 1, 0);
        consumer.vertex(matrix, x1, y2, z2).color(r, g, b, a).texture(0, 1).overlay(normal).light(light).normal(0, 1, 0);

        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).texture(0, 0).overlay(normal).light(light).normal(0, -1, 0);
        consumer.vertex(matrix, x1, y1, z2).color(r, g, b, a).texture(0, 1).overlay(normal).light(light).normal(0, -1, 0);
        consumer.vertex(matrix, x2, y1, z2).color(r, g, b, a).texture(1, 1).overlay(normal).light(light).normal(0, -1, 0);
        consumer.vertex(matrix, x2, y1, z1).color(r, g, b, a).texture(1, 0).overlay(normal).light(light).normal(0, -1, 0);

        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).texture(0, 0).overlay(normal).light(light).normal(-1, 0, 0);
        consumer.vertex(matrix, x1, y2, z1).color(r, g, b, a).texture(0, 1).overlay(normal).light(light).normal(-1, 0, 0);
        consumer.vertex(matrix, x1, y2, z2).color(r, g, b, a).texture(1, 1).overlay(normal).light(light).normal(-1, 0, 0);
        consumer.vertex(matrix, x1, y1, z2).color(r, g, b, a).texture(1, 0).overlay(normal).light(light).normal(-1, 0, 0);

        consumer.vertex(matrix, x2, y1, z1).color(r, g, b, a).texture(0, 0).overlay(normal).light(light).normal(1, 0, 0);
        consumer.vertex(matrix, x2, y1, z2).color(r, g, b, a).texture(1, 0).overlay(normal).light(light).normal(1, 0, 0);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).texture(1, 1).overlay(normal).light(light).normal(1, 0, 0);
        consumer.vertex(matrix, x2, y2, z1).color(r, g, b, a).texture(0, 1).overlay(normal).light(light).normal(1, 0, 0);
    }

    private void renderLabel(MatrixStack matrices, WorldRenderContext context,
                             TrackedPlayer tp, double distance) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null) return;

        String dimLabel = switch (tp.getDimension()) {
            case "nether" -> " [NETHER]";
            case "end" -> " [END]";
            default -> "";
        };

        String line1 = tp.getName();
        String line2 = tp.isVisible() ?
                String.format("X:%.0f Y:%.0f Z:%.0f%s", tp.getX(), tp.getY(), tp.getZ(), dimLabel) :
                "Online" + dimLabel;

        matrices.push();
        matrices.translate(0, 0.9, 0);

        double scale = Math.max(0.025, Math.min(0.06, 0.025 + distance * 0.001));
        matrices.scale((float) scale, (float) -scale, (float) scale);

        Camera camera = context.camera();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        matrices.multiply(new Quaternionf().rotationYXZ((float) Math.toRadians(yaw), (float) Math.toRadians(pitch), 0));

        VertexConsumerProvider.Immediate immediate =
                client.getBufferBuilders().getEntityVertexConsumers();

        int nameW = client.textRenderer.getWidth(line1);
        int coordW = client.textRenderer.getWidth(line2);
        int maxW = Math.max(nameW, coordW);

        Matrix4f bgMatrix = matrices.peek().getPositionMatrix();
        VertexConsumer bg = immediate.getBuffer(RenderLayer.getGuiTexturedOverlay(
                net.minecraft.util.Identifier.ofVanilla("textures/misc/white.png")));

        fillRect(bg, bgMatrix, -maxW / 2f - 2, -22, maxW / 2f + 2, 4, 0, 0, 0, 160);

        client.textRenderer.drawWithOutline(
                Text.literal(line1).asOrderedText(),
                -nameW / 2f, -20,
                0xFFFFFF, 0x000000,
                matrices.peek().getPositionMatrix(),
                immediate,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        client.textRenderer.drawWithOutline(
                Text.literal(line2).asOrderedText(),
                -coordW / 2f, -10,
                0x88FF88, 0x000000,
                matrices.peek().getPositionMatrix(),
                immediate,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        immediate.draw();

        matrices.pop();
    }

    private void fillRect(VertexConsumer consumer, Matrix4f matrix,
                          float x1, float y1, float x2, float y2,
                          int r, int g, int b, int a) {
        consumer.vertex(matrix, x1, y1, 0).color(r, g, b, a).texture(0, 0).overlay(0).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
        consumer.vertex(matrix, x2, y1, 0).color(r, g, b, a).texture(1, 0).overlay(0).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
        consumer.vertex(matrix, x2, y2, 0).color(r, g, b, a).texture(1, 1).overlay(0).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
        consumer.vertex(matrix, x1, y2, 0).color(r, g, b, a).texture(0, 1).overlay(0).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
    }
}
