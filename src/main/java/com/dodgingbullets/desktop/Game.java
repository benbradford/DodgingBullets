package com.dodgingbullets.desktop;

import com.dodgingbullets.core.Player;
import com.dodgingbullets.core.Renderer;
import com.dodgingbullets.core.Texture;
import com.dodgingbullets.core.Bullet;
import com.dodgingbullets.core.ShellCasing;
import com.dodgingbullets.core.Turret;
import com.dodgingbullets.core.Direction;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    private long window;
    private Renderer renderer;
    private Player player;
    private Texture grassTexture;
    private Texture shadowTexture;
    private Texture bulletTexture;
    private Texture shellTexture;
    private Texture brokenTurretTexture;
    private Texture vignetteTexture;
    private Map<Direction, Texture> turretTextures = new HashMap<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<ShellCasing> shells = new ArrayList<>();
    private Turret turret;
    private float cameraX = 0;
    private float cameraY = 0;
    private boolean showGrid = false;
    private static final int GRID_SIZE = 16; // Quarter player width (64/4 = 16)
    private boolean[] keys = new boolean[4]; // W, A, S, D
    private boolean jumpPressed = false;
    private boolean jumpHeld = false;
    private boolean mousePressed = false;
    private double mouseX = 0;
    private double mouseY = 0;
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Disable GLFW thread check for Maven exec plugin
        Configuration.GLFW_CHECK_THREAD0.set(false);
        
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        
        // Android phone aspect ratio (16:9) landscape
        window = glfwCreateWindow(640, 360, "Dodging Bullets", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
            
            boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;
            switch (key) {
                case GLFW_KEY_W: keys[0] = pressed; break; // W = forward/up
                case GLFW_KEY_S: keys[1] = pressed; break; // S = backward/down
                case GLFW_KEY_A: keys[2] = pressed; break; // A = left
                case GLFW_KEY_D: keys[3] = pressed; break; // D = right
                case GLFW_KEY_J: 
                    jumpPressed = (action == GLFW_PRESS);
                    jumpHeld = (action == GLFW_PRESS || action == GLFW_REPEAT);
                    if (action == GLFW_RELEASE) jumpHeld = false;
                    break;
                case GLFW_KEY_G:
                    if (action == GLFW_PRESS) showGrid = !showGrid;
                    break;
            }
        });
        
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                mousePressed = (action == GLFW_PRESS);
            }
        });
        
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = 360 - ypos; // Flip Y coordinate to match OpenGL
        });
        
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            glfwSetWindowPos(window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        
        renderer = new DesktopRenderer();
        renderer.initialize();
        
        grassTexture = renderer.loadTexture("assets/grass_realistic_seamless.png");
        shadowTexture = renderer.loadTexture("assets/shadow.png");
        bulletTexture = renderer.loadTexture("assets/bullet.png");
        shellTexture = renderer.loadTexture("assets/shell.png");
        vignetteTexture = renderer.loadTexture("assets/vin.png");
        
        // Load turret textures
        turretTextures.put(Direction.RIGHT, renderer.loadTexture("assets/gunturret_e.png"));
        turretTextures.put(Direction.UP_RIGHT, renderer.loadTexture("assets/gunturret_ne.png"));
        turretTextures.put(Direction.UP, renderer.loadTexture("assets/gunturret_n.png"));
        turretTextures.put(Direction.UP_LEFT, renderer.loadTexture("assets/gunturret_nw.png"));
        turretTextures.put(Direction.LEFT, renderer.loadTexture("assets/gunturret_w.png"));
        turretTextures.put(Direction.DOWN_LEFT, renderer.loadTexture("assets/gunturret_sw.png"));
        turretTextures.put(Direction.DOWN, renderer.loadTexture("assets/gunturret_s.png"));
        turretTextures.put(Direction.DOWN_RIGHT, renderer.loadTexture("assets/gunturret_se.png"));
        
        // Load broken turret texture
        brokenTurretTexture = renderer.loadTexture("assets/gunturret_broken.png");
        
        // Create turret at a fixed position
        turret = new Turret(800, 150);
        
        player = new Player(320, 180); // Center of screen
        player.loadTextures(renderer);
        player.setTurret(turret);
    }
    
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            player.update(keys, jumpPressed, jumpHeld);
            jumpPressed = false; // Reset jump press after processing
            
            // Update camera to follow player with map edge clamping
            float mapWidth = 2560;
            float mapHeight = 1440;
            float screenWidth = 640;
            float screenHeight = 360;
            
            // Calculate desired camera position (player centered)
            float desiredCameraX = player.getX() - screenWidth / 2;
            float desiredCameraY = player.getY() - screenHeight / 2;
            
            // Clamp camera to map boundaries
            cameraX = Math.max(0, Math.min(mapWidth - screenWidth, desiredCameraX));
            cameraY = Math.max(0, Math.min(mapHeight - screenHeight, desiredCameraY));
            
            // Update turret
            turret.update(player.getX(), player.getY());
            
            // Handle player shooting with mouse
            if (mousePressed) {
                // Calculate world position of mouse
                double worldMouseX = mouseX + cameraX;
                double worldMouseY = mouseY + cameraY;
                
                // Calculate direction from player to mouse
                double deltaX = worldMouseX - player.getX();
                double deltaY = worldMouseY - player.getY();
                double angle = Math.atan2(deltaY, deltaX);
                
                Direction shootDirection = Player.calculateDirectionFromAngle(angle);
                player.setShootingDirection(shootDirection);
                
                // Only shoot if player has ammo
                if (player.canShoot()) {
                    player.shoot();
                    float[] gunPos = player.getGunBarrelPosition();
                    bullets.add(new Bullet(gunPos[0], gunPos[1], angle, true)); // Player bullet
                    // Add shell casing effect
                    shells.add(new ShellCasing(player.getX(), player.getY()));
                }
                mousePressed = false;
            }
            
            // Handle turret shooting
            if (!turret.isDestroyed() && turret.canSeePlayer(player.getX(), player.getY()) && 
                turret.canSeePlayerInCurrentDirection(player.getX(), player.getY()) && turret.shouldShoot()) {
                float[] barrelPos = turret.getBarrelPosition();
                // Calculate angle from turret to player
                double deltaX = player.getX() - turret.getX();
                double deltaY = player.getY() - turret.getY();
                double angleToPlayer = Math.atan2(deltaY, deltaX);
                bullets.add(new Bullet(barrelPos[0], barrelPos[1], angleToPlayer, false)); // Enemy bullet
            }
            
            // Update bullets and check collisions
            Iterator<Bullet> bulletIter = bullets.iterator();
            while (bulletIter.hasNext()) {
                Bullet bullet = bulletIter.next();
                bullet.update();
                
                // Check collision with turret (only player bullets can damage turret)
                if (bullet.isPlayerBullet() && turret.isInSpriteHitbox(bullet.getX(), bullet.getY())) {
                    turret.takeDamage();
                    bulletIter.remove(); // Remove bullet on hit
                    continue;
                }
                
                // Check collision with player (only enemy bullets can damage player)
                if (!bullet.isPlayerBullet() && isPlayerHit(bullet.getX(), bullet.getY())) {
                    player.takeDamage(5);
                    bulletIter.remove(); // Remove bullet on hit
                    continue;
                }
                
                if (bullet.isExpired()) {
                    bulletIter.remove();
                }
            }
            
            // Update shell casings
            Iterator<ShellCasing> shellIter = shells.iterator();
            while (shellIter.hasNext()) {
                ShellCasing shell = shellIter.next();
                shell.update();
                if (shell.isExpired()) {
                    shellIter.remove();
                }
            }
            
            renderer.clear();
            
            // Render tiled grass background (with camera offset)
            renderTiledBackground();
            
            // Render smaller shadow (positioned below player, with camera offset)
            float shadowSize = 40 - (player.getJumpOffset() * 0.3f);
            renderer.render(shadowTexture, 
                player.getX() - shadowSize/2 - cameraX, player.getY() - shadowSize/2 - 20 - cameraY, shadowSize, shadowSize);
            
            // Depth-sorted rendering: render objects from top to bottom (higher Y to lower Y)
            if (player.getY() < turret.getY()) {
                // Player is below turret, render turret first (behind)
                Texture turretTexture = turret.isDestroyed() ? brokenTurretTexture : turretTextures.get(turret.getFacingDirection());
                renderer.render(turretTexture, turret.getX() - 64 - cameraX, turret.getY() - 64 - cameraY, 128, 128);
                
                Texture currentTexture = player.getCurrentTexture();
                renderer.render(currentTexture, 
                    player.getX() - 32 - cameraX, player.getY() - 32 + player.getJumpOffset() - cameraY, 64, 64);
            } else {
                // Turret is below player, render player first (behind)
                Texture currentTexture = player.getCurrentTexture();
                renderer.render(currentTexture, 
                    player.getX() - 32 - cameraX, player.getY() - 32 + player.getJumpOffset() - cameraY, 64, 64);
                
                Texture turretTexture = turret.isDestroyed() ? brokenTurretTexture : turretTextures.get(turret.getFacingDirection());
                renderer.render(turretTexture, turret.getX() - 64 - cameraX, turret.getY() - 64 - cameraY, 128, 128);
            }
            
            // Render bullets (smaller size, with camera offset)
            for (Bullet bullet : bullets) {
                renderer.render(bulletTexture, bullet.getX() - 3 - cameraX, bullet.getY() - 3 - cameraY, 6, 6);
            }
            
            // Render shell casings with rotation and fade (smaller size, with camera offset)
            for (ShellCasing shell : shells) {
                renderer.renderRotatedWithAlpha(shellTexture, shell.getX() - 3 - cameraX, shell.getY() - 1.5f - cameraY, 6, 3, shell.getRotation(), shell.getAlpha());
            }
            
            // Render health bar
            renderHealthBar();
            
            // Render ammo bar
            renderAmmoBar();
            
            // Render grid overlay last (always on top)
            renderGrid();
            
            // Render vignette overlay (full screen with reduced opacity)
            // Render vignette overlay (full screen with damage flash effect)
            float flashIntensity = player.getDamageFlashIntensity();
            float red = 1.0f + flashIntensity * 0.5f; // Add red tint when damaged
            float green = 1.0f - flashIntensity * 0.3f; // Reduce green when damaged
            float blue = 1.0f - flashIntensity * 0.3f; // Reduce blue when damaged
            renderer.renderTextureWithColor(vignetteTexture, 0, 0, 640, 360, red, green, blue, 0.15f);
            
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    
    private boolean isPlayerHit(float bulletX, float bulletY) {
        // Player sprite hitbox (12 pixels wide, full height)
        return bulletX >= player.getX() - 6 && bulletX <= player.getX() + 6 && 
               bulletY >= player.getY() - 32 && bulletY <= player.getY() + 32;
    }
    
    private void renderHealthBar() {
        float healthBarWidth = 40;
        float healthBarHeight = 6;
        float healthBarX = player.getX() - healthBarWidth/2 - cameraX;
        float healthBarY = player.getY() - 45 - cameraY; // Above player
        
        // Calculate health percentage and color
        float healthPercent = player.getHealth() / 100.0f;
        float red = 1.0f - healthPercent;   // More red as health decreases
        float green = healthPercent;        // More green as health increases
        
        // Render white outline (always visible)
        renderer.renderRectOutline(healthBarX, healthBarY, healthBarWidth, healthBarHeight, 1.0f, 1.0f, 1.0f, 1.0f);
        
        // Render health fill (from left to right based on health percentage)
        float fillWidth = healthBarWidth * healthPercent;
        if (fillWidth > 0) {
            renderer.renderRect(healthBarX, healthBarY, fillWidth, healthBarHeight, red, green, 0.0f, 1.0f);
        }
    }
    
    private void renderAmmoBar() {
        float ammoBarWidth = 40;
        float ammoBarHeight = 6;
        float ammoBarX = player.getX() - ammoBarWidth/2 - cameraX;
        float ammoBarY = player.getY() - 55 - cameraY; // Below health bar
        
        // Calculate ammo percentage
        float ammoPercent = player.getAmmo() / 5.0f;
        
        // Render white outline (always visible)
        renderer.renderRectOutline(ammoBarX, ammoBarY, ammoBarWidth, ammoBarHeight, 1.0f, 1.0f, 1.0f, 1.0f);
        
        // Render ammo fill (blue color, from left to right based on ammo percentage)
        float fillWidth = ammoBarWidth * ammoPercent;
        if (fillWidth > 0) {
            renderer.renderRect(ammoBarX, ammoBarY, fillWidth, ammoBarHeight, 0.0f, 0.5f, 1.0f, 1.0f); // Blue color
        }
    }
    
    private void renderTiledBackground() {
        int tileSize = 64; // Size of each grass tile
        float mapWidth = 2560;
        float mapHeight = 1440;
        
        // Calculate which tiles are visible based on camera position
        int startTileX = Math.max(0, (int)(cameraX / tileSize) - 1);
        int startTileY = Math.max(0, (int)(cameraY / tileSize) - 1);
        int endTileX = Math.min((int)(mapWidth / tileSize), startTileX + (640 / tileSize) + 3);
        int endTileY = Math.min((int)(mapHeight / tileSize), startTileY + (360 / tileSize) + 3);
        
        for (int x = startTileX; x < endTileX; x++) {
            for (int y = startTileY; y < endTileY; y++) {
                float worldX = x * tileSize;
                float worldY = y * tileSize;
                renderer.render(grassTexture, worldX - cameraX, worldY - cameraY, tileSize, tileSize);
            }
        }
    }
    
    private void renderMapBoundaries() {
        float mapWidth = 2560;
        float mapHeight = 1440;
        
        // Only render boundaries if they're visible on screen
        // Top boundary
        if (cameraY <= 10) {
            renderer.render(bulletTexture, 0 - cameraX, 0 - cameraY, mapWidth, 4);
        }
        // Bottom boundary  
        if (cameraY + 360 >= mapHeight - 10) {
            renderer.render(bulletTexture, 0 - cameraX, mapHeight - 4 - cameraY, mapWidth, 4);
        }
        // Left boundary
        if (cameraX <= 10) {
            renderer.render(bulletTexture, 0 - cameraX, 0 - cameraY, 4, mapHeight);
        }
        // Right boundary
        if (cameraX + 640 >= mapWidth - 10) {
            renderer.render(bulletTexture, mapWidth - 4 - cameraX, 0 - cameraY, 4, mapHeight);
        }
    }
    
    private void renderGrid() {
        if (!showGrid) return;
        
        float mapWidth = 2560;
        float mapHeight = 1440;
        
        // Calculate visible grid range with proper bounds
        int startGridX = (int)Math.floor(cameraX / GRID_SIZE);
        int startGridY = (int)Math.floor(cameraY / GRID_SIZE);
        int endGridX = (int)Math.ceil((cameraX + 640) / GRID_SIZE);
        int endGridY = (int)Math.ceil((cameraY + 360) / GRID_SIZE);
        
        // Render vertical grid lines (thicker)
        for (int x = startGridX; x <= endGridX; x++) {
            float worldX = x * GRID_SIZE;
            if (worldX >= 0 && worldX <= mapWidth) {
                renderer.render(bulletTexture, worldX - cameraX, 0 - cameraY, 2, mapHeight);
            }
        }
        
        // Render horizontal grid lines (thicker)
        for (int y = startGridY; y <= endGridY; y++) {
            float worldY = y * GRID_SIZE;
            if (worldY >= 0 && worldY <= mapHeight) {
                renderer.render(bulletTexture, 0 - cameraX, worldY - cameraY, mapWidth, 2);
            }
        }
        
        // Highlight player's sprite hitbox cells (yellow)
        int[] playerCells = getPlayerGridCells();
        int leftGrid = playerCells[0];
        int rightGrid = playerCells[1];
        int bottomGrid = playerCells[2];
        int topGrid = playerCells[3];
        
        for (int x = leftGrid; x <= rightGrid; x++) {
            for (int y = bottomGrid; y <= topGrid; y++) {
                float cellWorldX = gridToWorld(x);
                float cellWorldY = gridToWorld(y);
                // Render semi-transparent yellow highlight for sprite hitbox
                renderer.renderRotatedWithAlpha(shellTexture, 
                    cellWorldX - cameraX, cellWorldY - cameraY, 
                    GRID_SIZE, GRID_SIZE, 0, 0.3f);
            }
        }
        
        // Highlight player's movement hitbox cells (blue) - rendered on top
        int[] movementCells = getPlayerMovementCells();
        int leftMovement = movementCells[0];
        int rightMovement = movementCells[1];
        int bottomMovement = movementCells[2];
        int topMovement = movementCells[3];
        
        for (int x = leftMovement; x <= rightMovement; x++) {
            for (int y = bottomMovement; y <= topMovement; y++) {
                float cellWorldX = gridToWorld(x);
                float cellWorldY = gridToWorld(y);
                // Render semi-transparent blue highlight for movement hitbox (using bullet texture as blue)
                renderer.renderRotatedWithAlpha(bulletTexture, 
                    cellWorldX - cameraX, cellWorldY - cameraY, 
                    GRID_SIZE, GRID_SIZE, 0, 0.4f);
            }
        }
        
        // Highlight turret's sprite hitbox cells (yellow)
        int[] turretCells = getTurretGridCells();
        int leftTurret = turretCells[0];
        int rightTurret = turretCells[1];
        int bottomTurret = turretCells[2];
        int topTurret = turretCells[3];
        
        for (int x = leftTurret; x <= rightTurret; x++) {
            for (int y = bottomTurret; y <= topTurret; y++) {
                float cellWorldX = gridToWorld(x);
                float cellWorldY = gridToWorld(y);
                // Render semi-transparent yellow highlight for turret sprite hitbox
                renderer.renderRotatedWithAlpha(shellTexture, 
                    cellWorldX - cameraX, cellWorldY - cameraY, 
                    GRID_SIZE, GRID_SIZE, 0, 0.2f);
            }
        }
        
        // Highlight turret's movement hitbox cells (blue) - rendered on top
        int[] turretMovementCells = getTurretMovementCells();
        int leftTurretMovement = turretMovementCells[0];
        int rightTurretMovement = turretMovementCells[1];
        int bottomTurretMovement = turretMovementCells[2];
        int topTurretMovement = turretMovementCells[3];
        
        for (int x = leftTurretMovement; x <= rightTurretMovement; x++) {
            for (int y = bottomTurretMovement; y <= topTurretMovement; y++) {
                float cellWorldX = gridToWorld(x);
                float cellWorldY = gridToWorld(y);
                // Render semi-transparent blue highlight for turret movement hitbox
                renderer.renderRotatedWithAlpha(bulletTexture, 
                    cellWorldX - cameraX, cellWorldY - cameraY, 
                    GRID_SIZE, GRID_SIZE, 0, 0.3f);
            }
        }
    }
    
    // Grid utility methods
    public static int worldToGrid(float worldPos) {
        return (int)(worldPos / GRID_SIZE);
    }
    
    public static float gridToWorld(int gridPos) {
        return gridPos * GRID_SIZE;
    }
    
    public static float snapToGrid(float worldPos) {
        return gridToWorld(worldToGrid(worldPos));
    }
    
    // Get grid cells occupied by an object (sprite hitbox)
    public static int[] getOccupiedCells(float x, float y, float width, float height) {
        int leftGrid = worldToGrid(x - 6);   // 12 pixels wide total (6 each side)
        int rightGrid = worldToGrid(x + 6);  // 12 pixels wide total (6 each side)
        int topGrid = worldToGrid(y + height/2 - 1);
        int bottomGrid = worldToGrid(y - height/2);
        
        return new int[]{leftGrid, rightGrid, bottomGrid, topGrid};
    }
    
    // Get movement hitbox cells (feet area - bottom 1/5th of sprite)
    public static int[] getMovementHitboxCells(float x, float y, float width, float height) {
        float feetHeight = height / 5; // Bottom 1/5th
        int leftGrid = worldToGrid(x - 6);   // Same 12 pixel width
        int rightGrid = worldToGrid(x + 6);  // Same 12 pixel width
        int topGrid = worldToGrid(y - height/2 + feetHeight - 1);
        int bottomGrid = worldToGrid(y - height/2);
        
        return new int[]{leftGrid, rightGrid, bottomGrid, topGrid};
    }
    
    // Get player's current grid cells (sprite hitbox)
    public int[] getPlayerGridCells() {
        return getOccupiedCells(player.getX(), player.getY(), 64, 64);
    }
    
    // Get player's movement hitbox cells
    public int[] getPlayerMovementCells() {
        return getMovementHitboxCells(player.getX(), player.getY(), 64, 64);
    }
    
    // Get turret's sprite hitbox cells (actual sprite boundaries - about 4x4 cells)
    public int[] getTurretGridCells() {
        int leftGrid = worldToGrid(turret.getX() - 32);   // 64 pixel width (4 cells)
        int rightGrid = worldToGrid(turret.getX() + 32);  // 64 pixel width (4 cells)
        int topGrid = worldToGrid(turret.getY() + 32);    // 64 pixel height (4 cells)
        int bottomGrid = worldToGrid(turret.getY() - 32); // 64 pixel height (4 cells)
        
        return new int[]{leftGrid, rightGrid, bottomGrid, topGrid};
    }
    
    // Get turret's movement hitbox cells (lower half of actual sprite)
    public int[] getTurretMovementCells() {
        int leftGrid = worldToGrid(turret.getX() - 32);   // Same width as sprite
        int rightGrid = worldToGrid(turret.getX() + 32);  // Same width as sprite
        int topGrid = worldToGrid(turret.getY());         // Center line
        int bottomGrid = worldToGrid(turret.getY() - 32); // Bottom of sprite
        
        return new int[]{leftGrid, rightGrid, bottomGrid, topGrid};
    }
    
    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    
    public static void main(String[] args) {
        new Game().run();
    }
}
