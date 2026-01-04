package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.enemies.Bear;
import com.dodgingbullets.gameobjects.environment.Foliage;
import com.dodgingbullets.gameobjects.environment.AmmoPowerUp;

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
    
    public static Player createPlayer() {
        return mapData.player;
    }
    
    public static String getBackgroundTexture() {
        return mapData.backgroundTexture;
    }
    
    public static float getMapWidth() {
        return mapData.mapWidth;
    }
    
    public static float getMapHeight() {
        return mapData.mapHeight;
    }
}
