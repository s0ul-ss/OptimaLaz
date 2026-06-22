package com.optimalaz.client.tracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class TrackerManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, TrackedPlayer> trackedPlayers = new LinkedHashMap<>();
    private File saveFile;
    private String currentServerAddress = null;

    public void tick(MinecraftClient client) {
        if (client.getNetworkHandler() == null) return;
        String serverAddress = getServerKey(client);
        if (!serverAddress.equals(currentServerAddress)) {
            currentServerAddress = serverAddress;
            loadFromFile(client);
        }
        Collection<PlayerListEntry> playerList = client.getNetworkHandler().getPlayerList();
        String myName = client.player != null ? client.player.getGameProfile().getName() : "";

        for (Map.Entry<String, TrackedPlayer> entry : new ArrayList<>(trackedPlayers.entrySet())) {
            String name = entry.getKey();
            TrackedPlayer tracked = entry.getValue();
            boolean found = false;
            for (PlayerListEntry ple : playerList) {
                if (ple.getProfile().getName().equals(name) && !name.equals(myName)) {
                    found = true;
                    PlayerEntity pe = null;
                    if (client.world != null) {
                        for (var entity : client.world.getEntities()) {
                            if (entity instanceof PlayerEntity p && p.getGameProfile().getName().equals(name)) {
                                pe = p; break;
                            }
                        }
                    }
                    if (pe != null) {
                        tracked.setX(pe.getX()); tracked.setY(pe.getY()); tracked.setZ(pe.getZ());
                        tracked.setDimension(getDimKey(client.world));
                        tracked.setOnline(true); tracked.setVisible(true);
                    } else {
                        tracked.setOnline(true); tracked.setVisible(false);
                    }
                    break;
                }
            }
            if (!found) { tracked.setOnline(false); tracked.setVisible(false); }
        }
    }

    private String getDimKey(World world) {
        if (world == null) return "overworld";
        if (world.getRegistryKey().equals(World.NETHER)) return "nether";
        if (world.getRegistryKey().equals(World.END)) return "end";
        return "overworld";
    }

    private String getServerKey(MinecraftClient client) {
        if (client.getCurrentServerEntry() != null) return client.getCurrentServerEntry().address;
        return "singleplayer";
    }

    public void addTracker(String name) {
        if (!trackedPlayers.containsKey(name)) { trackedPlayers.put(name, new TrackedPlayer(name)); saveToFile(); }
    }

    public void removeTracker(String name) { trackedPlayers.remove(name); saveToFile(); }
    public boolean isTracked(String name) { return trackedPlayers.containsKey(name); }
    public Map<String, TrackedPlayer> getTrackedPlayers() { return Collections.unmodifiableMap(trackedPlayers); }

    private void saveToFile() {
        if (saveFile == null) return;
        try {
            saveFile.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(saveFile)) {
                GSON.toJson(new ArrayList<>(trackedPlayers.keySet()), w);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadFromFile(MinecraftClient client) {
        String key = currentServerAddress.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        saveFile = new File(client.runDirectory, "config/optimalaz/" + key + ".json");
        trackedPlayers.clear();
        if (!saveFile.exists()) return;
        try (FileReader r = new FileReader(saveFile)) {
            Type t = new TypeToken<List<String>>(){}.getType();
            List<String> names = GSON.fromJson(r, t);
            if (names != null) names.forEach(n -> trackedPlayers.put(n, new TrackedPlayer(n)));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public List<String> getAllOnlinePlayers(MinecraftClient client) {
        List<String> result = new ArrayList<>();
        if (client.getNetworkHandler() == null) return result;
        String myName = client.player != null ? client.player.getGameProfile().getName() : "";
        for (PlayerListEntry e : client.getNetworkHandler().getPlayerList()) {
            String n = e.getProfile().getName();
            if (!n.equals(myName)) result.add(n);
        }
        result.sort(String::compareToIgnoreCase);
        return result;
    }
}
