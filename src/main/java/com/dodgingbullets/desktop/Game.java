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
    private Map<Direction, Texture> turretTextures = new HashMap<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<ShellCasing> shells = new ArrayList<>();
    private Turret turret;
    private float cameraX = 0;
    private float cameraY = 0;
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
        
        // Load turret textures
        turretTextures.put(Direction.RIGHT, renderer.loadTexture("assets/gunturret_e.png"));
        turretTextures.put(Direction.UP_RIGHT, renderer.loadTexture("assets/gunturret_ne.png"));
        turretTextures.put(Direction.UP, renderer.loadTexture("assets/gunturret_n.png"));
        turretTextures.put(Direction.UP_LEFT, renderer.loadTexture("assets/gunturret_nw.png"));
        turretTextures.put(Direction.LEFT, renderer.loadTexture("assets/gunturret_w.png"));
        turretTextures.put(Direction.DOWN_LEFT, renderer.loadTexture("assets/gunturret_sw.png"));
        turretTextures.put(Direction.DOWN, renderer.loadTexture("assets/gunturret_s.png"));
        turretTextures.put(Direction.DOWN_RIGHT, renderer.loadTexture("assets/gunturret_se.png"));
        
        // Create turret at a fixed position
        turret = new Turret(500, 250);
        
        player = new Player(320, 180); // Center of screen
        player.loadTextures(renderer);
    }
    
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            player.update(keys, jumpPressed, jumpHeld);
            jumpPressed = false; // Reset jump press after processing
            
            // Update camera to follow player (keep player centered)
            cameraX = player.getX() - 320; // Center horizontally (640/2 = 320)
            cameraY = player.getY() - 180; // Center vertically (360/2 = 180)
            
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
                
                float[] gunPos = player.getGunBarrelPosition();
                bullets.add(new Bullet(gunPos[0], gunPos[1], angle));
                // Add shell casing effect
                shells.add(new ShellCasing(player.getX(), player.getY()));
                mousePressed = false;
            }
            
            // Handle turret shooting
            if (turret.shouldShoot()) {
                float[] barrelPos = turret.getBarrelPosition();
                bullets.add(new Bullet(barrelPos[0], barrelPos[1], turret.getFacingDirection()));
            }
            
            // Update bullets
            Iterator<Bullet> bulletIter = bullets.iterator();
            while (bulletIter.hasNext()) {
                Bullet bullet = bulletIter.next();
                bullet.update();
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
            
            // Render map boundaries
            renderMapBoundaries();
            
            // Render smaller shadow (positioned below player, with camera offset)
            float shadowSize = 40 - (player.getJumpOffset() * 0.3f);
            renderer.render(shadowTexture, 
                player.getX() - shadowSize/2 - cameraX, player.getY() - shadowSize/2 - 20 - cameraY, shadowSize, shadowSize);
            
            // Render player (elevated by jump offset, always centered on screen)
            Texture currentTexture = player.getCurrentTexture();
            renderer.render(currentTexture, 
                320 - 32, 180 - 32 + player.getJumpOffset(), 64, 64);
            
            // Render turret (with camera offset)
            Texture turretTexture = turretTextures.get(turret.getFacingDirection());
            renderer.render(turretTexture, turret.getX() - 32 - cameraX, turret.getY() - 32 - cameraY, 64, 64);
            
            // Render bullets (smaller size, with camera offset)
            for (Bullet bullet : bullets) {
                renderer.render(bulletTexture, bullet.getX() - 3 - cameraX, bullet.getY() - 3 - cameraY, 6, 6);
            }
            
            // Render shell casings with rotation and fade (smaller size, with camera offset)
            for (ShellCasing shell : shells) {
                renderer.renderRotatedWithAlpha(shellTexture, shell.getX() - 3 - cameraX, shell.getY() - 1.5f - cameraY, 6, 3, shell.getRotation(), shell.getAlpha());
            }
            
            glfwSwapBuffers(window);
            glfwPollEvents();
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
