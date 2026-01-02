package com.dodgingbullets.gameobjects;

public abstract class GameObject {
    protected float x, y;
    protected boolean active = true;
    
    public GameObject(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public abstract void update(float deltaTime);
    
    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
