package com.dodgingbullets.gameobjects.enemies;

import com.dodgingbullets.gameobjects.EnemyObject;
import com.dodgingbullets.gameobjects.Shooter;

public class GunTurret extends EnemyObject implements Shooter {
    private float direction;
    private float lastShotTime;
    private float shotCooldown = 0.5f;
    private float sightRange = 400f;
    private float viewAngle = 45f;
    private boolean isIdle = true;
    private float lastScanTime;
    private float scanInterval = 2.0f;
    
    public GunTurret(float x, float y) {
        super(x, y, 100);
        this.direction = 0f;
    }
    
    @Override
    public void update(float deltaTime) {
        if (!active) return;
        
        lastShotTime += deltaTime;
        lastScanTime += deltaTime;
        
        // AI logic would go here
        // For now, just basic idle scanning
        if (isIdle && lastScanTime >= scanInterval) {
            direction += 45f;
            if (direction >= 360f) direction = 0f;
            lastScanTime = 0f;
        }
    }
    
    @Override
    public void render() {
        // Rendering logic would go here
        // This would call the renderer with turret sprite and direction
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        // 64x64 sprite hitbox
        return x < this.x + 64 && x + width > this.x && 
               y < this.y + 64 && y + height > this.y;
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        // 64x32 movement hitbox (lower half)
        return x < this.x + 64 && x + width > this.x && 
               y < this.y + 64 && y + height > this.y + 32;
    }
    
    @Override
    public void shoot(float targetX, float targetY) {
        if (!canShoot()) return;
        
        // Calculate angle and create bullet
        float angle = (float) Math.atan2(targetY - y, targetX - x);
        // Bullet creation logic would go here
        
        lastShotTime = 0f;
    }
    
    @Override
    public boolean canShoot() {
        return lastShotTime >= shotCooldown;
    }
    
    public float getDirection() { return direction; }
    public void setDirection(float direction) { this.direction = direction; }
    public boolean isIdle() { return isIdle; }
    public void setIdle(boolean idle) { this.isIdle = idle; }
}
