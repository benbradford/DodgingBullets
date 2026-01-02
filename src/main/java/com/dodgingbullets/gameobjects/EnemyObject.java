package com.dodgingbullets.gameobjects;

public abstract class EnemyObject extends GameObject implements Renderable, Collidable, Damageable {
    protected int health;
    protected int maxHealth;
    
    public EnemyObject(float x, float y, int health) {
        super(x, y);
        this.health = health;
        this.maxHealth = health;
    }
    
    @Override
    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            active = false;
        }
    }
    
    @Override
    public boolean isDestroyed() {
        return health <= 0;
    }
    
    @Override
    public int getHealth() {
        return health;
    }
    
    @Override
    public float getRenderY() {
        return y;
    }
}
