package com.dodgingbullets.core;

public interface Renderer {
    void initialize();
    void clear();
    void render(Texture texture, float x, float y, float width, float height);
    void renderRotated(Texture texture, float x, float y, float width, float height, float rotation);
    void renderRotatedWithAlpha(Texture texture, float x, float y, float width, float height, float rotation, float alpha);
    void renderRect(float x, float y, float width, float height, float r, float g, float b, float a);
    void renderRectOutline(float x, float y, float width, float height, float r, float g, float b, float a);
    void renderTextureWithColor(Texture texture, float x, float y, float width, float height, float r, float g, float b, float a);
    void present();
    void cleanup();
    Texture loadTexture(String path);
}
