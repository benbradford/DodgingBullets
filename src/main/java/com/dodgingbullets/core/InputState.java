package com.dodgingbullets.core;

public class InputState {
    public final boolean[] keys;
    public final boolean jumpPressed;
    public final boolean jumpHeld;
    public final boolean mousePressed;
    public final boolean mouseHeld;
    public final double worldMouseX;
    public final double worldMouseY;
    
    public InputState(boolean[] keys, boolean jumpPressed, boolean jumpHeld, 
                     boolean mousePressed, boolean mouseHeld, double worldMouseX, double worldMouseY) {
        this.keys = keys.clone();
        this.jumpPressed = jumpPressed;
        this.jumpHeld = jumpHeld;
        this.mousePressed = mousePressed;
        this.mouseHeld = mouseHeld;
        this.worldMouseX = worldMouseX;
        this.worldMouseY = worldMouseY;
    }
}
