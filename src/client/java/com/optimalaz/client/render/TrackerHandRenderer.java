package com.optimalaz.client.render;

import com.optimalaz.client.OptimaLazClient;
import com.optimalaz.client.tracker.TrackedPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Map;

public class TrackerHandRenderer {

    public void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Map<String, TrackedPlayer> tracked = OptimaLazClient.trackerManager.getTrackedPlayers();
        if (tracked.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        for (Map.Entry<String, TrackedPlayer> entry : tracked.entrySet()) {
            TrackedPlayer tp = entry.getValue();
            if (!tp.isOnline()) continue;

            Vec3d targetPos = new Vec3d(tp.getX(), tp.getY() + 1.0, tp.getZ());
            Vec3d playerPos = client.player.getEyePos();
            Vec3d dir = targetPos.subtract(playerPos);
            double dist = dir.length();
            if (dist < 0.01) continue;

            Vec3d normDir = dir.normalize();
            Vec3d handPos = playerPos.add(normDir.multiply(2.5));

            matrices.push();
            matrices.translate(handPos.x - camPos.x, handPos.y - camPos.y, handPos.z - camPos.z);

            float yaw = (float) Math.atan2(-normDir.x, normDir.z);
            float pitch = (float) Math.asin(-normDir.y / normDir.length());
            matrices.multiply(new Quaternionf().rotationYXZ(yaw, pitch, 0));

            VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
            renderRedHand(matrices, immediate);
            immediate.draw();

            renderLabel(matrices, context, tp, dist);
            matrices.pop();
        }
    }

    private void renderRedHand(MatrixStack matrices, VertexConsumerProvider provider) {
        VertexConsumer consumer = provider.getBuffer(RenderLayer.getSolid());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        renderBox(consumer, matrix, -0.075f, -0.075f, 0, 0.075f, 0.075f, 0.3f, 1f, 0.1f, 0.1f, 1f, light);
        renderBox(consumer, matrix, -0.03f, -0.03f, 0.3f, 0.03f, 0.03f, 0.8f, 1f, 0.1f, 0.1f, 1f, light);
    }

    private void renderBox(VertexConsumer c, Matrix4f m,
                           float x1, float y1, float z1, float x2, float y2, float z2,
                           float r, float g, float b, float a, int light) {
        c.vertex(m,x1,y1,z1).color(r,g,b,a).texture(0,0).overlay(0).light(light).normal(0,0,1);
        c.vertex(m,x2,y1,z1).color(r,g,b,a).texture(1,0).overlay(0).light(light).normal(0,0,1);
        c.vertex(m,x2,y2,z1).color(r,g,b,a).texture(1,1).overlay(0).light(light).normal(0,0,1);
        c.vertex(m,x1,y2,z1).color(r,g,b,a).texture(0,1).overlay(0).light(light).normal(0,0,1);

        c.vertex(m,x1,y1,z2).color(r,g,b,a).texture(0,0).overlay(0).light(light).normal(0,0,-1);
        c.vertex(m,x1,y2,z2).color(r,g,b,a).texture(0,1).overlay(0).light(light).normal(0,0,-1);
        c.vertex(m,x2,y2,z2).color(r,g,b,a).texture(1,1).overlay(0).light(light).normal(0,0,-1);
        c.vertex(m,x2,y1,z2).color(r,g,b,a).texture(1,0).overlay(0).light(light).normal(0,0,-1);

        c.vertex(m,x1,y2,z1).color(r,g,b,a).texture(0,0).overlay(0).light(light).normal(0,1,0);
        c.vertex(m,x2,y2,z1).color(r,g,b,a).texture(1,0).overlay(0).light(light).normal(0,1,0);
        c.vertex(m,x2,y2,z2).color(r,g,b,a).texture(1,1).overlay(0).light(light).normal(0,1,0);
        c.vertex(m,x1,y2,z2).color(r,g,b,a).texture(0,1).overlay(0).light(light).normal(0,1,0);

        c.vertex(m,x1,y1,z1).color(r,g,b,a).texture(0,0).overlay(0).light(light).normal(0,-1,0);
        c.vertex(m,x1,y1,z2).color(r,g,b,a).texture(0,1).overlay(0).light(light).normal(0,-1,0);
        c.vertex(m,x2,y1,z2).color(r,g,b,a).texture(1,1).overlay(0).light(light).normal(0,-1,0);
        c.vertex(m,x2,y1,z1).color(r,g,b,a).texture(1,0).overlay(0).light(light).normal(0,-1,0);

        c.vertex(m,x1,y1,z1).color(r,g,b,a).texture(0,0).overlay(0).light(light).normal(-1,0,0);
        c.vertex(m,x1,y2,z1).color(r,g,b,a).texture(0,1).overlay(0).light(light).normal(-1,0,0);
        c.vertex(m,x1,y2,z2).color(r,g,b,a).texture(1,1).overlay(0).light(light).normal(-1,0,0);
        c.vertex(m,x1,y1,z2).color(r,g,b,a).texture(1,0).overlay(0).light(light).normal(-1,0,0);

        c.vertex(m,x2,y1,z1).color(r,g,b,a).texture(0,0).overlay(0).light(light).normal(1,0,0);
        c.vertex(m,x2,y1,z2).color(r,g,b,a).texture(1,0).overlay(0).light(light).normal(1,0,0);
        c.vertex(m,x2,y2,z2).color(r,g,b,a).texture(1,1).overlay(0).light(light).normal(1,0,0);
        c.vertex(m,x2,y2,z1).color(r,g,b,a).texture(0,1).overlay(0).light(light).normal(1,0,0);
    }

    private void renderLabel(MatrixStack matrices, WorldRenderContext context, TrackedPlayer tp, double dist) {
        MinecraftClient client = MinecraftClient.getInstance();
        String dimLabel = switch (tp.getDimension()) {
            case "nether" -> " [NETHER]";
            case "end" -> " [END]";
            default -> "";
        };
        String line1 = tp.getName();
        String line2 = tp.isVisible()
                ? String.format("X:%.0f Y:%.0f Z:%.0f%s", tp.getX(), tp.getY(), tp.getZ(), dimLabel)
                : "Online" + dimLabel;

        matrices.push();
        matrices.translate(0, 0.9, 0);
        double scale = Math.max(0.025, Math.min(0.06, 0.025 + dist * 0.001));
        matrices.scale((float)scale, (float)-scale, (float)scale);

        Camera camera = context.camera();
        matrices.multiply(new Quaternionf().rotationYXZ(
                (float)Math.toRadians(camera.getYaw()),
                (float)Math.toRadians(camera.getPitch()), 0));

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        int n1 = client.textRenderer.getWidth(line1);
        int n2 = client.textRenderer.getWidth(line2);
        Matrix4f bgMatrix = matrices.peek().getPositionMatrix();

        client.textRenderer.drawWithOutline(Text.literal(line1).asOrderedText(), -n1/2f, -20, 0xFFFFFF, 0, bgMatrix, immediate, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        client.textRenderer.drawWithOutline(Text.literal(line2).asOrderedText(), -n2/2f, -10, 0x88FF88, 0, bgMatrix, immediate, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        immediate.draw();
        matrices.pop();
    }
}
