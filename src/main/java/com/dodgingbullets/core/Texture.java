package com.dodgingbullets.core;

public class Texture {
    private int textureId;
    private int width;
    private int height;
    
    public Texture(int textureId, int width, int height) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
    }
    
    public int getTextureId() { return textureId; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
