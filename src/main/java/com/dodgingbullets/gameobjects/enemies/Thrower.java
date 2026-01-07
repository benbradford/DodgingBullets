package com.dodgingbullets.gameobjects.enemies;

import com.dodgingbullets.core.Vec2;
import com.dodgingbullets.core.Direction;
import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.effects.PetrolBomb;
import java.util.ArrayList;
import java.util.List;

public class Thrower extends EnemyObject implements Renderable, Collidable, Damageable, Trackable, Positionable {
    
    public enum ThrowerState {
        IDLE, CHASE, THROWING, BACKING_OFF, HIT, DYING
    }
    
    private ThrowerState state = ThrowerState.IDLE;
    private Direction facingDirection = Direction.RIGHT;
    private Vec2 playerPosition = new Vec2(0, 0);
    private List<GameObject> collidableObjects;
    private List<PetrolBomb> petrolBombs;
    
    // Animation
    private int currentFrame = 0;
    private float frameTimer = 0f;
    private boolean animationForward = true;
    private boolean hasThrownBomb = false;
    private static final float FRAME_DURATION = 0.15f;
    private static final float HIT_FRAME_DURATION = 0.075f;
    
    // State timers
    private float stateTimer = 0f;
    private static final float BACKING_OFF_DURATION = 1.0f;
    private static final float HIT_DURATION = 0.3f;
    private static final float THROW_DURATION = 1.05f; // 7 frames * 0.15s
    private static final float FADE_DURATION = 2.0f;
    
    // Movement and physics
    private Vec2 velocity = new Vec2(0, 0);
    private Vec2 knockbackVelocity = new Vec2(0, 0);
    private float rotation = 0f;
    private float rotationSpeed = 0f;
    private float alpha = 1.0f;
    
    // Smart movement variables
    private Vec2 randomDirection = new Vec2(0, 0);
    private float randomMoveTimer = 0f;
    private float directionCommitTimer = 0f;
    private float zigzagTimer = 0f;
    private float zigzagFrequency = 3.0f;
    private float zigzagAmplitude = 0.8f;
    private float zigzagChangeTimer = 0f;
    
    // Constants
    private static final float SIGHT_RANGE = 350f;
    private static final float THROW_RANGE = 150f;
    private final float moveSpeed;
    private static final float KNOCKBACK_FORCE = 100f;
    private static final float FRICTION = 0.85f;
    private static final float ROTATION_FRICTION = 0.95f;
    private static final float RANDOM_MOVE_INTERVAL = 1.0f;
    private static final float MIN_DIRECTION_COMMIT_TIME = 0.3f;
    private static final float THROW_ACCURACY_OFFSET = 0.2f;
    private static final float ZIGZAG_CHANGE_INTERVAL = 1.5f;
    
    public Thrower(float x, float y, Direction initialDirection, List<GameObject> collidableObjects, List<PetrolBomb> petrolBombs) {
        this(x, y, initialDirection, collidableObjects, petrolBombs, 100, 120f); // Default values
    }
    
    public Thrower(float x, float y, Direction initialDirection, List<GameObject> collidableObjects, List<PetrolBomb> petrolBombs, int health, float speed) {
        super(x, y, health);
        this.facingDirection = initialDirection;
        this.moveSpeed = speed;
        this.collidableObjects = collidableObjects != null ? collidableObjects : new ArrayList<>();
        this.petrolBombs = petrolBombs != null ? petrolBombs : new ArrayList<>();
    }
    
    @Override
    public void update(float deltaTime) {
        updateState(deltaTime);
        updateAnimation(deltaTime);
        updatePhysics(deltaTime);
        
        // Update smart movement timers
        randomMoveTimer += deltaTime;
        directionCommitTimer += deltaTime;
        zigzagTimer += deltaTime;
        zigzagChangeTimer += deltaTime;
        
        // Randomly change zigzag parameters
        if (zigzagChangeTimer >= ZIGZAG_CHANGE_INTERVAL) {
            zigzagFrequency = 2.0f + (float)(Math.random() * 3.0f); // 2.0 to 5.0
            zigzagAmplitude = 0.4f + (float)(Math.random() * 0.8f); // 0.4 to 1.2
            zigzagChangeTimer = 0f;
        }
    }
    
