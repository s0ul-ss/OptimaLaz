package com.optimalaz.client;

import com.optimalaz.client.optimization.OptimizationManager;
import com.optimalaz.client.render.ArmorHudRenderer;
import com.optimalaz.client.render.DamageIndicatorManager;
import com.optimalaz.client.render.HealthBarRenderer;
import com.optimalaz.client.render.TrackerHandRenderer;
import com.optimalaz.client.screen.PlayerTrackerScreen;
import com.optimalaz.client.tracker.TrackerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class OptimaLazClient implements ClientModInitializer {

    public static KeyBinding openTrackerKey;
    public static TrackerManager trackerManager;
    public static TrackerHandRenderer handRenderer;
    public static HealthBarRenderer healthBarRenderer;
    public static DamageIndicatorManager damageIndicatorManager;
    public static OptimizationManager optimizationManager;

    @Override
    public void onInitializeClient() {
        openTrackerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.optimalaz.open_tracker",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "OptimaLaz"
        ));

        trackerManager = new TrackerManager();
        handRenderer = new TrackerHandRenderer();
        healthBarRenderer = new HealthBarRenderer();
        damageIndicatorManager = new DamageIndicatorManager();
        optimizationManager = new OptimizationManager();

        optimizationManager.applyStartupOptimizations();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openTrackerKey.wasPressed()) {
                if (client.world != null && client.player != null) {
                    client.setScreen(new PlayerTrackerScreen(client.currentScreen));
                }
            }
            if (client.world != null && client.player != null) {
                trackerManager.tick(client);
                damageIndicatorManager.tick(client);
                optimizationManager.tick(client);
            }
        });

        WorldRenderEvents.LAST.register(context -> {
            if (context.world() != null && context.camera() != null) {
                handRenderer.render(context);
                healthBarRenderer.render(context);
                damageIndicatorManager.render(context);
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            ArmorHudRenderer.render(drawContext);
        });
    }
}
