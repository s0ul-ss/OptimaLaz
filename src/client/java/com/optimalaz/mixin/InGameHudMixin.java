package com.optimalaz.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderPlayerList", at = @At("TAIL"))
    private void onRenderPlayerList(int width, DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.player == null) return;
        if (!client.options.playerListKey.isPressed()) return;

        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        if (entries == null || entries.isEmpty()) return;

        for (PlayerListEntry entry : entries) {
            String playerName = entry.getProfile().getName();
            if (playerName.equals(client.player.getGameProfile().getName())) continue;

            float hp = -1;
            if (client.world != null) {
                Vec3d myPos = client.player.getPos();
                for (var entity : client.world.getEntitiesByClass(PlayerEntity.class,
                        new Box(myPos.x - 200, myPos.y - 200, myPos.z - 200,
                                myPos.x + 200, myPos.y + 200, myPos.z + 200),
                        e -> e.getGameProfile().getName().equals(playerName))) {
                    hp = entity.getHealth();
                    break;
                }
            }
        }
    }
}
