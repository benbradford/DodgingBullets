package com.dodgingbullets.core;

public class Turret {
    private float x, y;
    private Direction facingDirection;
    private long lastShotTime;
    private static final long SHOT_INTERVAL = 2000; // 2 seconds between shots
    
    public Turret(float x, float y) {
        this.x = x;
        this.y = y;
        this.facingDirection = Direction.UP;
        this.lastShotTime = System.currentTimeMillis();
    }
    
    public void update(float playerX, float playerY) {
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
    }
    
    public boolean shouldShoot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= SHOT_INTERVAL) {
            lastShotTime = currentTime;
            return true;
        }
        return false;
    }
    
    public float[] getBarrelPosition() {
        // Gun barrel positions for each direction (adjusted for 64x64 turret)
        float barrelX = x, barrelY = y;
        switch (facingDirection) {
            case UP: barrelX += 0; barrelY += 32; break;
            case DOWN: barrelX += 0; barrelY -= 32; break;
            case LEFT: barrelX -= 32; barrelY += 0; break;
            case RIGHT: barrelX += 32; barrelY += 0; break;
            case UP_LEFT: barrelX -= 23; barrelY += 23; break;
            case UP_RIGHT: barrelX += 23; barrelY += 23; break;
            case DOWN_LEFT: barrelX -= 23; barrelY -= 23; break;
            case DOWN_RIGHT: barrelX += 23; barrelY -= 23; break;
        }
        return new float[]{barrelX, barrelY};
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public Direction getFacingDirection() { return facingDirection; }
}
