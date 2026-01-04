package com.dodgingbullets.gameobjects.environment;

import com.dodgingbullets.gameobjects.*;

public class Foliage extends GameObject implements Renderable, Collidable {
    private final float spriteWidth;
    private final float spriteHeight;
    private final float spriteCollisionWidth;
    private final float spriteCollisionHeight;
    private final float movementCollisionWidth;
    private final float movementCollisionHeight;
    private final String textureKey;

    public Foliage(float x, float y, float width, float height, float spriteCollisionWidth, float spriteCollisionHeight, 
                   float movementCollisionWidth, float movementCollisionHeight, String textureKey) {
        super(x, y);
        this.spriteWidth = width;
        this.spriteHeight = height;
        this.spriteCollisionWidth = spriteCollisionWidth;
        this.spriteCollisionHeight = spriteCollisionHeight;
        this.movementCollisionWidth = movementCollisionWidth;
        this.movementCollisionHeight = movementCollisionHeight;
        this.textureKey = textureKey;
    }
    
    public String getTextureKey() {
        return textureKey;
    }
    
    public float getSpriteWidth() {
        return spriteWidth;
    }
    
    public float getSpriteHeight() {
        return spriteHeight;
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
        // Sprite collision is offset from bottom of sprite
        float bottomY = position.y() - spriteHeight/2;
        return x < position.x() + spriteCollisionWidth/2 && x + width > position.x() - spriteCollisionWidth/2 && 
               y < bottomY + spriteCollisionHeight && y + height > bottomY;
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        // Movement collision is offset from bottom of sprite
        float bottomY = position.y() - spriteHeight/2;
        return x < position.x() + movementCollisionWidth/2 && x + width > position.x() - movementCollisionWidth/2 && 
               y < bottomY + movementCollisionHeight && y + height > bottomY;
    }
}
