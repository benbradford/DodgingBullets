package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.enemies.Bear;
import com.dodgingbullets.gameobjects.enemies.Thrower;
import com.dodgingbullets.gameobjects.enemies.Mortar;
import com.dodgingbullets.gameobjects.environment.Foliage;
import com.dodgingbullets.gameobjects.environment.AmmoPowerUp;
import com.dodgingbullets.gameobjects.effects.PetrolBomb;
import com.dodgingbullets.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class GameObjectFactory {
    
    private static MapLoader.MapData mapData;
    
    public static void loadLevel(String levelPath) {
        mapData = MapLoader.loadMap(levelPath);
    }
    
    public static List<GameObject> createTurrets() {
        return new ArrayList<>(mapData.turrets);
    }
    
    public static List<GameObject> createFoliage() {
        return new ArrayList<>(mapData.foliage);
    }
    
    public static List<GameObject> createAmmoPowerUps() {
        return new ArrayList<>(mapData.ammoPowerUps);
    }
    
    public static List<GameObject> createBears() {
        return new ArrayList<>(mapData.bears);
    }
    
    public static List<GameObject> createThrowers() {
        return new ArrayList<>(mapData.throwers);
    }
    
    public static List<GameObject> createMortars() {
        return new ArrayList<>(mapData.mortars);
    }
    
    public static Player createPlayer() {
        return mapData.player;
    }
    
    public static String[][] getMapGrid() {
        return mapData.mapGrid;
    }
    
    public static float getMapWidth() {
        return mapData.mapWidth;
    }
    
    public static float getMapHeight() {
        return mapData.mapHeight;
    }
    
    // Individual creation methods for editor
    public static GameObject createTurret(float x, float y, int health) {
        GunTurret turret = new GunTurret(x, y, health);
        return turret;
    }
    
    public static GameObject createBear(float x, float y, String facing, int health, float speed) {
        Direction direction = "west".equals(facing) ? Direction.LEFT : Direction.RIGHT;
        Bear bear = new Bear(x, y, direction, health, speed);
        // TODO: Add setHealth and setSpeed methods to Bear
        return bear;
    }
    
    public static GameObject createFoliage(float x, float y, String textureKey) {
        return new Foliage(x, y, 64, 64, 64, 64, 64, 32, textureKey, 0);
    }
    
    public static GameObject createFoliage(float x, float y, int width, int height, 
                                         int spriteCollisionWidth, int spriteCollisionHeight,
                                         int movementCollisionWidth, int movementCollisionHeight,
                                         String textureKey, int renderOffset) {
        return new Foliage(x, y, width, height, spriteCollisionWidth, spriteCollisionHeight,
                          movementCollisionWidth, movementCollisionHeight, textureKey, renderOffset);
    }
    
    public static GameObject createAmmoPowerUp(float x, float y) {
        return new AmmoPowerUp(x, y);
    }
    
    public static GameObject createThrower(float x, float y, int health, float speed) {
        // For editor, create with minimal parameters
        Thrower thrower = new Thrower(x, y, Direction.RIGHT, new ArrayList<>(), new ArrayList<>(), health, speed);
        // TODO: Add setHealth and setSpeed methods to Thrower
        return thrower;
    }
}
