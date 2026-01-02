package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.GameObject;
import com.dodgingbullets.gameobjects.Collidable;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

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
    
    // Health system
    private int health = 100;
    private static final int MAX_HEALTH = 100;
    private long lastDamageTime = 0;
    private static final long REGEN_DELAY = 3000; // 3 seconds
    private static final int REGEN_RATE = 15; // 15 health per second
    private long lastRegenTime = 0;
    private static final long DAMAGE_FLASH_DURATION = 500; // 0.5 seconds
    
    // Ammo system
    private int ammo = 10;
    private static final int MAX_AMMO = 10;
    private long lastAmmoRegenTime = 0;
    
    private Direction currentDirection = Direction.UP;
    private Direction shootingDirection = null;
    private long lastShotTime = 0;
    private static final long SHOOTING_OVERRIDE_DURATION = 300; // 0.3 seconds
    private GameObject turret; // Reference to check collision
    private List<GameObject> collidableObjects; // All collidable objects
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
    
    public void setTurret(GameObject turret) {
        this.turret = turret;
    }
    
    public void setCollidableObjects(List<GameObject> objects) {
        this.collidableObjects = objects;
    }
    
    private boolean wouldCollideWithObjects(float newX, float newY) {
        if (collidableObjects == null) return false;
        
        // Player movement hitbox (12 pixels wide, bottom 1/5th of sprite height)
        float playerWidth = 12;
        float playerHeight = 12.8f; // Bottom 1/5th of 64-pixel sprite
        float playerLeft = newX - 6;
        float playerBottom = newY - 32;
        
        for (GameObject obj : collidableObjects) {
            if (obj instanceof Collidable && ((Collidable) obj).checkMovementCollision(playerLeft, playerBottom, playerWidth, playerHeight)) {
                return true;
            }
        }
        return false;
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
        
        // Check collision with objects movement hitbox - X axis
        if (wouldCollideWithObjects(x, prevY)) {
            x = prevX; // Revert X movement only
        }
        
        // Check collision with objects movement hitbox - Y axis
        if (wouldCollideWithObjects(prevX, y)) {
            y = prevY; // Revert Y movement only
        }
        
        // Final check - if both movements together cause collision, revert both
        if (wouldCollideWithObjects(x, y)) {
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
        
        // Health regeneration
        long now = System.currentTimeMillis();
        if (health < MAX_HEALTH && now - lastDamageTime >= REGEN_DELAY) {
            if (now - lastRegenTime >= 1000) { // 1 second intervals
                health = Math.min(MAX_HEALTH, health + REGEN_RATE);
                lastRegenTime = now;
            }
        }
        
        // Ammo regeneration
        if (ammo < MAX_AMMO) {
            if (now - lastAmmoRegenTime >= 1000) { // 1 second intervals
                ammo = Math.min(MAX_AMMO, ammo + 1);
                lastAmmoRegenTime = now;
            }
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
    public int getHealth() { return health; }
    public int getAmmo() { return ammo; }
    
    public boolean canShoot() {
        return ammo > 0;
    }
    
    public void shoot() {
        if (ammo > 0) {
            ammo--;
            lastAmmoRegenTime = System.currentTimeMillis(); // Reset ammo regen timer
        }
    }
    
    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
        lastDamageTime = System.currentTimeMillis();
        lastRegenTime = System.currentTimeMillis(); // Reset regen timer
    }
    
    public float getDamageFlashIntensity() {
        long timeSinceDamage = System.currentTimeMillis() - lastDamageTime;
        if (timeSinceDamage < DAMAGE_FLASH_DURATION) {
            // Fade from 1.0 to 0.0 over the flash duration
            return 1.0f - (timeSinceDamage / (float)DAMAGE_FLASH_DURATION);
        }
        return 0.0f;
    }
    
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
