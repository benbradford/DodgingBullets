package com.dodgingbullets.desktop;

import com.dodgingbullets.core.Renderer;
import com.dodgingbullets.core.Texture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class DesktopRenderer implements Renderer {
    
    @Override
    public void initialize() {
        GL.createCapabilities();
        
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 704, 0, 396, -1, 1); // Android phone aspect ratio (16:9) landscape - 10% zoom out
        
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
    }
    
    @Override
    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT);
    }
    
    @Override
    public void render(Texture texture, float x, float y, float width, float height) {
        glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
        
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1); glVertex2f(x, y);
        glTexCoord2f(1, 1); glVertex2f(x + width, y);
        glTexCoord2f(1, 0); glVertex2f(x + width, y + height);
        glTexCoord2f(0, 0); glVertex2f(x, y + height);
        glEnd();
    }
    
    @Override
    public void renderRotated(Texture texture, float x, float y, float width, float height, float rotation) {
        glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
        
        glPushMatrix();
        glTranslatef(x + width/2, y + height/2, 0);
        glRotatef((float)Math.toDegrees(rotation), 0, 0, 1);
        glTranslatef(-width/2, -height/2, 0);
        
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1); glVertex2f(0, 0);
        glTexCoord2f(1, 1); glVertex2f(width, 0);
        glTexCoord2f(1, 0); glVertex2f(width, height);
        glTexCoord2f(0, 0); glVertex2f(0, height);
        glEnd();
        
        glPopMatrix();
    }
    
    @Override
    public void renderRotatedWithAlpha(Texture texture, float x, float y, float width, float height, float rotation, float alpha) {
        glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
        
        glPushMatrix();
        glColor4f(1.0f, 1.0f, 1.0f, alpha); // Set alpha
        glTranslatef(x + width/2, y + height/2, 0);
        glRotatef((float)Math.toDegrees(rotation), 0, 0, 1);
        glTranslatef(-width/2, -height/2, 0);
        
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1); glVertex2f(0, 0);
        glTexCoord2f(1, 1); glVertex2f(width, 0);
        glTexCoord2f(1, 0); glVertex2f(width, height);
        glTexCoord2f(0, 0); glVertex2f(0, height);
        glEnd();
        
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Reset alpha
        glPopMatrix();
    }
    
    @Override
    public void renderRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(r, g, b, a);
        
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
        
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glEnable(GL_TEXTURE_2D);
    }
    
    @Override
    public void renderRectOutline(float x, float y, float width, float height, float r, float g, float b, float a) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(r, g, b, a);
        
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
        
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glEnable(GL_TEXTURE_2D);
    }
    
    @Override
    public void renderTextureWithColor(Texture texture, float x, float y, float width, float height, float r, float g, float b, float a) {
        glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
        glColor4f(r, g, b, a);
        
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1); glVertex2f(x, y);
        glTexCoord2f(1, 1); glVertex2f(x + width, y);
        glTexCoord2f(1, 0); glVertex2f(x + width, y + height);
        glTexCoord2f(0, 0); glVertex2f(x, y + height);
        glEnd();
        
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    @Override
    public void present() {
        // GLFW swap buffers is handled in main loop
    }
    
    @Override
    public void cleanup() {
        // Cleanup handled in main
    }
    
    @Override
    public Texture loadTexture(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            STBImage.stbi_set_flip_vertically_on_load(false);
            ByteBuffer image = STBImage.stbi_load(path, w, h, channels, 4);
            
            if (image == null) {
                throw new RuntimeException("Failed to load texture: " + STBImage.stbi_failure_reason());
            }
            
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            
            STBImage.stbi_image_free(image);
            
            return new Texture(textureId, w.get(0), h.get(0));
        }
    }
}
