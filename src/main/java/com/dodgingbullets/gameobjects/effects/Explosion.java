package com.dodgingbullets.gameobjects.effects;

import com.dodgingbullets.gameobjects.*;

public class Explosion extends GameObject implements Renderable, Collidable {
    private static final String[] TEXTURE_NAMES = {
        "explosionanim_1_0.png",
        "explosionanim_1_17.png", 
        "explosionanim_1_34.png",
        "explosionanim_1_51.png",
        "explosionanim_1_68.png",
        "explosionanim_1_85.png",
        "explosionanim_1_102.png",
        "explosionanim_1_119.png",
        "explosionanim_1_136.png"
    };
    
    private static final float FRAME_DURATION = 0.2f;
    private static final float SIZE = 83.2f; // 64 * 1.3 = 30% bigger
    
    private int currentFrame = 0;
    private float frameTimer = 0f;
    
    public Explosion(float x, float y) {
        super(x, y);
    }
    
    @Override
    public void update(float deltaTime) {
        frameTimer += deltaTime;
        
        if (frameTimer >= FRAME_DURATION) {
            frameTimer = 0f;
            currentFrame++;
            
            if (currentFrame >= TEXTURE_NAMES.length) {
                active = false;
            }
        }
    }
    
    @Override
    public void render() {
        // Rendering handled by renderer
    }
    
    @Override
    public float getRenderY() {
        return y + 1000; // Render on top of everything
    }
    
    @Override
    public boolean checkSpriteCollision(float x, float y, float width, float height) {
        return x < this.x + SIZE/2 && x + width > this.x - SIZE/2 && 
               y < this.y + SIZE/2 && y + height > this.y - SIZE/2;
    }
    
    @Override
    public boolean checkMovementCollision(float x, float y, float width, float height) {
        return false; // Explosions don't block movement
    }
    
    public String getCurrentTexture() {
        if (currentFrame >= TEXTURE_NAMES.length) return null;
        return TEXTURE_NAMES[currentFrame];
    }
    
    public float getSize() {
        return SIZE;
    }
}
