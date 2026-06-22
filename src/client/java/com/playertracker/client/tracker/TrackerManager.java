package com.playertracker.client.tracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class TrackerManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, TrackedPlayer> trackedPlayers = new LinkedHashMap<>();
    private File saveFile;
    private String currentServerAddress = null;

    public TrackerManager() {
    }

    public void tick(MinecraftClient client) {
        if (client.getNetworkHandler() == null) return;

        String serverAddress = getCurrentServerKey(client);
        if (!serverAddress.equals(currentServerAddress)) {
            currentServerAddress = serverAddress;
            loadFromFile(client);
        }

        Collection<PlayerListEntry> playerList = client.getNetworkHandler().getPlayerList();

        for (Map.Entry<String, TrackedPlayer> entry : new ArrayList<>(trackedPlayers.entrySet())) {
            String name = entry.getKey();
            TrackedPlayer tracked = entry.getValue();

            boolean found = false;
            for (PlayerListEntry playerEntry : playerList) {
                String entryName = playerEntry.getProfile().getName();
                if (entryName.equals(name) && !entryName.equals(
                        client.player != null ? client.player.getGameProfile().getName() : "")) {
                    found = true;

                    net.minecraft.entity.player.PlayerEntity playerEntity = null;
                    for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
                        if (entity instanceof net.minecraft.entity.player.PlayerEntity pe) {
                            if (pe.getGameProfile().getName().equals(name)) {
                                playerEntity = pe;
                                break;
                            }
                        }
                    }

                    if (playerEntity != null) {
                        tracked.setX(playerEntity.getX());
                        tracked.setY(playerEntity.getY());
                        tracked.setZ(playerEntity.getZ());
                        tracked.setDimension(getDimensionKey(client.world));
                        tracked.setOnline(true);
                        tracked.setVisible(true);
                    } else {
                        tracked.setOnline(true);
                        tracked.setVisible(false);
                    }
                    break;
                }
            }

            if (!found) {
                tracked.setOnline(false);
                tracked.setVisible(false);
            }
        }
    }

    private String getDimensionKey(World world) {
        if (world.getRegistryKey().equals(World.NETHER)) return "nether";
        if (world.getRegistryKey().equals(World.END)) return "end";
        return "overworld";
    }

    private String getCurrentServerKey(MinecraftClient client) {
        if (client.getCurrentServerEntry() != null) {
            return client.getCurrentServerEntry().address;
        }
        return "singleplayer";
    }

    public void addTracker(String playerName) {
        if (!trackedPlayers.containsKey(playerName)) {
            trackedPlayers.put(playerName, new TrackedPlayer(playerName));
            saveToFile();
        }
    }

    public void removeTracker(String playerName) {
        trackedPlayers.remove(playerName);
        saveToFile();
    }

    public boolean isTracked(String playerName) {
        return trackedPlayers.containsKey(playerName);
    }

    public Map<String, TrackedPlayer> getTrackedPlayers() {
        return Collections.unmodifiableMap(trackedPlayers);
    }

    private void saveToFile() {
        if (saveFile == null) return;
        try {
            saveFile.getParentFile().mkdirs();
            List<String> names = new ArrayList<>(trackedPlayers.keySet());
            try (FileWriter writer = new FileWriter(saveFile)) {
                GSON.toJson(names, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFromFile(MinecraftClient client) {
        String key = currentServerAddress.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        saveFile = new File(client.runDirectory, "config/playertracker/" + key + ".json");

        trackedPlayers.clear();

        if (!saveFile.exists()) return;

        try (FileReader reader = new FileReader(saveFile)) {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> names = GSON.fromJson(reader, listType);
            if (names != null) {
                for (String name : names) {
                    trackedPlayers.put(name, new TrackedPlayer(name));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllOnlinePlayers(MinecraftClient client) {
        List<String> result = new ArrayList<>();
        if (client.getNetworkHandler() == null) return result;
        String myName = client.player != null ? client.player.getGameProfile().getName() : "";
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            if (!name.equals(myName)) {
                result.add(name);
            }
        }
        result.sort(String::compareToIgnoreCase);
        return result;
    }
}
