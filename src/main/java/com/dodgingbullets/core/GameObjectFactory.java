package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.environment.Foliage;
import com.dodgingbullets.gameobjects.environment.AmmoPowerUp;

import java.util.ArrayList;
import java.util.List;

public class GameObjectFactory {
    
    public static List<GameObject> createTurrets() {
        List<GameObject> gameObjects = new ArrayList<>();
        gameObjects.add(new GunTurret(800, 150));
        gameObjects.add(new GunTurret(1200, 400));
        gameObjects.add(new GunTurret(600, 600));
        return gameObjects;
    }
    
    public static List<GameObject> createFoliage() {
        List<GameObject> foliages = new ArrayList<>();
        // Regular foliage
        foliages.add(new Foliage(400, 300, 50f, 50f, 50f, 30f, 50f, 25f, "foliage", 0f));
        foliages.add(new Foliage(1000, 200, 50f, 50f, 50f, 30f, 50f, 25f, "foliage", 0f));
        foliages.add(new Foliage(1500, 800, 50f, 50f, 50f, 30f, 50f, 25f, "foliage", 0f));
        foliages.add(new Foliage(200, 900, 50f, 50f, 50f, 30f, 50f, 25f, "foliage", 0f));
        foliages.add(new Foliage(1800, 600, 50f, 50f, 50f, 30f, 50f, 25f, "foliage", 0f));
        
        // Palm trees (sprite collision for bullets, smaller movement collision for player)
        foliages.add(new Foliage(600, 400, 120f, 120f, 40f, 45f, 50f, 30f, "palm_trees", 30f));
        foliages.add(new Foliage(1200, 700, 120f, 120f, 40f, 45f, 50f, 30f, "palm_trees", 30f));
        foliages.add(new Foliage(300, 600, 120f, 120f, 40f, 45f, 50f, 30f, "palm_trees", 30f));
        
        // Palm tree groups (large forest areas) - 2605x996 scaled to ~500x190
        foliages.add(new Foliage(1800, 1000, 500f, 190f, 380f, 120f, 430f, 120f, "palm_trees_group", 50f));
        foliages.add(new Foliage(800, 1200, 500f, 190f, 380f, 120f, 430f, 120f, "palm_trees_group", 50f));

        
        return foliages;
    }
    
    public static List<GameObject> createAmmoPowerUps() {
        List<GameObject> powerUps = new ArrayList<>();
        powerUps.add(new AmmoPowerUp(500, 500));
        powerUps.add(new AmmoPowerUp(1400, 300));
        return powerUps;
    }
    
    public static Player createPlayer() {
        return new Player(320, 180);
    }
}
