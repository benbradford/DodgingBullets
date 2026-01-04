package com.dodgingbullets.gameobjects.enemies;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.core.Direction;
import com.dodgingbullets.core.Vec2;

import java.util.List;

public class Bear extends EnemyObject implements Trackable, Positionable {
    
    public enum BearState {
        IDLE, WAKING_UP, RUNNING, HIT, DYING
    }
    
    private BearState state = BearState.IDLE;
    private Direction facingDirection;
    private Vec2 velocity = new Vec2(0, 0);
    private Vec2 knockbackVelocity = new Vec2(0, 0);
    private Vec2 playerPosition = new Vec2(0, 0);
    private List<GameObject> collidableObjects;
    
    // Animation
    private int currentFrame = 0;
    private float frameTimer = 0;
    private boolean animationForward = true;
    private static final float FRAME_DURATION = 0.15f;
    private static final float HIT_FRAME_DURATION = 0.075f; // 2x faster
    
    // State timers
    private float stateTimer = 0;
    private float fadeAlpha = 1.0f;
    private float rotationAngle = 0.0f;
    private float rotationSpeed = 0.0f;
    private float verticalVelocity = 0.0f;
    private float groundY = 0.0f;
    private Vec2 randomDirection = new Vec2(0, 0);
    private float randomMoveTimer = 0.0f;
    private float directionCommitTimer = 0.0f;
    
    // Constants
    private static final float SIGHT_RANGE = 250f;
    private static final float ATTACK_RANGE = 30f;
    private static final float MOVE_SPEED = 150f;
    private static final float KNOCKBACK_FORCE = 200f;
    private static final float FRICTION = 0.85f;
    private static final float GRAVITY = 300f;
    private static final float INITIAL_JUMP_VELOCITY = 150f;
    private static final float ROTATION_FRICTION = 0.95f;
    private static final float RANDOM_MOVE_INTERVAL = 1.0f;
    private static final float MIN_DIRECTION_COMMIT_TIME = 0.3f;
    private static final int DAMAGE_PER_SECOND = 20;
    private static final float WAKEUP_DURATION = 0.9f; // 6 frames * 0.15s
    private static final float HIT_DURATION = 0.825f; // 11 frames * 0.075s
    private static final float FADE_DURATION = 2.0f;
    
    public Bear(float x, float y) {
        this(x, y, Direction.RIGHT); // Default to east
    }
    
    public Bear(float x, float y, Direction facingDirection) {
        super(x, y, 50);
        this.facingDirection = facingDirection;
    }
    
    @Override
    public void update(float deltaTime) {
        if (!active) return;
        
        frameTimer += deltaTime;
        stateTimer += deltaTime;
        randomMoveTimer += deltaTime;
        directionCommitTimer += deltaTime;
        
        // Apply knockback velocity with friction
        if (knockbackVelocity.distance(new Vec2(0, 0)) > 1f) {
            Vec2 newPosition = position.add(knockbackVelocity.multiply(deltaTime));
            
            // Check collision before applying knockback movement
            if (!checkCollisionAtPosition(newPosition)) {
                position = newPosition;
            } else {
                // Stop knockback if hitting obstacle
                knockbackVelocity = new Vec2(0, 0);
            }
            
            knockbackVelocity = knockbackVelocity.multiply(FRICTION);
        }
        
        updateAnimation(deltaTime);
        updateState(deltaTime);
    }
    
    @Override
    public void update(float playerX, float playerY) {
        playerPosition = new Vec2(playerX, playerY);
        
        if (state == BearState.IDLE) {
            float distance = position.distance(playerPosition);
            if (distance <= SIGHT_RANGE && hasLineOfSight()) {
                state = BearState.WAKING_UP;
                stateTimer = 0;
                currentFrame = 0;
                animationForward = true;
            }
        }
    }
    
