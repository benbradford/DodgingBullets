package com.dodgingbullets.core;

public class Turret {
    private float x, y;
    private Direction facingDirection;
    private long lastShotTime;
    private static final long SHOT_INTERVAL = 500; // 0.5 seconds between shots (4x faster)
    private static final float MAX_SIGHT = 320; // Maximum sight range
    private boolean isIdle = true;
    private long lastDirectionChange = 0;
    private static final long IDLE_ROTATION_INTERVAL = 2000; // 2 seconds per direction
    private int health = 100;
    private static final int MAX_HEALTH = 100;
    private static final int BULLET_DAMAGE = 10;
    private boolean isDestroyed = false;
    
    public Turret(float x, float y) {
        this.x = x;
        this.y = y;
        this.facingDirection = Direction.UP;
        this.lastShotTime = System.currentTimeMillis();
        this.lastDirectionChange = System.currentTimeMillis();
    }
    
    public void update(float playerX, float playerY) {
        boolean playerInRange = canSeePlayer(playerX, playerY);
        
        if (playerInRange && (!isIdle || canSeePlayerInCurrentDirection(playerX, playerY))) {
            // Player in range AND (not idle OR facing player direction) - track player
            if (isIdle) {
                isIdle = false; // Exit idle state
            }
            
            // Calculate direction to player
            float deltaX = playerX - x;
            float deltaY = playerY - y;
            
            // Determine facing direction based on angle to player
            double angle = Math.atan2(deltaY, deltaX);
            double degrees = Math.toDegrees(angle);
            
            // Normalize to 0-360
            if (degrees < 0) degrees += 360;
            
            // Convert to 8-directional facing
            if (degrees >= 337.5 || degrees < 22.5) {
                facingDirection = Direction.RIGHT; // East
            } else if (degrees >= 22.5 && degrees < 67.5) {
                facingDirection = Direction.UP_RIGHT; // Northeast
            } else if (degrees >= 67.5 && degrees < 112.5) {
                facingDirection = Direction.UP; // North
            } else if (degrees >= 112.5 && degrees < 157.5) {
                facingDirection = Direction.UP_LEFT; // Northwest
            } else if (degrees >= 157.5 && degrees < 202.5) {
                facingDirection = Direction.LEFT; // West
            } else if (degrees >= 202.5 && degrees < 247.5) {
                facingDirection = Direction.DOWN_LEFT; // Southwest
            } else if (degrees >= 247.5 && degrees < 292.5) {
                facingDirection = Direction.DOWN; // South
            } else {
                facingDirection = Direction.DOWN_RIGHT; // Southeast
            }
        } else {
            // Player out of range OR not facing player - enter/continue idle scanning
            if (!isIdle) {
                isIdle = true;
                lastDirectionChange = System.currentTimeMillis(); // Reset timer when entering idle
            }
            
            // Idle scanning - rotate clockwise every 2 seconds
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDirectionChange >= IDLE_ROTATION_INTERVAL) {
                facingDirection = getNextClockwiseDirection(facingDirection);
                lastDirectionChange = currentTime;
            }
        }
    }
    
    public boolean canSeePlayerInCurrentDirection(float playerX, float playerY) {
        // Calculate angle to player
        float deltaX = playerX - x;
        float deltaY = playerY - y;
        double angle = Math.atan2(deltaY, deltaX);
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) degrees += 360;
        
        // Check if player is in the current facing direction (45-degree cone)
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
    
    public boolean shouldShoot() {
        if (isDestroyed) return false; // Destroyed turrets can't shoot
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= SHOT_INTERVAL) {
            lastShotTime = currentTime;
            return true;
        }
        return false;
    }
    
    public boolean canSeePlayer(float playerX, float playerY) {
        float deltaX = playerX - x;
        float deltaY = playerY - y;
        float distance = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        return distance <= MAX_SIGHT;
    }
    
    public boolean takeDamage() {
        if (isDestroyed) return false; // Already destroyed
        
        // Being shot alerts the turret - exit idle state immediately
        isIdle = false;
        
        health -= BULLET_DAMAGE;
        if (health <= 0) {
            isDestroyed = true;
            return true; // Turret was destroyed
        }
        return false; // Turret still alive
    }
    
    public boolean isInSpriteHitbox(float bulletX, float bulletY) {
        // Check if bullet is within turret's sprite hitbox (64x64 pixels, 4x4 cells)
        return bulletX >= x - 32 && bulletX <= x + 32 && 
               bulletY >= y - 32 && bulletY <= y + 32;
    }
    
    public float[] getBarrelPosition() {
        // Gun barrel positions for each direction (adjusted for actual sprite barrel locations)
        float barrelX = x, barrelY = y;
        switch (facingDirection) {
            case UP: barrelX += 0; barrelY += 32; break;           // North
            case DOWN: barrelX += 0; barrelY -= 32; break;         // South
            case LEFT: barrelX -= 45; barrelY += 7; break;         // West
            case RIGHT: barrelX += 45; barrelY += 8; break;        // East
            case UP_LEFT: barrelX -= 30; barrelY += 28; break;     // Northwest
            case UP_RIGHT: barrelX += 30; barrelY += 28; break;    // Northeast
            case DOWN_LEFT: barrelX -= 42; barrelY -= 22; break;   // Southwest
            case DOWN_RIGHT: barrelX += 42; barrelY -= 22; break;  // Southeast
        }
        return new float[]{barrelX, barrelY};
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public Direction getFacingDirection() { return facingDirection; }
    public boolean isDestroyed() { return isDestroyed; }
    public int getHealth() { return health; }
}
