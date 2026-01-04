package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.environment.Foliage;
import com.dodgingbullets.gameobjects.environment.AmmoPowerUp;

import java.io.*;
import java.util.*;

public class MapLoader {
    
    public static class MapData {
        public String backgroundTexture = "vibrant_random_grass.png"; // default
        public float mapWidth = 2560; // default
        public float mapHeight = 1440; // default
        public List<GameObject> turrets = new ArrayList<>();
        public List<GameObject> foliage = new ArrayList<>();
        public List<GameObject> ammoPowerUps = new ArrayList<>();
        public Player player;
    }
    
    public static MapData loadMap(String mapPath) {
        MapData mapData = new MapData();
        
        try (InputStream is = MapLoader.class.getClassLoader().getResourceAsStream(mapPath);
             Scanner scanner = new Scanner(is)) {
            
            StringBuilder json = new StringBuilder();
            while (scanner.hasNextLine()) {
                json.append(scanner.nextLine());
            }
            
            parseJson(json.toString(), mapData);
            
        } catch (Exception e) {
            System.err.println("Failed to load map: " + mapPath + " - " + e.getMessage());
            // Return default hardcoded map as fallback
            return createDefaultMap();
        }
        
        return mapData;
    }
    
    private static void parseJson(String json, MapData mapData) {
        // Extract background texture
        if (json.contains("\"backgroundTexture\":")) {
            mapData.backgroundTexture = extractString(json, "\"backgroundTexture\":");
        }
        
        // Extract map dimensions
        if (json.contains("\"mapWidth\":")) {
            mapData.mapWidth = extractFloat(json, "\"mapWidth\":");
        }
        if (json.contains("\"mapHeight\":")) {
            mapData.mapHeight = extractFloat(json, "\"mapHeight\":");
        }
        
        // Simple JSON parsing without external libraries
        String[] sections = json.split("\"turrets\":|\"foliage\":|\"ammoPowerUps\":|\"player\":");
        
        for (int i = 1; i < sections.length; i++) {
            String section = sections[i].trim();
            if (section.startsWith("[")) {
                if (i == 1) parseTurrets(section, mapData);
                else if (i == 2) parseFoliage(section, mapData);
                else if (i == 3) parseAmmoPowerUps(section, mapData);
            } else if (section.startsWith("{")) {
                parsePlayer(section, mapData);
            }
        }
    }
    
    private static void parseTurrets(String section, MapData mapData) {
        String[] objects = section.split("\\{");
        for (String obj : objects) {
            if (obj.contains("\"x\":")) {
                int x = extractInt(obj, "\"x\":");
                int y = extractInt(obj, "\"y\":");
                mapData.turrets.add(new GunTurret(x, y));
            }
        }
    }
    
    private static void parseFoliage(String section, MapData mapData) {
        String[] objects = section.split("\\{");
        for (String obj : objects) {
            if (obj.contains("\"x\":")) {
                float x = extractFloat(obj, "\"x\":");
                float y = extractFloat(obj, "\"y\":");
                float width = extractFloat(obj, "\"width\":");
                float height = extractFloat(obj, "\"height\":");
                float scw = extractFloat(obj, "\"spriteCollisionWidth\":");
                float sch = extractFloat(obj, "\"spriteCollisionHeight\":");
                float mcw = extractFloat(obj, "\"movementCollisionWidth\":");
                float mch = extractFloat(obj, "\"movementCollisionHeight\":");
                String textureKey = extractString(obj, "\"textureKey\":");
                float renderOffset = extractFloat(obj, "\"renderOffset\":");
                
                mapData.foliage.add(new Foliage(x, y, width, height, scw, sch, mcw, mch, textureKey, renderOffset));
            }
        }
    }
    
    private static void parseAmmoPowerUps(String section, MapData mapData) {
        String[] objects = section.split("\\{");
        for (String obj : objects) {
            if (obj.contains("\"x\":")) {
                int x = extractInt(obj, "\"x\":");
                int y = extractInt(obj, "\"y\":");
                mapData.ammoPowerUps.add(new AmmoPowerUp(x, y));
            }
        }
    }
    
    private static void parsePlayer(String section, MapData mapData) {
        int x = extractInt(section, "\"x\":");
        int y = extractInt(section, "\"y\":");
        mapData.player = new Player(x, y);
    }
    
    private static int extractInt(String text, String key) {
        int start = text.indexOf(key) + key.length();
        while (start < text.length() && !Character.isDigit(text.charAt(start)) && text.charAt(start) != '-') {
            start++;
        }
        int end = start;
        while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '-')) {
            end++;
        }
        return Integer.parseInt(text.substring(start, end));
    }
    
    private static float extractFloat(String text, String key) {
        int start = text.indexOf(key) + key.length();
        while (start < text.length() && !Character.isDigit(text.charAt(start)) && text.charAt(start) != '-' && text.charAt(start) != '.') {
            start++;
        }
        int end = start;
        while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '-' || text.charAt(end) == '.')) {
            end++;
        }
        return Float.parseFloat(text.substring(start, end));
    }
    
    private static String extractString(String text, String key) {
        int start = text.indexOf(key) + key.length();
        start = text.indexOf('"', start) + 1;
        int end = text.indexOf('"', start);
        return text.substring(start, end);
    }
    
    private static MapData createDefaultMap() {
        // Fallback to original hardcoded values
        MapData mapData = new MapData();
        mapData.backgroundTexture = "vibrant_random_grass.png";
        
        // Hardcoded turrets
        mapData.turrets.add(new GunTurret(800, 150));
        mapData.turrets.add(new GunTurret(1200, 400));
        mapData.turrets.add(new GunTurret(600, 600));
        
        // Hardcoded foliage
        mapData.foliage.add(new Foliage(400, 300, 50f, 50f, 50f, 30f, 50f, 25f, "foliage", 10f));
        mapData.foliage.add(new Foliage(1000, 200, 50f, 50f, 50f, 30f, 50f, 25f, "foliage", 10f));
        
        // Hardcoded ammo power-ups
        mapData.ammoPowerUps.add(new AmmoPowerUp(500, 500));
        mapData.ammoPowerUps.add(new AmmoPowerUp(1400, 300));
        
        // Hardcoded player
        mapData.player = new Player(320, 180);
        
        return mapData;
    }
}
