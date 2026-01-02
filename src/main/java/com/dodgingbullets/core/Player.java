package com.dodgingbullets.core;

import java.util.HashMap;
import java.util.Map;

public class Player {
    private float x, y;
    private float jumpOffset = 0;
    private float jumpVelocity = 0;
    private boolean isJumping = false;
    private boolean spaceHeld = false;
    private long jumpStartTime = 0;
    private static final float MIN_JUMP_STRENGTH = 6.0f;
    private static final float MAX_JUMP_STRENGTH = 21.0f;
    private static final long MAX_CHARGE_TIME = 500; // milliseconds
    private static final float GRAVITY = 0.3f;
    
    private Direction currentDirection = Direction.UP;
    private Direction shootingDirection = null;
    private long lastShotTime = 0;
    private static final long SHOOTING_OVERRIDE_DURATION = 300; // 0.3 seconds
    private Turret turret; // Reference to check collision
    private boolean isMoving = false;
    private int animationFrame = 0;
    private boolean animationForward = true;
    private long lastAnimationTime = 0;
    private static final long ANIMATION_DELAY = 150; // milliseconds
    
    private Map<String, Texture> textures = new HashMap<>();
    
    public Player(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void setTurret(Turret turret) {
        this.turret = turret;
    }
    
    private boolean wouldCollideWithTurret(float newX, float newY) {
        if (turret == null) return false;
        
        // Player movement hitbox at new position
        int playerLeft = (int)((newX - 6) / 16);   // 12 pixel width
        int playerRight = (int)((newX + 6) / 16);
        int playerBottom = (int)((newY - 32) / 16); // Bottom 1/5th
        int playerTop = (int)((newY - 32 + 12.8f) / 16);
        
        // Turret movement hitbox (lower half)
        int turretLeft = (int)((turret.getX() - 32) / 16);
        int turretRight = (int)((turret.getX() + 32) / 16);
        int turretBottom = (int)((turret.getY() - 32) / 16);
        int turretTop = (int)(turret.getY() / 16);
        
        // Check overlap
        return !(playerRight < turretLeft || playerLeft > turretRight || 
                 playerTop < turretBottom || playerBottom > turretTop);
    }
    
    public void loadTextures(Renderer renderer) {
        for (Direction dir : Direction.values()) {
            String prefix = "mc" + dir.getPrefix();
            textures.put(prefix + "idle", renderer.loadTexture("assets/" + prefix + "idle.png"));
            textures.put(prefix + "01", renderer.loadTexture("assets/" + prefix + "01.png"));
            textures.put(prefix + "02", renderer.loadTexture("assets/" + prefix + "02.png"));
            textures.put(prefix + "03", renderer.loadTexture("assets/" + prefix + "03.png"));
        }
    }
    
    public void update(boolean[] keys, boolean jumpPressed, boolean jumpHeld) {
        // Store previous position for boundary checking
        float prevX = x, prevY = y;
        
        // Handle jump start immediately on press
        if (jumpPressed && !isJumping) {
            isJumping = true;
            jumpVelocity = MIN_JUMP_STRENGTH; // Start with minimum jump
            jumpStartTime = System.currentTimeMillis();
            this.spaceHeld = jumpHeld;
        }
        
        // Boost jump if J is still held during ascent
        if (isJumping && jumpHeld && jumpVelocity > 0) {
            long chargeTime = System.currentTimeMillis() - jumpStartTime;
            if (chargeTime <= MAX_CHARGE_TIME) {
                float chargeRatio = chargeTime / (float)MAX_CHARGE_TIME;
                float boostStrength = (MAX_JUMP_STRENGTH - MIN_JUMP_STRENGTH) * chargeRatio * 0.015f;
                jumpVelocity += boostStrength;
            }
        }
        
        if (!jumpHeld) {
            this.spaceHeld = false;
        }
        
        // Update jump physics
        if (isJumping) {
            jumpOffset += jumpVelocity;
            jumpVelocity -= GRAVITY;
            
            // Early jump break if J released during jump
            if (!jumpHeld && jumpVelocity > 0) {
                jumpVelocity *= 0.7f; // Reduce jump velocity
            }
            
            if (jumpOffset <= 0) {
                jumpOffset = 0;
                jumpVelocity = 0;
                isJumping = false;
            }
        }
        
        boolean wasMoving = isMoving;
        isMoving = false;
        
        if (keys[0]) { // W (up)
            if (keys[2]) { // A (left)
                currentDirection = Direction.UP_LEFT;
                y += 2;
                x -= 2;
                isMoving = true;
            } else if (keys[3]) { // D (right)
                currentDirection = Direction.UP_RIGHT;
                y += 2;
                x += 2;
                isMoving = true;
            } else {
                currentDirection = Direction.UP;
                y += 2;
                isMoving = true;
            }
        } else if (keys[1]) { // S (down)
            if (keys[2]) { // A (left)
                currentDirection = Direction.DOWN_LEFT;
                y -= 2;
                x -= 2;
                isMoving = true;
            } else if (keys[3]) { // D (right)
                currentDirection = Direction.DOWN_RIGHT;
                y -= 2;
                x += 2;
                isMoving = true;
            } else {
                currentDirection = Direction.DOWN;
                y -= 2;
                isMoving = true;
            }
        } else if (keys[2]) { // A (left)
            currentDirection = Direction.LEFT;
            x -= 2;
            isMoving = true;
        } else if (keys[3]) { // D (right)
            currentDirection = Direction.RIGHT;
            x += 2;
            isMoving = true;
        }
        
        // Boundary checking - map is 4x screen size (2560x1440)
        float mapWidth = 2560;
        float mapHeight = 1440;
        float margin = 32; // Half player width for proper collision
        
        // Check X boundary separately
        if (x < margin || x > mapWidth - margin) {
            x = prevX; // Revert X movement only
        }
        
        // Check Y boundary separately  
        if (y < margin || y > mapHeight - margin) {
            y = prevY; // Revert Y movement only
        }
        
        // Check collision with turret movement hitbox - X axis
        if (wouldCollideWithTurret(x, prevY)) {
            x = prevX; // Revert X movement only
        }
        
        // Check collision with turret movement hitbox - Y axis
        if (wouldCollideWithTurret(prevX, y)) {
            y = prevY; // Revert Y movement only
        }
        
        // Final check - if both movements together cause collision, revert both
        if (wouldCollideWithTurret(x, y)) {
            x = prevX;
            y = prevY;
        }
        
        // Update movement state based on final position
        if (x == prevX && y == prevY) {
            isMoving = false; // No movement occurred
        }
        
        // Clear shooting direction if player starts moving in a different direction after override period
        if (isMoving && shootingDirection != null && 
            System.currentTimeMillis() - lastShotTime >= SHOOTING_OVERRIDE_DURATION &&
            currentDirection != shootingDirection) {
            shootingDirection = null;
        }
        
        if (!wasMoving && isMoving) {
            animationFrame = 0;
            animationForward = true;
            lastAnimationTime = System.currentTimeMillis();
        }
        
        if (isMoving) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAnimationTime > ANIMATION_DELAY) {
                if (animationForward) {
                    animationFrame++;
                    if (animationFrame >= 3) {
                        animationFrame = 2;
                        animationForward = false;
                    }
                } else {
                    animationFrame--;
                    if (animationFrame < 0) {
                        animationFrame = 1;
                        animationForward = true;
                    }
                }
                lastAnimationTime = currentTime;
            }
        }
    }
    
    public Texture getCurrentTexture() {
        // Use shooting direction if within override time, or if not moving and have shot before
        Direction displayDirection = currentDirection;
        boolean withinOverrideTime = shootingDirection != null && System.currentTimeMillis() - lastShotTime < SHOOTING_OVERRIDE_DURATION;
        boolean stationaryAfterShooting = shootingDirection != null && !isMoving;
        
        if (withinOverrideTime || stationaryAfterShooting) {
            displayDirection = shootingDirection;
        }
        
        String prefix = "mc" + displayDirection.getPrefix();
        if (isMoving) {
            return textures.get(prefix + String.format("%02d", animationFrame + 1));
        } else {
            return textures.get(prefix + "idle");
        }
    }
    
    public void setShootingDirection(Direction direction) {
        this.shootingDirection = direction;
        this.lastShotTime = System.currentTimeMillis();
    }
    
    public static Direction calculateDirectionFromAngle(double angle) {
        // Convert angle to degrees and normalize to 0-360
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) degrees += 360;
        
        // Convert to 8-directional
        if (degrees >= 337.5 || degrees < 22.5) {
            return Direction.RIGHT;
        } else if (degrees >= 22.5 && degrees < 67.5) {
            return Direction.UP_RIGHT;
        } else if (degrees >= 67.5 && degrees < 112.5) {
            return Direction.UP;
        } else if (degrees >= 112.5 && degrees < 157.5) {
            return Direction.UP_LEFT;
        } else if (degrees >= 157.5 && degrees < 202.5) {
            return Direction.LEFT;
        } else if (degrees >= 202.5 && degrees < 247.5) {
            return Direction.DOWN_LEFT;
        } else if (degrees >= 247.5 && degrees < 292.5) {
            return Direction.DOWN;
        } else {
            return Direction.DOWN_RIGHT;
        }
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public float getJumpOffset() { return jumpOffset; }
    public Direction getCurrentDirection() { return currentDirection; }
    
    public float[] getGunBarrelPosition() {
        // Use shooting direction if within override time, otherwise use current direction
        Direction displayDirection = currentDirection;
        boolean withinOverrideTime = shootingDirection != null && System.currentTimeMillis() - lastShotTime < SHOOTING_OVERRIDE_DURATION;
        boolean stationaryAfterShooting = shootingDirection != null && !isMoving;
        
        if (withinOverrideTime || stationaryAfterShooting) {
            displayDirection = shootingDirection;
        }
        
        // Approximate gun barrel positions based on direction
        float gunX = x, gunY = y;
        switch (displayDirection) {
            case UP: gunX += 8; gunY += 20; break;
            case DOWN: gunX -= 8; gunY -= 20; break;
            case LEFT: gunX -= 20; gunY += 2; break;  // Raised from -5 to +2
            case RIGHT: gunX += 20; gunY += 2; break; // Raised from -5 to +2
            case UP_LEFT: gunX -= 12; gunY += 16; break;
            case UP_RIGHT: gunX += 12; gunY += 16; break;
            case DOWN_LEFT: gunX -= 12; gunY -= 16; break;
            case DOWN_RIGHT: gunX += 12; gunY -= 16; break;
        }
        return new float[]{gunX, gunY};
    }
}
