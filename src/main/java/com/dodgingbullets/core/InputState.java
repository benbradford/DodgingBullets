package com.dodgingbullets.core;

public class InputState {
    public final boolean[] keys;
    public final boolean jumpPressed;
    public final boolean jumpHeld;
    public final boolean mousePressed;
    public final boolean mouseHeld;
    public final boolean grenadePressed;
    public final boolean running;
    public final boolean qPressed;
    public final double mouseX;
    public final double mouseY;
    public final double worldMouseX;
    public final double worldMouseY;
    
    public InputState(boolean[] keys, boolean jumpPressed, boolean jumpHeld, 
                     boolean mousePressed, boolean mouseHeld, boolean grenadePressed,
                     double mouseX, double mouseY, double worldMouseX, double worldMouseY) {
        this.keys = keys.clone();
        this.jumpPressed = jumpPressed;
        this.jumpHeld = jumpHeld;
        this.mousePressed = mousePressed;
        this.mouseHeld = mouseHeld;
        this.grenadePressed = grenadePressed;
        this.running = keys.length > 4 ? keys[4] : false;
        this.qPressed = false; // Will be set separately
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.worldMouseX = worldMouseX;
        this.worldMouseY = worldMouseY;
    }
    
    public InputState(boolean[] keys, boolean jumpPressed, boolean jumpHeld, 
                     boolean mousePressed, boolean mouseHeld, boolean grenadePressed, boolean qPressed,
                     double mouseX, double mouseY, double worldMouseX, double worldMouseY) {
        this.keys = keys.clone();
        this.jumpPressed = jumpPressed;
        this.jumpHeld = jumpHeld;
        this.mousePressed = mousePressed;
        this.mouseHeld = mouseHeld;
        this.grenadePressed = grenadePressed;
        this.running = keys.length > 4 ? keys[4] : false;
        this.qPressed = qPressed;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.worldMouseX = worldMouseX;
        this.worldMouseY = worldMouseY;
    }
}
