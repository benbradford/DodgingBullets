package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.enemies.Bear;
import com.dodgingbullets.gameobjects.enemies.Thrower;
import com.dodgingbullets.gameobjects.enemies.Mortar;
import com.dodgingbullets.gameobjects.environment.Foliage;
import com.dodgingbullets.gameobjects.environment.AmmoPowerUp;

import java.io.*;
import java.util.*;

public class MapLoader {
    
    public static class MapData {
        public String[][] mapGrid = {{"floorgrey6.png"}}; // default 1x1 grid
        public float mapWidth = 128; // calculated from grid
        public float mapHeight = 128; // calculated from grid
        public List<GameObject> turrets = new ArrayList<>();
        public List<GameObject> foliage = new ArrayList<>();
        public List<GameObject> ammoPowerUps = new ArrayList<>();
        public List<GameObject> bears = new ArrayList<>();
        public List<GameObject> throwers = new ArrayList<>();
        public List<GameObject> mortars = new ArrayList<>();
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
        // Extract map grid
        if (json.contains("\"mapGrid\":")) {
            String mapGridSection = extractSection(json, "\"mapGrid\":");
            parseMapGrid(mapGridSection, mapData);
        }
        
        // Parse each section by finding its specific key
        if (json.contains("\"turrets\":")) {
            String turretsSection = extractSection(json, "\"turrets\":");
            parseTurrets(turretsSection, mapData);
        }
        
        if (json.contains("\"foliage\":")) {
            String foliageSection = extractSection(json, "\"foliage\":");
            parseFoliage(foliageSection, mapData);
        }
        
        if (json.contains("\"ammoPowerUps\":")) {
            String ammoSection = extractSection(json, "\"ammoPowerUps\":");
            parseAmmoPowerUps(ammoSection, mapData);
        }
        
        if (json.contains("\"bears\":")) {
            String bearsSection = extractSection(json, "\"bears\":");
            parseBears(bearsSection, mapData);
        }
        
        if (json.contains("\"throwers\":")) {
            String throwersSection = extractSection(json, "\"throwers\":");
            parseThrowers(throwersSection, mapData);
        }
        
        if (json.contains("\"mortars\":")) {
            String mortarsSection = extractSection(json, "\"mortars\":");
            parseMortars(mortarsSection, mapData);
        }
        
        if (json.contains("\"player\":")) {
            String playerSection = extractSection(json, "\"player\":");
            parsePlayer(playerSection, mapData);
        }
    }
    