    private void updateState(float deltaTime) {
        switch (state) {
            case IDLE:
                break;
                
            case WAKING_UP:
                if (stateTimer >= WAKEUP_DURATION) {
                    state = BearState.RUNNING;
                    stateTimer = 0;
                    currentFrame = 0;
                    animationForward = true;
                }
                break;
                
            case RUNNING:
                moveTowardsPlayer(deltaTime);
                break;
                
            case HIT:
                if (stateTimer >= HIT_DURATION) {
                    if (health <= 0) {
                        state = BearState.DYING;
                        stateTimer = 0;
                        currentFrame = 0;
                        animationForward = true;
                    } else {
                        state = BearState.RUNNING;
                        stateTimer = 0;
                        currentFrame = 0;
                        animationForward = true;
                    }
                }
                break;
                
            case DYING:
                // Animate to frame 7, then fade out over 2 seconds
                float animationTime = 7 * FRAME_DURATION; // Time to reach frame 7
                
                // Apply rotation with friction (slowing down over time)
                rotationAngle += rotationSpeed * deltaTime;
                rotationSpeed *= ROTATION_FRICTION;
                
                // Apply vertical physics (arc movement)
                verticalVelocity -= GRAVITY * deltaTime;
                float newY = position.y() + verticalVelocity * deltaTime;
                
                // Don't go below ground level
                if (newY < groundY) {
                    newY = groundY;
                    verticalVelocity = 0;
                }
                
                position = new Vec2(position.x(), newY);
                
                if (currentFrame >= 7) {
                    if (stateTimer >= animationTime + FADE_DURATION) {
                        active = false;
                    } else if (stateTimer >= animationTime) {
                        float fadeProgress = (stateTimer - animationTime) / FADE_DURATION;
                        fadeAlpha = 1.0f - fadeProgress;
                    }
                }
                break;
        }
    }
    
    private void updateAnimation(float deltaTime) {
        float frameDuration = (state == BearState.HIT) ? HIT_FRAME_DURATION : 
                             (state == BearState.DYING) ? FRAME_DURATION : FRAME_DURATION;
        
        if (frameTimer >= frameDuration) {
            frameTimer = 0;
            
            switch (state) {
                case IDLE:
                    // Ping-pong through 6 frames (0-5)
                    if (animationForward) {
                        currentFrame++;
                        if (currentFrame >= 5) {
                            animationForward = false;
                        }
                    } else {
                        currentFrame--;
                        if (currentFrame <= 0) {
                            animationForward = true;
                        }
                    }
                    break;
                    
                case WAKING_UP:
                    // Play through 6 frames once
                    currentFrame++;
                    if (currentFrame >= 6) {
                        currentFrame = 5; // Hold on last frame
                    }
                    break;
                    
                case RUNNING:
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
                    
                case HIT:
                    // Play through 11 frames once
                    if (currentFrame < 11) {
                        currentFrame++;
                    }
                    break;
                    
                case DYING:
                    // Animate slowly to frame 7, then hold
                    if (currentFrame < 7) {
                        currentFrame++;
                    }
                    break;
            }
        }
    }
    
