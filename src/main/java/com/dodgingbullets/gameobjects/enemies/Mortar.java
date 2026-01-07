package com.dodgingbullets.gameobjects.enemies;

import com.dodgingbullets.core.Direction;
import com.dodgingbullets.core.Vec2;
import com.dodgingbullets.gameobjects.EnemyObject;
import com.dodgingbullets.gameobjects.GameObject;
import com.dodgingbullets.gameobjects.Renderable;
import com.dodgingbullets.gameobjects.Collidable;
import com.dodgingbullets.gameobjects.Damageable;
import com.dodgingbullets.gameobjects.Trackable;
import com.dodgingbullets.gameobjects.Shooter;
import com.dodgingbullets.gameobjects.Positionable;

import java.util.List;

public class Mortar extends EnemyObject implements Renderable, Collidable, Damageable, Trackable, Shooter, Positionable {
    
    public enum MortarState {
        PATROL,
        ENGAGED,
        FIRING
    }
    
    private MortarState state = MortarState.PATROL;
    private Direction lookDirection;
    private float lookDistance;
    private float firingSpeed;
    
    private Vec2 playerPosition = new Vec2(0, 0);
    private List<GameObject> collidableObjects;
    
    private float stateTimer = 0f;
    private int currentFrame = 0;
    private float frameTimer = 0f;
    private static final float FRAME_DURATION = 0.1f;
    private static final int FIRING_FRAMES = 9;
    private boolean hasFired = false;
    
    private float damageFlashTimer = 0f;
    private static final float DAMAGE_FLASH_DURATION = 0.3f;
    
    public Mortar(float x, float y, Direction lookDirection, float lookDistance, int health, float firingSpeed) {
        super(x, y, health);
        this.lookDirection = lookDirection;
        this.lookDistance = lookDistance;
        this.firingSpeed = firingSpeed;
    }
    
    @Override
    public void update(float deltaTime) {
        if (damageFlashTimer > 0) {
            damageFlashTimer -= deltaTime;
        }
        
        switch (state) {
            case PATROL:
                if (canSeePlayer(playerPosition.x(), playerPosition.y())) {
                    state = MortarState.ENGAGED;
                    stateTimer = 0f;
                    updateLookDirection();
                }
                break;
                
            case ENGAGED:
                updateLookDirection();
                stateTimer += deltaTime;
                if (stateTimer >= firingSpeed) {
                    state = MortarState.FIRING;
                    stateTimer = 0f;
                    currentFrame = 0;
                    frameTimer = 0f;
                    hasFired = false;
                }
                break;
                
            case FIRING:
                frameTimer += deltaTime;
                if (frameTimer >= FRAME_DURATION) {
                    frameTimer = 0f;
                    currentFrame++;
                    if (currentFrame >= FIRING_FRAMES) {
                        state = MortarState.ENGAGED;
                        stateTimer = 0f;
                    }
                }
                break;
        }
    }
    
    private void updateLookDirection() {
        if (playerPosition == null) return;
        
        Vec2 toPlayer = playerPosition.subtract(position);
        double angle = Math.atan2(toPlayer.y(), toPlayer.x());
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) degrees += 360;
        
