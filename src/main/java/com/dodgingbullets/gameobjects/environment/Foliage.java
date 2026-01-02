package com.dodgingbullets.gameobjects.environment;

import com.dodgingbullets.gameobjects.*;

public class Foliage extends GameObject implements Renderable, Collidable {
    private static final float SPRITE_SIZE = 50f;

    public Foliage(float x, float y) {
        super(x, y);
    }
    
    @Override
    public void update(float deltaTime) {
        // Static object - no update needed
    }
    
    @Override
    public void render() {
        // Rendering handled by renderer
    }
    
    @Override
    public float getRenderY() {
        return position.y(); // Use center Y for depth sorting (same as turret)
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        // Full sprite hitbox (same pattern as turret)
        return x < position.x() + SPRITE_SIZE/2 && x + width > position.x() - SPRITE_SIZE/2 && 
               y < position.y() + SPRITE_SIZE/2 && y + height > position.y() - SPRITE_SIZE/2;
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        // Bottom half for movement blocking (same pattern as turret: y < this.y)
        return x < position.x() + SPRITE_SIZE/2 && x + width > position.x() - SPRITE_SIZE/2 && 
               y < position.y() && y + height > position.y() - SPRITE_SIZE/2;
    }
}
