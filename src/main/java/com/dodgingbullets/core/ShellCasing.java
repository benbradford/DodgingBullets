package com.dodgingbullets.core;

public class ShellCasing {
    private float x, y, ground;
    private float velocityX, velocityY;
    private float rotation;
    private float rotationSpeed;
    private long creationTime;
    private static final long SOLID_LIFETIME = 1200;
    private static final long FADE_DURATION = 1000; // 1 second fade
    private static final long TOTAL_LIFETIME = SOLID_LIFETIME + FADE_DURATION;
    private static final float GRAVITY = 0.4f;
    private static final float INITIAL_UPWARD_VELOCITY = 3.0f;
    
    public ShellCasing(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        this.ground = startY - 40 + (float)(Math.random() * 10 - 5); // -45 to -35 range

        this.creationTime = System.currentTimeMillis();
        
        // Random horizontal velocity (shell ejects to the side)
        this.velocityX = (float)(Math.random() * 4 - 2); // -2 to +2
        this.velocityY = INITIAL_UPWARD_VELOCITY + (float)(Math.random() * 2); // 3-5 upward
        
        // Random rotation
        this.rotation = (float)(Math.random() * Math.PI * 2);
        this.rotationSpeed = (float)(Math.random() * 0.3 + 0.1); // 0.1 to 0.4 rad/frame
    }
    
    public void update() {
        x += velocityX;
        y += velocityY;
        velocityY -= GRAVITY;
        rotation += rotationSpeed;
        
        // Bounce when hitting ground level (just below player sprite bottom)
        float groundLevel = this.ground;
        if (velocityY < 0 && y <= groundLevel) {
            y = groundLevel;
            velocityY = Math.abs(velocityY) * 0.3f; // Small bounce
            velocityX *= 0.8f; // Friction
            rotationSpeed *= 0.7f; // Slow rotation
        }
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > TOTAL_LIFETIME;
    }
    
    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - creationTime;
        if (elapsed < SOLID_LIFETIME) {
            return 1.0f; // Fully opaque
        } else {
            // Fade out over the last second
            long fadeElapsed = elapsed - SOLID_LIFETIME;
            return 1.0f - (fadeElapsed / (float)FADE_DURATION);
        }
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public float getRotation() { return rotation; }
}
