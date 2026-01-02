package com.dodgingbullets.gameobjects.environment;

import com.dodgingbullets.core.Vec2;
import com.dodgingbullets.gameobjects.*;

public class AmmoPowerUp extends GameObject implements Renderable, Collidable {
    private boolean collected = false;
    
    public AmmoPowerUp(float x, float y) {
        super(x, y);
    }
    
    @Override
    public void update(float deltaTime) {
        // No update logic needed
    }
    
    @Override
    public void render() {
        // Rendering handled by GameRenderer
    }
    
    @Override
    public float getRenderY() {
        return position.y();
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        Vec2 otherPos = new Vec2(x, y);
        Vec2 halfSize = new Vec2(32, 32);
        Vec2 min = position.subtract(halfSize);
        Vec2 max = position.add(halfSize);
        return otherPos.x() >= min.x() && otherPos.x() <= max.x() && 
               otherPos.y() >= min.y() && otherPos.y() <= max.y();
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        return false; // No movement blocking
    }
    
    public boolean isCollected() {
        return collected;
    }
    
    public void collect() {
        collected = true;
    }
}
