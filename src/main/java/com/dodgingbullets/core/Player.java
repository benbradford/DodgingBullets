package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.GameObject;
import com.dodgingbullets.gameobjects.Collidable;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Player {
    private Vec2 position;
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
    
    // Special bullets system
    private int specialBullets = 0;
    private static final int MAX_SPECIAL_BULLETS = 100;
    
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
        this.position = new Vec2(x, y);
    }
    
    public void setTurret(GameObject turret) {
        this.turret = turret;
    }
    
    public void setCollidableObjects(List<GameObject> objects) {
        this.collidableObjects = objects;
    }
    
    private boolean wouldCollideWithObjects(Vec2 newPos) {
        if (collidableObjects == null) return false;
        
        // Player movement hitbox (12 pixels wide, bottom 1/5th of sprite height)
        float playerWidth = 12;
        float playerHeight = 12.8f; // Bottom 1/5th of 64-pixel sprite
        float playerLeft = newPos.x() - 6;
        float playerBottom = newPos.y() - 32;
        
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
        Vec2 prevPos = position;
        
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
        Vec2 movement = new Vec2(0, 0);
        
        if (keys[0]) { // W (up)
            if (keys[2]) { // A (left)
                currentDirection = Direction.UP_LEFT;
                movement = new Vec2(-2, 2);
                isMoving = true;
            } else if (keys[3]) { // D (right)
                currentDirection = Direction.UP_RIGHT;
                movement = new Vec2(2, 2);
                isMoving = true;
            } else {
                currentDirection = Direction.UP;
                movement = new Vec2(0, 2);
                isMoving = true;
            }
        } else if (keys[1]) { // S (down)
            if (keys[2]) { // A (left)
                currentDirection = Direction.DOWN_LEFT;
                movement = new Vec2(-2, -2);
                isMoving = true;
            } else if (keys[3]) { // D (right)
                currentDirection = Direction.DOWN_RIGHT;
                movement = new Vec2(2, -2);
                isMoving = true;
            } else {
                currentDirection = Direction.DOWN;
                movement = new Vec2(0, -2);
                isMoving = true;
            }
        } else if (keys[2]) { // A (left)
            currentDirection = Direction.LEFT;
            movement = new Vec2(-2, 0);
            isMoving = true;
        } else if (keys[3]) { // D (right)
            currentDirection = Direction.RIGHT;
            movement = new Vec2(2, 0);
            isMoving = true;
        }
        
        Vec2 newPos = position.add(movement);
        
        // Boundary checking - map is 4x screen size (2560x1440)
        Vec2 mapBounds = new Vec2(GameConfig.MAP_WIDTH - 32, GameConfig.MAP_HEIGHT - 32);
        Vec2 minBounds = new Vec2(32, 32);
        newPos = newPos.clamp(minBounds, mapBounds);
        
        // Check collision with objects - X axis first
        Vec2 xOnlyPos = new Vec2(newPos.x(), prevPos.y());
        if (wouldCollideWithObjects(xOnlyPos)) {
            newPos = new Vec2(prevPos.x(), newPos.y()); // Revert X movement
        }
        
        // Check collision with objects - Y axis
        Vec2 yOnlyPos = new Vec2(prevPos.x(), newPos.y());
        if (wouldCollideWithObjects(yOnlyPos)) {
            newPos = new Vec2(newPos.x(), prevPos.y()); // Revert Y movement
        }
        
        // Final check - if both movements together cause collision, revert both
        if (wouldCollideWithObjects(newPos)) {
            newPos = prevPos;
        }
        
        position = newPos;
        
        // Update movement state based on final position
        if (position.equals(prevPos)) {
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
        
        // Ammo regeneration (only for regular ammo)
        if (specialBullets == 0 && ammo < MAX_AMMO) {
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
    
    public float getX() { return position.x(); }
    public float getY() { return position.y(); }
    public Vec2 getPosition() { return position; }
    public float getJumpOffset() { return jumpOffset; }
    public Direction getCurrentDirection() { return currentDirection; }
    public int getHealth() { return health; }
    public int getAmmo() { 
        return specialBullets > 0 ? specialBullets : ammo; 
    }
    
    public boolean hasSpecialBullets() {
        return specialBullets > 0;
    }
    
    public void collectAmmoPowerUp() {
        specialBullets = MAX_SPECIAL_BULLETS;
        ammo = MAX_AMMO; // Restore regular ammo for when special runs out
    }
    
    public boolean canShoot() {
        return specialBullets > 0 || ammo > 0;
    }
    
    public void shoot() {
        if (specialBullets > 0) {
            specialBullets--;
        } else if (ammo > 0) {
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
        Vec2 gunPos = position;
        switch (displayDirection) {
            case UP: gunPos = gunPos.add(new Vec2(8, 20)); break;
            case DOWN: gunPos = gunPos.add(new Vec2(-8, -20)); break;
            case LEFT: gunPos = gunPos.add(new Vec2(-20, 2)); break;
            case RIGHT: gunPos = gunPos.add(new Vec2(20, 2)); break;
            case UP_LEFT: gunPos = gunPos.add(new Vec2(-12, 16)); break;
            case UP_RIGHT: gunPos = gunPos.add(new Vec2(12, 16)); break;
            case DOWN_LEFT: gunPos = gunPos.add(new Vec2(-12, -16)); break;
            case DOWN_RIGHT: gunPos = gunPos.add(new Vec2(12, -16)); break;
        }
        return new float[]{gunPos.x(), gunPos.y()};
    }
}
