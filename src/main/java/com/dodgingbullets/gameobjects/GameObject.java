package com.dodgingbullets.gameobjects;

import com.dodgingbullets.core.Vec2;

public abstract class GameObject {
    protected Vec2 position;
    protected boolean active = true;
    
    public GameObject(float x, float y) {
        this.position = new Vec2(x, y);
    }
    
    public GameObject(Vec2 position) {
        this.position = position;
    }
    
    public abstract void update(float deltaTime);
    
    public float getX() { return position.x(); }
    public float getY() { return position.y(); }
    public Vec2 getPosition() { return position; }
    public void setPosition(Vec2 position) { this.position = position; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
