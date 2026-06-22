package com.optimalaz.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.*;

public class DamageIndicatorManager {

    private static final class Indicator {
        final double x, y, z;
        final float damage;
        int age;
        static final int MAX_AGE = 40;
        final boolean isCrit;

        Indicator(double x, double y, double z, float damage, boolean isCrit) {
            this.x = x;
            this.y = y + 0.5;
            this.z = z;
            this.damage = damage;
            this.age = 0;
            this.isCrit = isCrit;
        }
    }

    private final List<Indicator> indicators = new ArrayList<>();
    private final Map<Integer, Float> lastEntityHealth = new HashMap<>();

    public void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        Vec3d camPos = client.player.getPos();

        List<LivingEntity> entities = client.world.getEntitiesByClass(
            LivingEntity.class,
            new Box(camPos.x - 30, camPos.y - 30, camPos.z - 30,
                    camPos.x + 30, camPos.y + 30, camPos.z + 30),
            e -> e != client.player && e.isAlive()
        );

        for (LivingEntity entity : entities) {
            int id = entity.getId();
            float currentHp = entity.getHealth();
            Float prevHp = lastEntityHealth.get(id);

            if (prevHp != null && currentHp < prevHp) {
                float dmg = prevHp - currentHp;
                boolean crit = dmg > entity.getMaxHealth() * 0.15f;
                indicators.add(new Indicator(entity.getX(), entity.getY() + entity.getHeight(), entity.getZ(), dmg, crit));
            }

            lastEntityHealth.put(id, currentHp);
        }

        lastEntityHealth.entrySet().removeIf(e -> {
            return client.world.getEntityById(e.getKey()) == null;
        });

        indicators.removeIf(i -> i.age++ > Indicator.MAX_AGE);
    }

    public void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || indicators.isEmpty()) return;

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (Indicator ind : new ArrayList<>(indicators)) {
            float progress = (float) ind.age / Indicator.MAX_AGE;
            float alpha = 1.0f - progress;
            float riseY = progress * 1.5f;

            matrices.push();
            matrices.translate(
                ind.x - camPos.x,
                (ind.y + riseY) - camPos.y,
                ind.z - camPos.z
            );

            float yaw = camera.getYaw();
            float pitch = camera.getPitch();
            matrices.multiply(new Quaternionf().rotationYXZ(
                (float) Math.toRadians(yaw),
                (float) Math.toRadians(pitch),
                0
            ));

            float scale = ind.isCrit ? 0.04f : 0.03f;
            matrices.scale(scale, -scale, scale);

            String dmgText = (ind.isCrit ? "✦ " : "") + String.format("%.1f", ind.damage);

            int textAlpha = (int)(alpha * 255);
            int color;
            if (ind.isCrit) color = (textAlpha << 24) | 0xFFDD00;
            else color = (textAlpha << 24) | 0xFF4444;

            int w = client.textRenderer.getWidth(dmgText);
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            client.textRenderer.drawWithOutline(
                Text.literal(dmgText).asOrderedText(),
                -w / 2f, 0,
                color, 0x000000 | (textAlpha << 24),
                matrix,
                immediate,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
            );

            immediate.draw();
            matrices.pop();
        }
    }
}
