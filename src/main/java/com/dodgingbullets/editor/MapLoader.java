package com.dodgingbullets.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MapLoader {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static LevelData loadLevel(String filename) {
        try {
            String filePath = "src/main/resources/maps/" + filename;
            if (!Files.exists(Paths.get(filePath))) {
                return createDefaultLevel();
            }
            
            try (FileReader reader = new FileReader(filePath)) {
                return gson.fromJson(reader, LevelData.class);
            }
        } catch (IOException e) {
            System.err.println("Error loading level: " + e.getMessage());
            return createDefaultLevel();
        }
    }

    public static void saveLevel(String filename, LevelData levelData) throws IOException {
        String filePath = "src/main/resources/maps/" + filename;
        
        // Ensure directory exists
        Files.createDirectories(Paths.get(filePath).getParent());
        
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(levelData, writer);
        }
    }

    private static LevelData createDefaultLevel() {
        LevelData level = new LevelData();
        level.backgroundTexture = "vibrant_random_grass.png";
        level.mapWidth = 768;
        level.mapHeight = 512;
        
        // Initialize default 6x4 grid
        level.mapGrid = new String[4][6];
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 6; x++) {
                level.mapGrid[y][x] = "floorgrey1.png";
            }
        }
        
        // Initialize empty lists
        level.turrets = new java.util.ArrayList<>();
        level.bears = new java.util.ArrayList<>();
        level.foliage = new java.util.ArrayList<>();
        level.ammoPowerUps = new java.util.ArrayList<>();
        level.throwers = new java.util.ArrayList<>();
        
        // Default player position
        level.player = new LevelData.PlayerData();
        level.player.x = 200;
        level.player.y = 200;
        
        return level;
    }
}