    private void moveTowardsPlayer(float deltaTime) {
        Vec2 direction = playerPosition.subtract(position);
        float distance = direction.distance(new Vec2(0, 0));
        
        if (distance > 0) {
            Vec2 normalizedDirection = direction.multiply(1.0f / distance);
            velocity = normalizedDirection.multiply(MOVE_SPEED);
            
            // Calculate new position
            Vec2 newPosition = position.add(velocity.multiply(deltaTime));
            
            // Check if direct path is blocked
            if (!checkCollisionAtPosition(newPosition)) {
                // Direct path clear - move towards player
                position = newPosition;
                updateFacingDirection(normalizedDirection);
            } else {
                // Path blocked - use smart movement to find way around
                if (randomMoveTimer >= RANDOM_MOVE_INTERVAL && directionCommitTimer >= MIN_DIRECTION_COMMIT_TIME) {
                    // Determine which axis is blocked by testing X and Y movement separately
                    Vec2 xOnlyDirection = new Vec2(normalizedDirection.x(), 0).multiply(1.0f / Math.abs(normalizedDirection.x()));
                    Vec2 yOnlyDirection = new Vec2(0, normalizedDirection.y()).multiply(1.0f / Math.abs(normalizedDirection.y()));
                    
                    Vec2 xTestPos = position.add(xOnlyDirection.multiply(MOVE_SPEED * 0.5f * deltaTime));
                    Vec2 yTestPos = position.add(yOnlyDirection.multiply(MOVE_SPEED * 0.5f * deltaTime));
                    
                    boolean xBlocked = checkCollisionAtPosition(xTestPos);
                    boolean yBlocked = checkCollisionAtPosition(yTestPos);
                    
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
                            Vec2 testPos = position.add(testDir.multiply(MOVE_SPEED * 0.5f * deltaTime));
                            if (!checkCollisionAtPosition(testPos)) {
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
                    Vec2 randomVelocity = randomDirection.multiply(MOVE_SPEED * 0.5f);
                    Vec2 randomNewPosition = position.add(randomVelocity.multiply(deltaTime));
                    
                    if (!checkCollisionAtPosition(randomNewPosition)) {
                        position = randomNewPosition;
                        updateFacingDirection(randomDirection);
                    }
                }
            }
        }
    }
    
    private boolean checkCollisionAtPosition(Vec2 newPos) {
        if (collidableObjects == null) return false;
        
        // Bear movement hitbox (64x32 pixels, bottom half)
        float bearWidth = 64;
        float bearHeight = 32;
        float bearLeft = newPos.x() - 32;
        float bearBottom = newPos.y() - 16;
        
        for (GameObject obj : collidableObjects) {
            if (obj != this && obj instanceof Collidable && 
                ((Collidable) obj).checkMovementCollision(bearLeft, bearBottom, bearWidth, bearHeight)) {
                return true;
            }
        }
        return false;
    }
    
    public void setCollidableObjects(List<GameObject> collidableObjects) {
        this.collidableObjects = collidableObjects;
    }
    
    private void updateFacingDirection(Vec2 direction) {
        float angle = (float) Math.atan2(direction.y(), direction.x());
        angle = (float) Math.toDegrees(angle);
        if (angle < 0) angle += 360;
        
        // Convert angle to 8-directional facing
        if (angle >= 337.5f || angle < 22.5f) {
            facingDirection = Direction.RIGHT; // East
        } else if (angle >= 22.5f && angle < 67.5f) {
            facingDirection = Direction.UP_RIGHT; // Northeast
        } else if (angle >= 67.5f && angle < 112.5f) {
            facingDirection = Direction.UP; // North
        } else if (angle >= 112.5f && angle < 157.5f) {
            facingDirection = Direction.UP_LEFT; // Northwest
        } else if (angle >= 157.5f && angle < 202.5f) {
            facingDirection = Direction.LEFT; // West
        } else if (angle >= 202.5f && angle < 247.5f) {
            facingDirection = Direction.DOWN_LEFT; // Southwest
        } else if (angle >= 247.5f && angle < 292.5f) {
            facingDirection = Direction.DOWN; // South
        } else if (angle >= 292.5f && angle < 337.5f) {
            facingDirection = Direction.DOWN_RIGHT; // Southeast
        }
    }
    
    private void checkPlayerAttack() {
        float distance = position.distance(playerPosition);
        if (distance <= ATTACK_RANGE) {
            // Player takes damage - this will be handled by GameLoop
        }
    }
    
    public boolean isAttackingPlayer() { 
        return state == BearState.RUNNING && position.distance(playerPosition) <= ATTACK_RANGE; 
    }
    
    private boolean hasLineOfSight() {
        if (collidableObjects == null) return true;
        
        // Cast a ray from bear to player
        Vec2 direction = playerPosition.subtract(position);
        float distance = direction.distance(new Vec2(0, 0));
        
        if (distance == 0) return true;
        
        Vec2 normalizedDirection = direction.multiply(1.0f / distance);
        
        // Step along the ray and check for collisions
        float stepSize = 8.0f; // Check every 8 pixels
        int steps = (int) (distance / stepSize);
        
        for (int i = 1; i < steps; i++) {
            Vec2 rayPoint = position.add(normalizedDirection.multiply(i * stepSize));
            
            // Check if this point collides with any blocking object
            for (GameObject obj : collidableObjects) {
                if (obj != this && obj instanceof Collidable && 
                    ((Collidable) obj).checkMovementCollision(rayPoint.x() - 1, rayPoint.y() - 1, 2, 2)) {
                    return false; // Line of sight blocked
                }
            }
        }
        
        return true; // Clear line of sight
    }
    
    @Override
    public void takeDamage(int damage) {
        if (state == BearState.DYING) return;
        
        health -= damage;
        
        // Check if this hit will kill the bear
        if (health <= 0) {
            // Go straight to dying state
            state = BearState.DYING;
            stateTimer = 0;
            currentFrame = 0;
            groundY = position.y(); // Remember ground position
            
            // Set rotation speed and vertical velocity based on knockback direction
            Vec2 knockbackDirection = position.subtract(playerPosition);
            float distance = knockbackDirection.distance(new Vec2(0, 0));
            if (distance > 0) {
                Vec2 normalizedKnockback = knockbackDirection.multiply(1.0f / distance);
                knockbackVelocity = normalizedKnockback.multiply(KNOCKBACK_FORCE * 3f);
                
                // Determine rotation direction based on facing direction
                rotationSpeed = (facingDirection == Direction.RIGHT) ? 60f : -60f;
                
                // Add upward velocity for arc effect
                verticalVelocity = INITIAL_JUMP_VELOCITY;
            }
        } else {
            // Enter hit state
            state = BearState.HIT;
            stateTimer = 0;
            currentFrame = 0;
            
            // Apply knockback force in opposite direction from player
            Vec2 knockbackDirection = position.subtract(playerPosition);
            float distance = knockbackDirection.distance(new Vec2(0, 0));
            if (distance > 0) {
                Vec2 normalizedKnockback = knockbackDirection.multiply(1.0f / distance);
                knockbackVelocity = normalizedKnockback.multiply(KNOCKBACK_FORCE);
            }
        }
    }
    
    // Trackable interface methods
    @Override
    public boolean canSeePlayer(float playerX, float playerY) {
        float distance = position.distance(new Vec2(playerX, playerY));
        return distance <= SIGHT_RANGE && hasLineOfSight();
    }
    
    @Override
    public boolean canSeePlayerInCurrentDirection(float playerX, float playerY) {
        return canSeePlayer(playerX, playerY);
    }
    
    @Override
    public Direction getFacingDirection() {
        return facingDirection;
    }
    
    // Collidable interface methods
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        // Bear sprite collision (64x64 pixels)
        Vec2 bulletPos = new Vec2(x, y);
        Vec2 halfSize = new Vec2(32, 32);
        Vec2 min = position.subtract(halfSize);
        Vec2 max = position.add(halfSize);
        return bulletPos.x() >= min.x() && bulletPos.x() <= max.x() && 
               bulletPos.y() >= min.y() && bulletPos.y() <= max.y();
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        // Bears don't block movement
        return false;
    }
    
    // Renderable interface methods
    @Override
    public void render() {
        // Rendering handled by GameRenderer
    }
    
    @Override
    public boolean isDestroyed() {
        return !active;
    }
    @Override
    public float[] getBarrelPosition() {
        return new float[]{position.x(), position.y()};
    }
    
    @Override
    public boolean isInSpriteHitbox(float x, float y) {
        // No hitbox when dying
        if (state == BearState.DYING) return false;
        return checkSpriteCollision(x, y, 1, 1);
    }
    
    // Getters for rendering
    public BearState getState() { return state; }
    public int getCurrentFrame() { return currentFrame; }
    public float getFadeAlpha() { return fadeAlpha; }
    public float getRotationAngle() { return rotationAngle; }
}