    private static String extractSection(String json, String key) {
        int start = json.indexOf(key) + key.length();
        // Skip whitespace and colon
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == ':')) {
            start++;
        }
        
        if (start >= json.length()) return "";
        
        char startChar = json.charAt(start);
        if (startChar == '[') {
            // Array section
            int bracketCount = 1;
            int end = start + 1;
            while (end < json.length() && bracketCount > 0) {
                if (json.charAt(end) == '[') bracketCount++;
                else if (json.charAt(end) == ']') bracketCount--;
                end++;
            }
            return json.substring(start, end);
        } else if (startChar == '{') {
            // Object section
            int braceCount = 1;
            int end = start + 1;
            while (end < json.length() && braceCount > 0) {
                if (json.charAt(end) == '{') braceCount++;
                else if (json.charAt(end) == '}') braceCount--;
                end++;
            }
            return json.substring(start, end);
        }
        
        return "";
    }
    
    private static void parseMapGrid(String section, MapData mapData) {
        // Remove brackets and split by rows
        String content = section.substring(1, section.length() - 1).trim();
        String[] rows = content.split("\\],\\s*\\[");
        
        // Clean up first and last row brackets
        if (rows.length > 0) {
            rows[0] = rows[0].replaceFirst("^\\[", "");
            rows[rows.length - 1] = rows[rows.length - 1].replaceFirst("\\]$", "");
        }
        
        mapData.mapGrid = new String[rows.length][];
        
        for (int i = 0; i < rows.length; i++) {
            String[] tiles = rows[i].split(",");
            mapData.mapGrid[i] = new String[tiles.length];
            
            for (int j = 0; j < tiles.length; j++) {
                String tile = tiles[j].trim();
                // Remove quotes
                if (tile.startsWith("\"") && tile.endsWith("\"")) {
                    tile = tile.substring(1, tile.length() - 1);
                }
                mapData.mapGrid[i][j] = tile;
            }
        }
        
        // Calculate map dimensions from grid
        mapData.mapHeight = mapData.mapGrid.length * 128;
        mapData.mapWidth = mapData.mapGrid[0].length * 128;
    }
    
    private static void parseTurrets(String section, MapData mapData) {
        String[] objects = section.split("\\{");
        for (String obj : objects) {
            if (obj.contains("\"x\":")) {
                int x = extractInt(obj, "\"x\":");
                int y = extractInt(obj, "\"y\":");
                int health = extractIntWithDefault(obj, "\"health\":", 100);
                GunTurret turret = new GunTurret(x, y, health);
                mapData.turrets.add(turret);
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
    
    private static void parseBears(String section, MapData mapData) {
        String[] objects = section.split("\\{");
        for (String obj : objects) {
            if (obj.contains("\"x\":")) {
                int x = extractInt(obj, "\"x\":");
                int y = extractInt(obj, "\"y\":");
                String facingStr = obj.contains("\"facing\":") ? extractString(obj, "\"facing\":") : "east";
                int health = extractIntWithDefault(obj, "\"health\":", 100);
                float speed = extractFloatWithDefault(obj, "\"speed\":", 150f);
                Direction facing = parseFacingDirection(facingStr);
                Bear bear = new Bear(x, y, facing, health, speed);
                mapData.bears.add(bear);
            }
        }
    }
    
    private static void parseThrowers(String section, MapData mapData) {
        String[] objects = section.split("\\{");
        for (String obj : objects) {
            if (obj.contains("\"x\":")) {
                int x = extractInt(obj, "\"x\":");
                int y = extractInt(obj, "\"y\":");
                String facingStr = obj.contains("\"facing\":") ? extractString(obj, "\"facing\":") : "east";
                int health = extractIntWithDefault(obj, "\"health\":", 100);
                float speed = extractFloatWithDefault(obj, "\"speed\":", 100f);
                Direction facing = parseFacingDirection(facingStr);
                // Note: Thrower constructor needs collidableObjects and petrolBombs lists
                // These will be set later in GameLoop initialization
                Thrower thrower = new Thrower(x, y, facing, null, null, health, speed);
                mapData.throwers.add(thrower);
            }
        }
    }
    
    private static void parseMortars(String section, MapData mapData) {
        String[] objects = section.split("\\{");
        for (String obj : objects) {
            if (obj.contains("\"x\":")) {
                int x = extractInt(obj, "\"x\":");
                int y = extractInt(obj, "\"y\":");
                String lookDirectionStr = obj.contains("\"lookDirection\":") ? extractString(obj, "\"lookDirection\":") : "east";
                float lookDistance = extractFloatWithDefault(obj, "\"lookDistance\":", 300f);
                int health = extractIntWithDefault(obj, "\"health\":", 30);
                float firingSpeed = extractFloatWithDefault(obj, "\"firingSpeed\":", 2f);
                Direction lookDirection = parseFacingDirection(lookDirectionStr);
                Mortar mortar = new Mortar(x, y, lookDirection, lookDistance, health, firingSpeed);
                mapData.mortars.add(mortar);
            }
        }
    }
    
    private static Direction parseFacingDirection(String facingStr) {
        switch (facingStr.toLowerCase()) {
            case "north": return Direction.UP;
            case "south": return Direction.DOWN;
            case "east": return Direction.RIGHT;
            case "west": return Direction.LEFT;
            case "north-east": return Direction.UP_RIGHT;
            case "north-west": return Direction.UP_LEFT;
            case "south-east": return Direction.DOWN_RIGHT;
            case "south-west": return Direction.DOWN_LEFT;
            default: return Direction.RIGHT; // Default to east
        }
    }
    
    private static void parsePlayer(String section, MapData mapData) {
        int x = extractInt(section, "\"x\":");
        int y = extractInt(section, "\"y\":");
        mapData.player = new Player(x, y);
    }
    
    private static int extractIntWithDefault(String json, String key, int defaultValue) {
        try {
            return extractInt(json, key);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private static float extractFloatWithDefault(String json, String key, float defaultValue) {
        try {
            return extractFloat(json, key);
        } catch (Exception e) {
            return defaultValue;
        }
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
        mapData.mapGrid = new String[][]{
            {"floorgrey1.png", "floorgrey2.png", "floorgrey3.png"},
            {"floorgrey4.png", "floorgrey5.png", "floorgrey6.png"}
        };
        mapData.mapWidth = 3 * 128;
        mapData.mapHeight = 2 * 128;
        
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
        
        // Hardcoded bears
        mapData.bears.add(new Bear(1000, 1000, Direction.LEFT));
        mapData.bears.add(new Bear(1800, 300, Direction.LEFT));
        mapData.bears.add(new Bear(400, 800, Direction.RIGHT));
        
        // Hardcoded player
        mapData.player = new Player(320, 180);
        
        return mapData;
    }
}
