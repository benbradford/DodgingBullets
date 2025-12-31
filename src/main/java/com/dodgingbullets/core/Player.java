package com.dodgingbullets.core;

import java.util.HashMap;
import java.util.Map;

public class Player {
    private float x, y;
    private Direction currentDirection = Direction.UP;
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
    
    public void loadTextures(Renderer renderer) {
        for (Direction dir : Direction.values()) {
            String prefix = "mc" + dir.getPrefix();
            textures.put(prefix + "idle", renderer.loadTexture("assets/" + prefix + "idle.png"));
            textures.put(prefix + "01", renderer.loadTexture("assets/" + prefix + "01.png"));
            textures.put(prefix + "02", renderer.loadTexture("assets/" + prefix + "02.png"));
            textures.put(prefix + "03", renderer.loadTexture("assets/" + prefix + "03.png"));
        }
    }
    
    public void update(boolean[] keys) {
        boolean wasMoving = isMoving;
        isMoving = false;
        
        if (keys[0]) { // UP
            if (keys[2]) { // LEFT
                currentDirection = Direction.UP_LEFT;
                y += 2;
                x -= 2;
                isMoving = true;
            } else if (keys[3]) { // RIGHT
                currentDirection = Direction.UP_RIGHT;
                y += 2;
                x += 2;
                isMoving = true;
            } else {
                currentDirection = Direction.UP;
                y += 2;
                isMoving = true;
            }
        } else if (keys[1]) { // DOWN
            if (keys[2]) { // LEFT
                currentDirection = Direction.DOWN_LEFT;
                y -= 2;
                x -= 2;
                isMoving = true;
            } else if (keys[3]) { // RIGHT
                currentDirection = Direction.DOWN_RIGHT;
                y -= 2;
                x += 2;
                isMoving = true;
            } else {
                currentDirection = Direction.DOWN;
                y -= 2;
                isMoving = true;
            }
        } else if (keys[2]) { // LEFT
            currentDirection = Direction.LEFT;
            x -= 2;
            isMoving = true;
        } else if (keys[3]) { // RIGHT
            currentDirection = Direction.RIGHT;
            x += 2;
            isMoving = true;
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
        String prefix = "mc" + currentDirection.getPrefix();
        if (isMoving) {
            return textures.get(prefix + String.format("%02d", animationFrame + 1));
        } else {
            return textures.get(prefix + "idle");
        }
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
}
