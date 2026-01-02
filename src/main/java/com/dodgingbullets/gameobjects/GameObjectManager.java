package com.dodgingbullets.gameobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameObjectManager {
    private List<GameObject> gameObjects = new ArrayList<>();
    
    public void addGameObject(GameObject obj) {
        gameObjects.add(obj);
    }
    
    public void removeGameObject(GameObject obj) {
        gameObjects.remove(obj);
    }
    
    public void updateAll(float deltaTime) {
        // Remove inactive objects
        gameObjects.removeIf(obj -> !obj.isActive());
        
        // Update all active objects
        for (GameObject obj : gameObjects) {
            obj.update(deltaTime);
        }
    }
    
    public List<Renderable> getRenderables() {
        return gameObjects.stream()
            .filter(obj -> obj instanceof Renderable)
            .map(obj -> (Renderable) obj)
            .sorted((a, b) -> Float.compare(a.getRenderY(), b.getRenderY()))
            .collect(Collectors.toList());
    }
    
    public List<Collidable> getCollidables() {
        return gameObjects.stream()
            .filter(obj -> obj instanceof Collidable)
            .map(obj -> (Collidable) obj)
            .collect(Collectors.toList());
    }
    
    public List<EnemyObject> getEnemies() {
        return gameObjects.stream()
            .filter(obj -> obj instanceof EnemyObject)
            .map(obj -> (EnemyObject) obj)
            .collect(Collectors.toList());
    }
}
