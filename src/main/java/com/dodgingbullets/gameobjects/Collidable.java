package com.dodgingbullets.gameobjects;

public interface Collidable {
    boolean checkSpriteCollision(float x, float y, float width, float height);
    boolean checkMovementCollision(float x, float y, float width, float height);
}
