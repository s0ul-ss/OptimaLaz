package com.playertracker.client;

import com.playertracker.client.render.TrackerHandRenderer;
import com.playertracker.client.screen.PlayerTrackerScreen;
import com.playertracker.client.tracker.TrackerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class PlayerTrackerClient implements ClientModInitializer {

    public static KeyBinding openMenuKey;
    public static TrackerManager trackerManager;
    public static TrackerHandRenderer handRenderer;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.playertracker.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "PlayerTracker"
        ));

        trackerManager = new TrackerManager();
        handRenderer = new TrackerHandRenderer();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.world != null && client.player != null) {
                    client.setScreen(new PlayerTrackerScreen(client.currentScreen));
                }
            }
            if (client.world != null && client.player != null) {
                trackerManager.tick(client);
            }
        });

        WorldRenderEvents.LAST.register(context -> {
            if (context.world() != null && context.camera() != null) {
                handRenderer.render(context);
            }
        });
    }
}
