package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.environment.Foliage;

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
        foliages.add(new Foliage(400, 300));
        foliages.add(new Foliage(1000, 200));
        foliages.add(new Foliage(1500, 800));
        foliages.add(new Foliage(200, 900));
        foliages.add(new Foliage(1800, 600));
        return foliages;
    }
    
    public static Player createPlayer() {
        return new Player(320, 180);
    }
}