    private void updateState(float deltaTime) {
        stateTimer += deltaTime;
        
        switch (state) {
            case IDLE:
                if (canSeePlayer(playerPosition.x(), playerPosition.y())) {
                    float distance = position.distance(playerPosition);
                    if (distance <= SIGHT_RANGE) {
                        setState(ThrowerState.CHASE);
                    }
                }
                break;
                
            case CHASE:
                float distance = position.distance(playerPosition);
                if (distance <= THROW_RANGE) {
                    setState(ThrowerState.THROWING);
                } else {
                    moveTowardsPlayer(deltaTime);
                }
                break;
                
            case THROWING:
                // Exit when animation completes (reached final frame)
                if (currentFrame >= 6) {
                    setState(ThrowerState.BACKING_OFF);
                } else if (stateTimer >= THROW_DURATION / 2 && !hasThrownBomb) {
                    // Throw grenade halfway through animation
                    throwPetrolBomb();
                    hasThrownBomb = true;
                }
                break;
                
            case BACKING_OFF:
                if (stateTimer >= BACKING_OFF_DURATION) {
                    setState(ThrowerState.CHASE);
                } else {
                    moveAwayFromPlayer(deltaTime);
                }
                break;
                
            case HIT:
                if (stateTimer >= HIT_DURATION) {
                    if (health <= 0) {
                        setState(ThrowerState.DYING);
                    } else {
                        setState(ThrowerState.CHASE);
                    }
                }
                break;
                
            case DYING:
                // Handled in updatePhysics
                break;
        }
    }
    
    private void updateAnimation(float deltaTime) {
        frameTimer += deltaTime;
        float frameDuration = (state == ThrowerState.HIT) ? HIT_FRAME_DURATION : FRAME_DURATION;
        
        if (frameTimer >= frameDuration) {
            frameTimer = 0f;
            
            switch (state) {
                case IDLE:
                    currentFrame = 0; // Single frame
                    break;
                    
                case CHASE:
                case BACKING_OFF:
                    // Ping-pong through 4 frames (0-3)
                    if (animationForward) {
                        currentFrame++;
                        if (currentFrame >= 3) {
                            animationForward = false;
                        }
                    } else {
                        currentFrame--;
                        if (currentFrame <= 0) {
                            animationForward = true;
                        }
                    }
                    break;
                    
                case THROWING:
                    // Linear through 7 frames (0-6)
                    if (currentFrame < 6) {
                        currentFrame++;
                    }
                    break;
                    
                case HIT:
                case DYING:
                    currentFrame = 0; // Single frame
                    break;
            }
        }
    }
    
    private void updatePhysics(float deltaTime) {
        if (state == ThrowerState.DYING) {
            // Death physics
            rotation += rotationSpeed * deltaTime;
            rotationSpeed *= ROTATION_FRICTION;
            
            // Apply knockback with gravity
            position = position.add(knockbackVelocity.multiply(deltaTime));
            knockbackVelocity = knockbackVelocity.multiply(FRICTION);
            
            // Fade out
            if (stateTimer >= THROW_DURATION) { // After animation completes
                alpha -= deltaTime / FADE_DURATION;
                if (alpha <= 0) {
                    alpha = 0;
                    active = false;
                }
            }
        } else if (state == ThrowerState.HIT) {
            // Hit knockback
            position = position.add(knockbackVelocity.multiply(deltaTime));
            knockbackVelocity = knockbackVelocity.multiply(FRICTION);
        }
    }
    
