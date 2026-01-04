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
    private final float renderOffset;

    public Foliage(float x, float y, float width, float height, float spriteCollisionWidth, float spriteCollisionHeight, 
                   float movementCollisionWidth, float movementCollisionHeight, String textureKey, float renderOffset) {
        super(x, y);
        this.spriteWidth = width;
        this.spriteHeight = height;
        this.spriteCollisionWidth = spriteCollisionWidth;
        this.spriteCollisionHeight = spriteCollisionHeight;
        this.movementCollisionWidth = movementCollisionWidth;
        this.movementCollisionHeight = movementCollisionHeight;
        this.textureKey = textureKey;
        this.renderOffset = renderOffset;
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
        return position.y() - renderOffset; // Use configurable offset for proper depth sorting
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        // Sprite collision is offset 30 pixels above bottom of sprite
        float bottomY = position.y() - spriteHeight/2 + 30;
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
