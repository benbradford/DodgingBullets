package com.dodgingbullets.desktop;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import java.nio.ByteBuffer;
import java.io.InputStream;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;

public class FontRenderer {
    private int fontTexture;
    private STBTTBakedChar.Buffer charData;
    private int fontSize = 18;
    
    public boolean initialize() {
        try {
            // Load embedded font data
            ByteBuffer fontBuffer = loadFontData();
            if (fontBuffer == null) {
                System.out.println("No system font found, using fallback");
                return false;
            }
            
            System.out.println("Font loaded successfully, creating atlas...");
            
            // Create font atlas
            int atlasWidth = 512;
            int atlasHeight = 512;
            ByteBuffer bitmap = BufferUtils.createByteBuffer(atlasWidth * atlasHeight);
            charData = STBTTBakedChar.malloc(96); // ASCII 32-127
            
            // Bake font into bitmap
            int result = stbtt_BakeFontBitmap(fontBuffer, fontSize, bitmap, atlasWidth, atlasHeight, 32, charData);
            System.out.println("Font baking result: " + result);
            
            // Create OpenGL texture
            fontTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, fontTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, atlasWidth, atlasHeight, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            
            System.out.println("Font texture created: " + fontTexture);
            return true;
        } catch (Exception e) {
            System.err.println("Font initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private ByteBuffer loadFontData() {
        // Try to load system font
        String[] fontPaths = {
            //"/System/Library/Fonts/Geneva.ttf",
            "/System/Library/Fonts/Monaco.ttf",
            "/System/Library/Fonts/Helvetica.ttc"
        };
        
        for (String path : fontPaths) {
            try {
                java.nio.file.Path fontPath = java.nio.file.Paths.get(path);
                if (java.nio.file.Files.exists(fontPath)) {
                    System.out.println("Loading font: " + path);
                    byte[] fontBytes = java.nio.file.Files.readAllBytes(fontPath);
                    return BufferUtils.createByteBuffer(fontBytes.length).put(fontBytes).flip();
                }
            } catch (Exception e) {
                System.out.println("Failed to load " + path + ": " + e.getMessage());
            }
        }
        return null;
    }
    
    public void renderText(String text, float x, float y, float r, float g, float b) {
        if (fontTexture == 0 || charData == null) {
            renderGeometricText(text, x, y, r, g, b);
            return;
        }
        
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, fontTexture);
        glColor3f(r, g, b);
        
        glBegin(GL_QUADS);
        
        float currentX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 32 && c < 127) {
                STBTTBakedChar bc = charData.get(c - 32);
                
                float x0 = currentX + bc.xoff();
                float y0 = y + bc.yoff();
                float x1 = x0 + (bc.x1() - bc.x0());
                float y1 = y0 + (bc.y1() - bc.y0());
                
                float s0 = bc.x0() / 512.0f;
                float t0 = bc.y0() / 512.0f;
                float s1 = bc.x1() / 512.0f;
                float t1 = bc.y1() / 512.0f;
                
                glTexCoord2f(s0, t1); glVertex2f(x0, y0);
                glTexCoord2f(s1, t1); glVertex2f(x1, y0);
                glTexCoord2f(s1, t0); glVertex2f(x1, y1);
                glTexCoord2f(s0, t0); glVertex2f(x0, y1);
                
                currentX += bc.xadvance();
            }
        }
        
        glEnd();
        glColor3f(1.0f, 1.0f, 1.0f);
    }
    
    public void cleanup() {
        if (fontTexture != 0) {
            glDeleteTextures(fontTexture);
        }
        if (charData != null) {
            charData.free();
        }
    }
    
    private void renderGeometricText(String text, float x, float y, float r, float g, float b) {
        glDisable(GL_TEXTURE_2D);
        glColor3f(r, g, b);
        
        float charWidth = 12;
        float charHeight = 16;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float charX = x + i * charWidth;
            renderSimpleChar(c, charX, y, charWidth, charHeight);
        }
        
        glColor3f(1.0f, 1.0f, 1.0f);
        glEnable(GL_TEXTURE_2D);
    }
    
    private void renderSimpleChar(char c, float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        
        switch (c) {
            case 'L':
                glVertex2f(x, y); glVertex2f(x + 2, y);
                glVertex2f(x + 2, y + h); glVertex2f(x, y + h);
                glVertex2f(x, y); glVertex2f(x + w - 2, y);
                glVertex2f(x + w - 2, y + 2); glVertex2f(x, y + 2);
                break;
            case 'E':
                glVertex2f(x, y); glVertex2f(x + 2, y);
                glVertex2f(x + 2, y + h); glVertex2f(x, y + h);
                glVertex2f(x, y + h - 2); glVertex2f(x + w - 2, y + h - 2);
                glVertex2f(x + w - 2, y + h); glVertex2f(x, y + h);
                glVertex2f(x, y + h/2 - 1); glVertex2f(x + w - 4, y + h/2 - 1);
                glVertex2f(x + w - 4, y + h/2 + 1); glVertex2f(x, y + h/2 + 1);
                glVertex2f(x, y); glVertex2f(x + w - 2, y);
                glVertex2f(x + w - 2, y + 2); glVertex2f(x, y + 2);
                break;
            case 'V':
                glVertex2f(x, y + h); glVertex2f(x + 2, y + h);
                glVertex2f(x + w/2, y + 2); glVertex2f(x + w/2 - 2, y + 2);
                glVertex2f(x + w/2, y + 2); glVertex2f(x + w/2 + 2, y + 2);
                glVertex2f(x + w, y + h); glVertex2f(x + w - 2, y + h);
                break;
            case '1':
                glVertex2f(x + w/2 - 1, y); glVertex2f(x + w/2 + 1, y);
                glVertex2f(x + w/2 + 1, y + h); glVertex2f(x + w/2 - 1, y + h);
                break;
            case '2':
                glVertex2f(x, y + h - 2); glVertex2f(x + w, y + h - 2);
                glVertex2f(x + w, y + h); glVertex2f(x, y + h);
                glVertex2f(x, y + h/2 - 1); glVertex2f(x + w, y + h/2 - 1);
                glVertex2f(x + w, y + h/2 + 1); glVertex2f(x, y + h/2 + 1);
                glVertex2f(x, y); glVertex2f(x + w, y);
                glVertex2f(x + w, y + 2); glVertex2f(x, y + 2);
                break;
            case '3':
                glVertex2f(x, y + h - 2); glVertex2f(x + w, y + h - 2);
                glVertex2f(x + w, y + h); glVertex2f(x, y + h);
                glVertex2f(x + w/2, y + h/2 - 1); glVertex2f(x + w, y + h/2 - 1);
                glVertex2f(x + w, y + h/2 + 1); glVertex2f(x + w/2, y + h/2 + 1);
                glVertex2f(x, y); glVertex2f(x + w, y);
                glVertex2f(x + w, y + 2); glVertex2f(x, y + 2);
                glVertex2f(x + w - 2, y); glVertex2f(x + w, y);
                glVertex2f(x + w, y + h); glVertex2f(x + w - 2, y + h);
                break;
        }
        
        glEnd();
    }
}
