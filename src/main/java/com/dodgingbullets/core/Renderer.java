package com.dodgingbullets.core;

public interface Renderer {
    void initialize();
    void clear();
    void render(Texture texture, float x, float y, float width, float height);
    void present();
    void cleanup();
    Texture loadTexture(String path);
}
