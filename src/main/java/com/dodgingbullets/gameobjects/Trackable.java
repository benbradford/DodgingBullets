package com.dodgingbullets.gameobjects;

import com.dodgingbullets.core.Direction;

public interface Trackable {
    void update(float playerX, float playerY);
    boolean canSeePlayer(float playerX, float playerY);
    boolean canSeePlayerInCurrentDirection(float playerX, float playerY);
    Direction getFacingDirection();
}
