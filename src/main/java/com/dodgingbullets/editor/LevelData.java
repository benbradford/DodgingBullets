package com.dodgingbullets.editor;

import java.util.List;

public class LevelData {
    public String backgroundTexture;
    public int mapWidth;
    public int mapHeight;
    public String[][] mapGrid;
    public List<TurretData> turrets;
    public List<BearData> bears;
    public List<FoliageData> foliage;
    public List<PowerUpData> ammoPowerUps;
    public List<ThrowerData> throwers;
    public PlayerData player;

    public static class TurretData {
        public int x;
        public int y;
        public int health = 100;  // Default health
        public float speed = 0;   // Turrets don't move
    }

    public static class BearData {
        public int x;
        public int y;
        public String facing;
        public int health = 100;    // Default health
        public float speed = 150f;  // Default speed
    }

    public static class FoliageData {
        public int x;
        public int y;
        public int width;
        public int height;
        public int spriteCollisionWidth;
        public int spriteCollisionHeight;
        public int movementCollisionWidth;
        public int movementCollisionHeight;
        public String textureKey;
        public int renderOffset;
    }

    public static class PowerUpData {
        public int x;
        public int y;
    }

    public static class ThrowerData {
        public int x;
        public int y;
        public String facing;
        public int health = 100;    // Default health
        public float speed = 100f;  // Default speed
    }

    public static class PlayerData {
        public int x;
        public int y;
    }
}