        if (degrees >= 337.5 || degrees < 22.5) lookDirection = Direction.RIGHT;
        else if (degrees >= 22.5 && degrees < 67.5) lookDirection = Direction.UP_RIGHT;
        else if (degrees >= 67.5 && degrees < 112.5) lookDirection = Direction.UP;
        else if (degrees >= 112.5 && degrees < 157.5) lookDirection = Direction.UP_LEFT;
        else if (degrees >= 157.5 && degrees < 202.5) lookDirection = Direction.LEFT;
        else if (degrees >= 202.5 && degrees < 247.5) lookDirection = Direction.DOWN_LEFT;
        else if (degrees >= 247.5 && degrees < 292.5) lookDirection = Direction.DOWN;
        else lookDirection = Direction.DOWN_RIGHT;
    }
    
    @Override
    public boolean canSeePlayer(float playerX, float playerY) {
        Vec2 playerPos = new Vec2(playerX, playerY);
        float distance = position.distance(playerPos);
        
        if (distance > lookDistance) return false;
        
        return hasLineOfSight(playerPos);
    }
    
    @Override
    public boolean canSeePlayerInCurrentDirection(float playerX, float playerY) {
        return canSeePlayer(playerX, playerY);
    }
    
    @Override
    public Direction getFacingDirection() {
        return lookDirection;
    }
    
    @Override
    public void update(float playerX, float playerY) {
        // This is called by GameLoop for Trackable interface
        // Our main update logic is in update(float deltaTime)
    }
    
    private boolean hasLineOfSight(Vec2 playerPos) {
        if (collidableObjects == null) return true;
        
        Vec2 direction = playerPos.subtract(position);
        float distance = direction.distance(new Vec2(0, 0));
        
        if (distance == 0) return true;
        
        Vec2 normalizedDirection = direction.multiply(1.0f / distance);
        float stepSize = 8.0f;
        int steps = (int) (distance / stepSize);
        
        for (int i = 1; i < steps; i++) {
            Vec2 rayPoint = position.add(normalizedDirection.multiply(i * stepSize));
            
            for (GameObject obj : collidableObjects) {
                if (obj != this && obj instanceof Collidable && 
                    ((Collidable) obj).checkSpriteCollision(rayPoint.x() - 1, rayPoint.y() - 1, 2, 2)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public boolean canShoot() {
        return state == MortarState.FIRING && currentFrame == 4 && !hasFired;
    }
    
    @Override
    public void shoot(float targetX, float targetY) {
        hasFired = true;
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        if (!active) return false;
        Vec2 bulletPos = new Vec2(x, y);
        Vec2 halfSize = new Vec2(32, 32);
        Vec2 min = position.subtract(halfSize);
        Vec2 max = position.add(halfSize);
        return bulletPos.x() >= min.x() && bulletPos.x() <= max.x() && 
               bulletPos.y() >= min.y() && bulletPos.y() <= max.y();
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        if (!active) return false;
        Vec2 testPos = new Vec2(x + width/2, y + height/2);
        Vec2 halfSize = new Vec2(32, 16);
        Vec2 min = position.subtract(halfSize);
        Vec2 max = position.add(halfSize);
        return testPos.x() >= min.x() && testPos.x() <= max.x() && 
               testPos.y() >= min.y() && testPos.y() <= max.y();
    }
    
    @Override
    public void takeDamage(int damage) {
        health -= damage;
        damageFlashTimer = DAMAGE_FLASH_DURATION;
        if (health <= 0) {
            active = false;
        }
    }
    
    @Override
    public boolean isDestroyed() {
        return health <= 0;
    }
    
    @Override
    public float getRenderY() {
        return position.y();
    }
    
    public void render() {
        // Rendering handled by GameRenderer
    }
    
    public void setPlayerPosition(Vec2 playerPosition) {
        this.playerPosition = playerPosition;
    }
    
    public void setCollidableObjects(List<GameObject> collidableObjects) {
        this.collidableObjects = collidableObjects;
    }
    
    public MortarState getState() {
        return state;
    }
    
    public Direction getLookDirection() {
        return lookDirection;
    }
    
    public int getCurrentFrame() {
        return currentFrame;
    }
    
    public boolean shouldFlash() {
        return damageFlashTimer > 0 && ((int)(damageFlashTimer * 20) % 2 == 0);
    }
    
    @Override
    public float[] getBarrelPosition() {
        return new float[]{position.x(), position.y()};
    }
    
    @Override
    public boolean isInSpriteHitbox(float x, float y) {
        Vec2 bulletPos = new Vec2(x, y);
        Vec2 halfSize = new Vec2(32, 32);
        Vec2 min = position.subtract(halfSize);
        Vec2 max = position.add(halfSize);
        return bulletPos.x() >= min.x() && bulletPos.x() <= max.x() && 
               bulletPos.y() >= min.y() && bulletPos.y() <= max.y();
    }
}
