package com.dodgingbullets.gameobjects.effects;

import com.dodgingbullets.core.Vec2;
import com.dodgingbullets.gameobjects.*;
import java.util.List;

public class PetrolBomb extends GameObject implements Renderable, Collidable {
    
    private Vec2 velocity;
    private float verticalVelocity;
    private float height;
    private float rotation = 0f;
    private float rotationSpeed;
    private boolean hasLanded = false;
    private List<GameObject> collidableObjects;
    
    // Constants
    private static final float GRAVITY = 300f;
    private static final float INITIAL_HEIGHT = 20f;
    private static final float GROUND_LEVEL = 0f;
    private static final float EXPLOSION_RADIUS = 80f;
    
    public PetrolBomb(float x, float y, Vec2 velocity, List<GameObject> collidableObjects) {
        super(x, y);
        this.velocity = velocity;
        this.verticalVelocity = 150f; // Initial upward velocity
        this.height = INITIAL_HEIGHT;
        this.collidableObjects = collidableObjects;
        
        // Set rotation direction based on horizontal velocity
        this.rotationSpeed = (velocity.x() > 0) ? 180f : -180f; // Clockwise for east, counter-clockwise for west
    }
    
    @Override
    public void update(float deltaTime) {
        if (hasLanded) {
            active = false;
            return;
        }
        
        // Update horizontal position
        position = position.add(velocity.multiply(deltaTime));
        
        // Update vertical physics
        verticalVelocity -= GRAVITY * deltaTime;
        height += verticalVelocity * deltaTime;
        
        // Update rotation
        rotation += rotationSpeed * deltaTime;
        
        // Check if landed
        if (height <= GROUND_LEVEL) {
            height = GROUND_LEVEL;
            land();
        }
    }
    
    private void land() {
        hasLanded = true;
        
        // Create explosion at landing position
        Explosion explosion = new Explosion(position.x(), position.y());
        
        // Add explosion to the game (this will be handled by GameLoop)
        // For now, we'll mark this bomb as inactive and let GameLoop handle explosion creation
        active = false;
    }
    
    public boolean shouldCreateExplosion() {
        return hasLanded;
    }
    
    public Vec2 getExplosionPosition() {
        return position;
    }
    
    // Shadow properties for rendering
    public float getShadowScale() {
        // Shadow gets larger as bomb gets higher
        float maxHeight = 100f; // Maximum expected height
        float scale = 0.5f + (height / maxHeight) * 0.5f; // Scale from 0.5 to 1.0
        return Math.max(0.3f, Math.min(1.0f, scale));
    }
    
    public Vec2 getShadowPosition() {
        // Shadow stays on the ground directly below the bomb
        return position;
    }
    
    // Interface implementations
    @Override
    public float getRenderY() {
        return position.y() + height; // Render based on ground position + height for depth sorting
    }
    
    @Override
    public void render() {
        // Rendering handled by GameRenderer
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        // Small collision box for the bomb
        Vec2 testPos = new Vec2(x + width/2, y + height/2);
        Vec2 bombCenter = new Vec2(position.x(), position.y() + this.height);
        return testPos.distance(bombCenter) <= 16f; // 16 pixel radius
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        // Bombs don't block movement
        return false;
    }
    
    // Getters for rendering
    public float getHeight() { return height; }
    public float getRotation() { return rotation; }
    public Vec2 getVelocity() { return velocity; }
}