    private void moveTowardsPlayer(float deltaTime) {
        Vec2 direction = playerPosition.subtract(position);
        float distance = direction.distance(new Vec2(0, 0));
        
        if (distance > 0) {
            Vec2 normalizedDirection = direction.multiply(1.0f / distance);
            Vec2 newVelocity = normalizedDirection.multiply(moveSpeed);
            Vec2 newPosition = position.add(newVelocity.multiply(deltaTime));
            
            // Check if direct path is blocked
            if (!isPositionBlocked(newPosition)) {
                // Add zigzag pattern during chase
                if (state == ThrowerState.CHASE) {
                    // Create perpendicular direction for zigzag
                    Vec2 perpendicular = new Vec2(-normalizedDirection.y(), normalizedDirection.x());
                    float zigzagOffset = (float) Math.sin(zigzagTimer * zigzagFrequency) * zigzagAmplitude;
                    Vec2 zigzagDirection = normalizedDirection.add(perpendicular.multiply(zigzagOffset));
                    
                    // Normalize the combined direction
                    float zigzagDistance = zigzagDirection.distance(new Vec2(0, 0));
                    if (zigzagDistance > 0) {
                        zigzagDirection = zigzagDirection.multiply(1.0f / zigzagDistance);
                    }
                    
                    Vec2 zigzagVelocity = zigzagDirection.multiply(moveSpeed);
                    Vec2 zigzagPosition = position.add(zigzagVelocity.multiply(deltaTime));
                    
                    // Use zigzag movement if not blocked, otherwise fall back to direct
                    if (!isPositionBlocked(zigzagPosition)) {
                        velocity = zigzagVelocity;
                        position = zigzagPosition;
                        updateFacingDirection(zigzagDirection);
                    } else {
                        velocity = newVelocity;
                        position = newPosition;
                        updateFacingDirection(normalizedDirection);
                    }
                } else {
                    // Direct path clear - move towards/away from player
                    velocity = newVelocity;
                    position = newPosition;
                    updateFacingDirection(normalizedDirection);
                }
            } else {
                // Path blocked - use smart movement to find way around
                if (randomMoveTimer >= RANDOM_MOVE_INTERVAL && directionCommitTimer >= MIN_DIRECTION_COMMIT_TIME) {
                    // Test X and Y movement separately
                    Vec2 xOnlyDirection = new Vec2(normalizedDirection.x(), 0);
                    if (Math.abs(normalizedDirection.x()) > 0) {
                        xOnlyDirection = xOnlyDirection.multiply(1.0f / Math.abs(normalizedDirection.x()));
                    }
                    Vec2 yOnlyDirection = new Vec2(0, normalizedDirection.y());
                    if (Math.abs(normalizedDirection.y()) > 0) {
                        yOnlyDirection = yOnlyDirection.multiply(1.0f / Math.abs(normalizedDirection.y()));
                    }
                    
                    Vec2 xTestPos = position.add(xOnlyDirection.multiply(moveSpeed * 0.5f * deltaTime));
                    Vec2 yTestPos = position.add(yOnlyDirection.multiply(moveSpeed * 0.5f * deltaTime));
                    
                    boolean xBlocked = isPositionBlocked(xTestPos);
                    boolean yBlocked = isPositionBlocked(yTestPos);
                    
                    Vec2 bestDirection = null;
                    
                    if (xBlocked && !yBlocked) {
                        // X movement blocked, try moving north/south
                        bestDirection = new Vec2(0, normalizedDirection.y() > 0 ? 1 : -1);
                    } else if (yBlocked && !xBlocked) {
                        // Y movement blocked, try moving east/west  
                        bestDirection = new Vec2(normalizedDirection.x() > 0 ? 1 : -1, 0);
                    } else if (xBlocked && yBlocked) {
                        // Both blocked, try perpendicular directions
                        Vec2[] perpendiculars = {
                            new Vec2(1, 0), new Vec2(-1, 0), new Vec2(0, 1), new Vec2(0, -1)
                        };
                        for (Vec2 testDir : perpendiculars) {
                            Vec2 testPos = position.add(testDir.multiply(moveSpeed * 0.5f * deltaTime));
                            if (!isPositionBlocked(testPos)) {
                                bestDirection = testDir;
                                break;
                            }
                        }
                    }
                    
                    if (bestDirection != null) {
                        randomDirection = bestDirection;
                        directionCommitTimer = 0.0f;
                    }
                    randomMoveTimer = 0.0f;
                }
                
                // Try random movement if we have a valid direction
                if (randomDirection.distance(new Vec2(0, 0)) > 0) {
                    Vec2 randomVelocity = randomDirection.multiply(moveSpeed * 0.5f);
                    Vec2 randomNewPosition = position.add(randomVelocity.multiply(deltaTime));
                    
                    if (!isPositionBlocked(randomNewPosition)) {
                        velocity = randomVelocity;
                        position = randomNewPosition;
                        updateFacingDirection(randomDirection);
                    }
                }
            }
        }
    }
    
