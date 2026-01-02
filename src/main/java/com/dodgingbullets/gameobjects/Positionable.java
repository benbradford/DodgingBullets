package com.dodgingbullets.gameobjects;

public interface Positionable {
    float[] getBarrelPosition();
    boolean isInSpriteHitbox(float x, float y);
}
