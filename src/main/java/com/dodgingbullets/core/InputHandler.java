package com.dodgingbullets.core;

public class InputHandler {
    private float cameraX;
    private float cameraY;
    
    public void updateCamera(float cameraX, float cameraY) {
        this.cameraX = cameraX;
        this.cameraY = cameraY;
    }
    
    public InputState processInput(boolean[] keys, boolean jumpPressed, boolean jumpHeld, 
                                 boolean mousePressed, double mouseX, double mouseY) {
        double worldMouseX = 0;
        double worldMouseY = 0;
        
        if (mousePressed) {
            // Scale mouse coordinates from window size to game world size
            double scaledMouseX = mouseX * (GameConfig.SCREEN_WIDTH / GameConfig.WINDOW_WIDTH);
            double scaledMouseY = mouseY * (GameConfig.SCREEN_HEIGHT / GameConfig.WINDOW_HEIGHT);
            
            worldMouseX = scaledMouseX + cameraX;
            worldMouseY = scaledMouseY + cameraY;
        }
        
        return new InputState(keys, jumpPressed, jumpHeld, mousePressed, worldMouseX, worldMouseY);
    }
}
