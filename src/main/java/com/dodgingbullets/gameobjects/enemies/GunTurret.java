package com.dodgingbullets.gameobjects.enemies;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.core.Direction;
import com.dodgingbullets.core.Vec2;

public class GunTurret extends EnemyObject implements Shooter, Trackable, Positionable {
    private Direction facingDirection;
    private long lastShotTime;
    private static final long SHOT_INTERVAL = 500;
    private static final float MAX_SIGHT = 320;
    private boolean isIdle = true;
    private long lastDirectionChange = 0;
    private static final long IDLE_ROTATION_INTERVAL = 2000;
    private static final long DAMAGE_FLASH_DURATION = 100;
    private static final long FLASH_INTERVAL = 50; // Flash every 50ms
    
    private long lastDamageTime = 0;
    
    public GunTurret(float x, float y) {
        super(x, y, 100);
        this.facingDirection = Direction.UP;
        this.lastShotTime = System.currentTimeMillis();
        this.lastDirectionChange = System.currentTimeMillis();
    }
    
    @Override
    public void update(float deltaTime) {
        if (!active) return;
        
        if (isIdle) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDirectionChange >= IDLE_ROTATION_INTERVAL) {
                facingDirection = getNextClockwiseDirection(facingDirection);
                lastDirectionChange = currentTime;
            }
        }
    }
    
    @Override
    public void update(float playerX, float playerY) {
        Vec2 playerPos = new Vec2(playerX, playerY);
        boolean playerInRange = canSeePlayer(playerX, playerY);
        
        if (playerInRange && (!isIdle || canSeePlayerInCurrentDirection(playerX, playerY))) {
            if (isIdle) {
                isIdle = false;
            }
            
            Vec2 delta = playerPos.subtract(position);
            double angle = delta.angle();
            double degrees = Math.toDegrees(angle);
            if (degrees < 0) degrees += 360;
            
            if (degrees >= 337.5 || degrees < 22.5) {
                facingDirection = Direction.RIGHT;
            } else if (degrees >= 22.5 && degrees < 67.5) {
                facingDirection = Direction.UP_RIGHT;
            } else if (degrees >= 67.5 && degrees < 112.5) {
                facingDirection = Direction.UP;
            } else if (degrees >= 112.5 && degrees < 157.5) {
                facingDirection = Direction.UP_LEFT;
            } else if (degrees >= 157.5 && degrees < 202.5) {
                facingDirection = Direction.LEFT;
            } else if (degrees >= 202.5 && degrees < 247.5) {
                facingDirection = Direction.DOWN_LEFT;
            } else if (degrees >= 247.5 && degrees < 292.5) {
                facingDirection = Direction.DOWN;
            } else {
                facingDirection = Direction.DOWN_RIGHT;
            }
        } else {
            if (!isIdle) {
                isIdle = true;
                lastDirectionChange = System.currentTimeMillis();
            }
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDirectionChange >= IDLE_ROTATION_INTERVAL) {
                facingDirection = getNextClockwiseDirection(facingDirection);
                lastDirectionChange = currentTime;
            }
        }
    }
    
    @Override
    public void render() {
        // Rendering handled by renderer
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        return x < position.x() + 32 && x + width > position.x() - 32 && 
               y < position.y() + 32 && y + height > position.y() - 32;
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        return x < position.x() + 32 && x + width > position.x() - 32 && 
               y < position.y() && y + height > position.y() - 32;
    }
    
    @Override
    public boolean canShoot() {
        if (isDestroyed()) return false;
        long currentTime = System.currentTimeMillis();
        return currentTime - lastShotTime >= SHOT_INTERVAL;
    }
    
    @Override
    public void shoot(float targetX, float targetY) {
        if (!canShoot()) return;
        lastShotTime = System.currentTimeMillis();
    }
    
    @Override
    public void takeDamage(int damage) {
        if (isDestroyed()) return;
        isIdle = false;
        lastDamageTime = System.currentTimeMillis();
        super.takeDamage(damage);
    }
    
    @Override
    public boolean canSeePlayer(float playerX, float playerY) {
        return position.distance(new Vec2(playerX, playerY)) <= MAX_SIGHT;
    }
    
    @Override
    public boolean canSeePlayerInCurrentDirection(float playerX, float playerY) {
        Vec2 playerPos = new Vec2(playerX, playerY);
        Vec2 delta = playerPos.subtract(position);
        double angle = delta.angle();
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) degrees += 360;
        
        switch (facingDirection) {
            case RIGHT: return degrees >= 337.5 || degrees < 22.5;
            case UP_RIGHT: return degrees >= 22.5 && degrees < 67.5;
            case UP: return degrees >= 67.5 && degrees < 112.5;
            case UP_LEFT: return degrees >= 112.5 && degrees < 157.5;
            case LEFT: return degrees >= 157.5 && degrees < 202.5;
            case DOWN_LEFT: return degrees >= 202.5 && degrees < 247.5;
            case DOWN: return degrees >= 247.5 && degrees < 292.5;
            case DOWN_RIGHT: return degrees >= 292.5 && degrees < 337.5;
            default: return false;
        }
    }
    
    @Override
    public Direction getFacingDirection() { 
        return facingDirection; 
    }
    
    @Override
    public boolean isInSpriteHitbox(float bulletX, float bulletY) {
        Vec2 bulletPos = new Vec2(bulletX, bulletY);
        Vec2 halfSize = new Vec2(32, 32);
        Vec2 min = position.subtract(halfSize);
        Vec2 max = position.add(halfSize);
        return bulletPos.x() >= min.x() && bulletPos.x() <= max.x() && 
               bulletPos.y() >= min.y() && bulletPos.y() <= max.y();
    }
    
    @Override
    public float[] getBarrelPosition() {
        Vec2 barrelOffset = new Vec2(0, 0);
        switch (facingDirection) {
            case UP: barrelOffset = new Vec2(0, 32); break;
            case DOWN: barrelOffset = new Vec2(0, -32); break;
            case LEFT: barrelOffset = new Vec2(-45, 7); break;
            case RIGHT: barrelOffset = new Vec2(45, 8); break;
            case UP_LEFT: barrelOffset = new Vec2(-30, 28); break;
            case UP_RIGHT: barrelOffset = new Vec2(30, 28); break;
            case DOWN_LEFT: barrelOffset = new Vec2(-42, -22); break;
            case DOWN_RIGHT: barrelOffset = new Vec2(42, -22); break;
        }
        Vec2 barrelPos = position.add(barrelOffset);
        return new float[]{barrelPos.x(), barrelPos.y()};
    }
    
    private Direction getNextClockwiseDirection(Direction current) {
        switch (current) {
            case UP: return Direction.UP_RIGHT;
            case UP_RIGHT: return Direction.RIGHT;
            case RIGHT: return Direction.DOWN_RIGHT;
            case DOWN_RIGHT: return Direction.DOWN;
            case DOWN: return Direction.DOWN_LEFT;
            case DOWN_LEFT: return Direction.LEFT;
            case LEFT: return Direction.UP_LEFT;
            case UP_LEFT: return Direction.UP;
            default: return Direction.UP;
        }
    }
    
    public boolean isDamageFlashing() {
        long timeSinceDamage = System.currentTimeMillis() - lastDamageTime;
        if (timeSinceDamage >= DAMAGE_FLASH_DURATION) return false;
        
        // Alternate between normal and white every FLASH_INTERVAL ms
        return (timeSinceDamage / FLASH_INTERVAL) % 2 == 1;
    }
}