    private void moveAwayFromPlayer(float deltaTime) {
        Vec2 direction = position.subtract(playerPosition);
        float distance = direction.distance(new Vec2(0, 0));
        
        if (distance > 0) {
            Vec2 normalizedDirection = direction.multiply(1.0f / distance);
            Vec2 newVelocity = normalizedDirection.multiply(moveSpeed * 1.5f);
            Vec2 newPosition = position.add(newVelocity.multiply(deltaTime));
            
            // Check if direct path is blocked
            if (!isPositionBlocked(newPosition)) {
                // Direct path clear - move away from player
                velocity = newVelocity;
                position = newPosition;
                updateFacingDirection(normalizedDirection);
            } else {
                // Path blocked - use smart movement to find way around
                if (randomMoveTimer >= RANDOM_MOVE_INTERVAL && directionCommitTimer >= MIN_DIRECTION_COMMIT_TIME) {
                    // Test X and Y movement separately
                    Vec2 xOnlyDirection = new Vec2(normalizedDirection.x(), 0);
                    if (Math.abs(normalizedDirection.x()) > 0) {
                        xOnlyDirection = xOnlyDirection.multiply(1.0f / Math.abs(normalizedDirection.x()));
                    }
                    Vec2 yOnlyDirection = new Vec2(0, normalizedDirection.y());
                    if (Math.abs(normalizedDirection.y()) > 0) {
                        yOnlyDirection = yOnlyDirection.multiply(1.0f / Math.abs(normalizedDirection.y()));
                    }
                    
                    Vec2 xTestPos = position.add(xOnlyDirection.multiply(moveSpeed * 1.5f * 0.5f * deltaTime));
                    Vec2 yTestPos = position.add(yOnlyDirection.multiply(moveSpeed * 1.5f * 0.5f * deltaTime));
                    
                    boolean xBlocked = isPositionBlocked(xTestPos);
                    boolean yBlocked = isPositionBlocked(yTestPos);
                    
                    Vec2 bestDirection = null;
                    
                    if (xBlocked && !yBlocked) {
                        bestDirection = new Vec2(0, normalizedDirection.y() > 0 ? 1 : -1);
                    } else if (yBlocked && !xBlocked) {
                        bestDirection = new Vec2(normalizedDirection.x() > 0 ? 1 : -1, 0);
                    } else if (xBlocked && yBlocked) {
                        Vec2[] perpendiculars = {
                            new Vec2(1, 0), new Vec2(-1, 0), new Vec2(0, 1), new Vec2(0, -1)
                        };
                        for (Vec2 testDir : perpendiculars) {
                            Vec2 testPos = position.add(testDir.multiply(moveSpeed * 1.5f * 0.5f * deltaTime));
                            if (!isPositionBlocked(testPos)) {
                                bestDirection = testDir;
                                break;
                            }
                        }
                    }
                    
                    if (bestDirection != null) {
                        randomDirection = bestDirection;
                        directionCommitTimer = 0.0f;
                    }
                    randomMoveTimer = 0.0f;
                }
                
                // Try random movement if we have a valid direction
                if (randomDirection.distance(new Vec2(0, 0)) > 0) {
                    Vec2 randomVelocity = randomDirection.multiply(moveSpeed * 1.5f * 0.5f);
                    Vec2 randomNewPosition = position.add(randomVelocity.multiply(deltaTime));
                    
                    if (!isPositionBlocked(randomNewPosition)) {
                        velocity = randomVelocity;
                        position = randomNewPosition;
                        updateFacingDirection(randomDirection);
                    }
                }
            }
        }
    }
    
