package com.dodgingbullets.core;

public class Bullet {
    private float x, y;
    private float velocityX, velocityY;
    private long creationTime;
    private boolean isPlayerBullet;
    private static final long LIFETIME = 1000; // 2 seconds in milliseconds
    private static final float SPEED = 10.0f;
    
    public Bullet(float startX, float startY, Direction direction, boolean isPlayerBullet) {
        this.x = startX;
        this.y = startY;
        this.isPlayerBullet = isPlayerBullet;
        this.creationTime = System.currentTimeMillis();
        
        // Set velocity based on direction
        switch (direction) {
            case UP: velocityX = 0; velocityY = SPEED; break;
            case DOWN: velocityX = 0; velocityY = -SPEED; break;
            case LEFT: velocityX = -SPEED; velocityY = 0; break;
            case RIGHT: velocityX = SPEED; velocityY = 0; break;
            case UP_LEFT: velocityX = -SPEED * 0.7f; velocityY = SPEED * 0.7f; break;
            case UP_RIGHT: velocityX = SPEED * 0.7f; velocityY = SPEED * 0.7f; break;
            case DOWN_LEFT: velocityX = -SPEED * 0.7f; velocityY = -SPEED * 0.7f; break;
            case DOWN_RIGHT: velocityX = SPEED * 0.7f; velocityY = -SPEED * 0.7f; break;
        }
    }
    
    public Bullet(float startX, float startY, double angle, boolean isPlayerBullet) {
        this.x = startX;
        this.y = startY;
        this.isPlayerBullet = isPlayerBullet;
        this.creationTime = System.currentTimeMillis();
        
        // Set velocity based on angle
        velocityX = (float)(Math.cos(angle) * SPEED);
        velocityY = (float)(Math.sin(angle) * SPEED);
    }
    
    public void update() {
        x += velocityX;
        y += velocityY;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > LIFETIME;
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isPlayerBullet() { return isPlayerBullet; }
}
