package com.dodgingbullets.core;

public class GameConfig {
    // Map dimensions
    public static final float MAP_WIDTH = 2560;
    public static final float MAP_HEIGHT = 1440;

    // Screen dimensions (10% zoom out)
    public static final float SCREEN_WIDTH = 704;
    public static final float SCREEN_HEIGHT = 396;
    
    // Window dimensions for input scaling
    public static final float WINDOW_WIDTH = 640;
    public static final float WINDOW_HEIGHT = 360;

    // Gameplay constants
    public static final int PLAYER_DAMAGE = 10;
    public static final int ENEMY_DAMAGE = 5;
    public static final int EXPLOSION_DAMAGE = 1;

    // Rendering constants
    public static final int TILE_SIZE = 64;
    public static final float TARGET_FPS = 60.0f;
    public static final float DELTA_TIME = 1.0f / TARGET_FPS;
}