    private boolean isPositionBlocked(Vec2 newPos) {
        if (collidableObjects == null) return false;
        
        for (GameObject obj : collidableObjects) {
            if (obj != this && obj instanceof Collidable) {
                if (((Collidable) obj).checkMovementCollision(newPos.x() - 32, newPos.y() - 16, 64, 32)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void updateFacingDirection(Vec2 direction) {
        double angle = Math.atan2(direction.y(), direction.x());
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) degrees += 360;
        
        if (degrees >= 337.5 || degrees < 22.5) facingDirection = Direction.RIGHT;
        else if (degrees >= 22.5 && degrees < 67.5) facingDirection = Direction.UP_RIGHT;
        else if (degrees >= 67.5 && degrees < 112.5) facingDirection = Direction.UP;
        else if (degrees >= 112.5 && degrees < 157.5) facingDirection = Direction.UP_LEFT;
        else if (degrees >= 157.5 && degrees < 202.5) facingDirection = Direction.LEFT;
        else if (degrees >= 202.5 && degrees < 247.5) facingDirection = Direction.DOWN_LEFT;
        else if (degrees >= 247.5 && degrees < 292.5) facingDirection = Direction.DOWN;
        else if (degrees >= 292.5 && degrees < 337.5) facingDirection = Direction.DOWN_RIGHT;
    }
    
    private void throwPetrolBomb() {
        // Calculate throw direction with some randomness
        Vec2 throwDirection = playerPosition.subtract(position);
        double baseAngle = Math.atan2(throwDirection.y(), throwDirection.x());
        double randomOffset = (Math.random() - THROW_ACCURACY_OFFSET) * THROW_ACCURACY_OFFSET;
        double throwAngle = baseAngle + randomOffset;
        
        Vec2 throwVelocity = Vec2.fromAngle(throwAngle, 200f); // 200 pixels/second
        
        PetrolBomb bomb = new PetrolBomb(position.x(), position.y(), throwVelocity, collidableObjects);
        petrolBombs.add(bomb);
    }
    
    private void setState(ThrowerState newState) {
        state = newState;
        stateTimer = 0f;
        
        if (newState == ThrowerState.THROWING || newState == ThrowerState.HIT || newState == ThrowerState.DYING) {
            currentFrame = 0;
            animationForward = true;
            if (newState == ThrowerState.THROWING) {
                hasThrownBomb = false; // Reset bomb throwing flag
            }
        } else if (newState == ThrowerState.BACKING_OFF || newState == ThrowerState.CHASE) {
            // Reset frame for walking animations
            currentFrame = 0;
            animationForward = true;
        }
    }
    
    @Override
    public void takeDamage(int damage) {
        if (state == ThrowerState.DYING) return;
        
        health -= damage;
        
        if (health <= 0) {
            // Death knockback
            Vec2 knockbackDirection = position.subtract(playerPosition);
            if (knockbackDirection.distance(new Vec2(0, 0)) > 0) {
                Vec2 normalizedKnockback = knockbackDirection.multiply(1.0f / knockbackDirection.distance(new Vec2(0, 0)));
                knockbackVelocity = normalizedKnockback.multiply(KNOCKBACK_FORCE * 3f);
                rotationSpeed = (facingDirection == Direction.RIGHT || facingDirection == Direction.UP_RIGHT || 
                               facingDirection == Direction.DOWN_RIGHT) ? 60f : -60f;
            }
            setState(ThrowerState.DYING);
        } else {
            // Hit knockback
            Vec2 knockbackDirection = position.subtract(playerPosition);
            if (knockbackDirection.distance(new Vec2(0, 0)) > 0) {
                Vec2 normalizedKnockback = knockbackDirection.multiply(1.0f / knockbackDirection.distance(new Vec2(0, 0)));
                knockbackVelocity = normalizedKnockback.multiply(KNOCKBACK_FORCE);
            }
            setState(ThrowerState.HIT);
        }
    }
    
    public void killInstantly() {
        health = 0;
        Vec2 knockbackDirection = position.subtract(playerPosition);
        if (knockbackDirection.distance(new Vec2(0, 0)) > 0) {
            Vec2 normalizedKnockback = knockbackDirection.multiply(1.0f / knockbackDirection.distance(new Vec2(0, 0)));
            knockbackVelocity = normalizedKnockback.multiply(KNOCKBACK_FORCE * 3f);
            rotationSpeed = (facingDirection == Direction.RIGHT || facingDirection == Direction.UP_RIGHT || 
                           facingDirection == Direction.DOWN_RIGHT) ? 60f : -60f;
        }
        setState(ThrowerState.DYING);
    }
    
    @Override
    public boolean canSeePlayer(float playerX, float playerY) {
        return hasLineOfSight(new Vec2(playerX, playerY));
    }
    
    @Override
    public boolean canSeePlayerInCurrentDirection(float playerX, float playerY) {
        // Throwers don't have directional vision like turrets, they can see in all directions
        return canSeePlayer(playerX, playerY);
    }
    
    @Override
    public void update(float playerX, float playerY) {
        // Update player position for AI logic
        setPlayerPosition(new Vec2(playerX, playerY));
    }
    
    private boolean hasLineOfSight(Vec2 targetPos) {
        if (collidableObjects == null) return true;
        
        Vec2 direction = targetPos.subtract(position);
        float distance = direction.distance(new Vec2(0, 0));
        
        if (distance == 0) return true;
        
        Vec2 normalizedDirection = direction.multiply(1.0f / distance);
        float stepSize = 8.0f;
        int steps = (int) (distance / stepSize);
        
        for (int i = 1; i < steps; i++) {
            Vec2 rayPoint = position.add(normalizedDirection.multiply(i * stepSize));
            
            for (GameObject obj : collidableObjects) {
                if (obj != this && obj instanceof Collidable && 
                    ((Collidable) obj).checkMovementCollision(rayPoint.x() - 1, rayPoint.y() - 1, 2, 2)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public void setPlayerPosition(Vec2 playerPos) {
        this.playerPosition = playerPos;
    }
    
    public void setCollidableObjects(List<GameObject> collidableObjects) {
        this.collidableObjects = collidableObjects;
    }
    
    public void setPetrolBombs(List<PetrolBomb> petrolBombs) {
        this.petrolBombs = petrolBombs;
    }
    
    // Interface implementations
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
        Vec2 bulletPos = new Vec2(x + width/2, y + height/2);
        Vec2 halfSize = new Vec2(32, 32);
        Vec2 min = position.subtract(halfSize);
        Vec2 max = position.add(halfSize);
        return bulletPos.x() >= min.x() && bulletPos.x() <= max.x() && 
               bulletPos.y() >= min.y() && bulletPos.y() <= max.y();
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        // Allow player to pass through thrower - no movement collision
        return false;
    }
    
    // Getters
    public ThrowerState getState() { return state; }
    public Direction getFacingDirection() { return facingDirection; }
    public int getCurrentFrame() { return currentFrame; }
    public float getRotation() { return rotation; }
    public float getAlpha() { return alpha; }
    
    // Positionable interface methods
    @Override
    public float[] getBarrelPosition() {
        // Return center position for simplicity
        return new float[]{position.x(), position.y()};
    }
    
    @Override
    public boolean isInSpriteHitbox(float x, float y) {
        return checkSpriteCollision(x, y, 1, 1);
    }
}
