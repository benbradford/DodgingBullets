package com.dodgingbullets.core;

public class Bullet {
    private Vec2 position;
    private Vec2 velocity;
    private long creationTime;
    private boolean isPlayerBullet;
    private boolean isSpecialBullet;
    private static final long LIFETIME = 1000; // 2 seconds in milliseconds
    private static final float SPEED = 10.0f;
    
    public Bullet(float startX, float startY, Direction direction, boolean isPlayerBullet) {
        this.position = new Vec2(startX, startY);
        this.isPlayerBullet = isPlayerBullet;
        this.creationTime = System.currentTimeMillis();
        
        // Set velocity based on direction
        switch (direction) {
            case UP: velocity = new Vec2(0, SPEED); break;
            case DOWN: velocity = new Vec2(0, -SPEED); break;
            case LEFT: velocity = new Vec2(-SPEED, 0); break;
            case RIGHT: velocity = new Vec2(SPEED, 0); break;
            case UP_LEFT: velocity = new Vec2(-SPEED * 0.7f, SPEED * 0.7f); break;
            case UP_RIGHT: velocity = new Vec2(SPEED * 0.7f, SPEED * 0.7f); break;
            case DOWN_LEFT: velocity = new Vec2(-SPEED * 0.7f, -SPEED * 0.7f); break;
            case DOWN_RIGHT: velocity = new Vec2(SPEED * 0.7f, -SPEED * 0.7f); break;
        }
    }
    
    public Bullet(float startX, float startY, double angle, boolean isPlayerBullet) {
        this(startX, startY, angle, isPlayerBullet, false);
    }
    
    public Bullet(float startX, float startY, double angle, boolean isPlayerBullet, boolean isSpecialBullet) {
        this.position = new Vec2(startX, startY);
        this.isPlayerBullet = isPlayerBullet;
        this.isSpecialBullet = isSpecialBullet;
        this.creationTime = System.currentTimeMillis();
        
        // Add spread for special bullets
        double finalAngle = angle;
        if (isSpecialBullet) {
            finalAngle += (Math.random() - 0.5) * 0.3; // ±0.15 radian spread
        }
        
        // Set velocity based on angle using Vec2.fromAngle
        this.velocity = Vec2.fromAngle(finalAngle, SPEED);
    }
    
    public void update() {
        position = position.add(velocity);
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > LIFETIME;
    }
    
    public float getX() { return position.x(); }
    public float getY() { return position.y(); }
    public boolean isPlayerBullet() { return isPlayerBullet; }
    public boolean isSpecialBullet() { return isSpecialBullet; }
}
