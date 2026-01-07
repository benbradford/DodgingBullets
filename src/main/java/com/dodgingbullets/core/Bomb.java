package com.dodgingbullets.core;

public class Bomb {
    private Vec2 position;
    private Vec2 targetPosition;
    private Vec2 velocity;
    private float height;
    private float verticalVelocity;
    private boolean active = true;
    private boolean hasExploded = false;
    
    private static final float GRAVITY = 300f;
    private static final float INITIAL_HEIGHT = 20f;
    private static final float FLIGHT_SPEED = 200f;
    
    public Bomb(float startX, float startY, float targetX, float targetY) {
        this.position = new Vec2(startX, startY);
        this.targetPosition = new Vec2(targetX, targetY);
        this.height = INITIAL_HEIGHT;
        
        // Calculate horizontal velocity
        Vec2 direction = targetPosition.subtract(position);
        float distance = direction.distance(new Vec2(0, 0));
        float flightTime = distance / FLIGHT_SPEED;
        
        this.velocity = direction.multiply(1.0f / distance).multiply(FLIGHT_SPEED);
        
        // Calculate initial vertical velocity to reach target
        this.verticalVelocity = (GRAVITY * flightTime) / 2;
    }
    
    public void update(float deltaTime) {
        if (!active || hasExploded) return;
        
        // Update horizontal position
        position = position.add(velocity.multiply(deltaTime));
        
        // Update vertical position
        verticalVelocity -= GRAVITY * deltaTime;
        height += verticalVelocity * deltaTime;
        
        // Check if bomb has landed
        if (height <= 0) {
            height = 0;
            hasExploded = true;
        }
    }
    
    public Vec2 getPosition() {
        return position;
    }
    
    public Vec2 getShadowPosition() {
        return position;
    }
    
    public float getHeight() {
        return height;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public boolean hasExploded() {
        return hasExploded;
    }
    
    public void setInactive() {
        active = false;
    }
}
