package com.dodgingbullets.gameobjects;

public interface Damageable {
    void takeDamage(int damage);
    boolean isDestroyed();
    int getHealth();
}
