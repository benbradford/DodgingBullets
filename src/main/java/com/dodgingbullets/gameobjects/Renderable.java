package com.dodgingbullets.gameobjects;

public interface Renderable {
    void render();
    float getRenderY(); // For depth sorting
}
