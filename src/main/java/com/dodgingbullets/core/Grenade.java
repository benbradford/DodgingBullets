package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.GameObject;
import com.dodgingbullets.gameobjects.Collidable;
import com.dodgingbullets.gameobjects.Renderable;

import java.util.List;

public class Grenade extends GameObject implements Renderable, Collidable {
    private Vec2 velocity;
    private Vec2 targetPosition;
    private Vec2 startPosition;
    private float flightTime;
    private float totalFlightTime;
    private float rotation = 0;
    private float rotationSpeed;
    private boolean isFlying = true;
    private boolean hasLanded = false;
    private long landTime = 0;
    private float bounceDistance;
    private Vec2 bounceDirection;
    private float bounceProgress = 0;
    private Vec2 landPosition;
    private List<GameObject> collidableObjects;
    
    public Grenade(float startX, float startY, float targetX, float targetY, List<GameObject> collidables) {
        super(startX, startY);
        this.startPosition = new Vec2(startX, startY);
        this.collidableObjects = collidables;
        
        // Calculate target position with range limits
        Vec2 direction = new Vec2(targetX - startX, targetY - startY);
        float distance = direction.distance(new Vec2(0, 0));
        
        if (distance < GameConfig.GRENADE_MIN_RANGE) {
            direction = direction.multiply(GameConfig.GRENADE_MIN_RANGE / distance);
        } else if (distance > GameConfig.GRENADE_MAX_RANGE) {
            direction = direction.multiply(GameConfig.GRENADE_MAX_RANGE / distance);
        }
        
        this.targetPosition = new Vec2(startX, startY).add(direction);
        this.totalFlightTime = 1.0f; // 1 second flight time
        this.flightTime = 0;
        
        // Calculate velocity for arc
        Vec2 displacement = targetPosition.subtract(position);
        this.velocity = displacement.multiply(1.0f / totalFlightTime);
        
        // Set rotation speed based on horizontal direction (much slower)
        this.rotationSpeed = displacement.x() > 0 ? -0.5f : 0.5f; // Counter-clockwise if moving right
        
        // Calculate bounce
        this.bounceDistance = 20 + (float)(Math.random() * 20); // 20-40 pixels (2x longer)
        this.bounceDirection = displacement.multiply(1.0f / displacement.distance(new Vec2(0, 0)));
    }
    
    @Override
    public void update(float deltaTime) {
        if (isFlying) {
            flightTime += deltaTime;
            
            if (flightTime >= totalFlightTime) {
                // Land
                position = targetPosition;
                landPosition = position;
                isFlying = false;
                hasLanded = true;
                landTime = System.currentTimeMillis();
                bounceProgress = 0;
            } else {
                // Update position along arc with parabolic trajectory
                float progress = flightTime / totalFlightTime;
                
                // Linear interpolation to target
                Vec2 linearPos = startPosition.add(targetPosition.subtract(startPosition).multiply(progress));
                
                // Add parabolic arc (goes up then down)
                float arcHeight = 50.0f; // Increased arc height
                float yOffset = 4 * arcHeight * progress * (1 - progress); // Parabolic curve
                
                position = new Vec2(linearPos.x(), linearPos.y() + yOffset);
                
                // Update rotation
                rotation += rotationSpeed * deltaTime * 60; // 60 for frame rate normalization
            }
        } else if (hasLanded && bounceProgress < 1.0f) {
            // Bounce phase - roll forward in direction of travel
            bounceProgress += deltaTime * 2.0f; // 0.5 second bounce
            if (bounceProgress > 1.0f) bounceProgress = 1.0f;
            
            Vec2 bounceOffset = bounceDirection.multiply(bounceDistance * bounceProgress);
            Vec2 newPos = landPosition.add(bounceOffset);
            
            // Check collision during bounce
            if (wouldCollideWithObjects(newPos)) {
                bounceProgress = 1.0f; // Stop bouncing
            } else {
                position = newPos;
            }
        }
        
        // Check for explosion
        if (hasLanded && System.currentTimeMillis() - landTime >= GameConfig.GRENADE_FUSE_TIME) {
            active = false; // Will be handled by GameLoop
        }
    }
    
    private boolean wouldCollideWithObjects(Vec2 newPos) {
        if (collidableObjects == null) return false;
        
        float grenadeSize = 16;
        float grenadeLeft = newPos.x() - grenadeSize/2;
        float grenadeBottom = newPos.y() - grenadeSize/2;
        
        for (GameObject obj : collidableObjects) {
            if (obj instanceof Collidable && ((Collidable) obj).checkMovementCollision(grenadeLeft, grenadeBottom, grenadeSize, grenadeSize)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public float getRenderY() {
        return position.y();
    }
    
    @Override
    public void render() {
        // Rendering handled by GameRenderer
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        return false; // Grenades don't collide with bullets
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        return false; // Grenades don't block movement
    }
    
    public float getScale() {
        if (isFlying) {
            // Scale up and down during flight for 3D effect
            float progress = flightTime / totalFlightTime;
            return 0.8f + 0.4f * (float)Math.sin(progress * Math.PI); // 0.8 to 1.2 scale
        }
        return 1.0f;
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public boolean isFlying() {
        return isFlying;
    }
    
    public boolean shouldExplode() {
        return hasLanded && System.currentTimeMillis() - landTime >= GameConfig.GRENADE_FUSE_TIME;
    }
}
