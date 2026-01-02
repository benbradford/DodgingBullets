package com.dodgingbullets.gameobjects;

public interface Shooter {
    void shoot(float targetX, float targetY);
    boolean canShoot();
}
