package com.dodgingbullets.desktop;

import com.dodgingbullets.core.Player;
import com.dodgingbullets.core.Renderer;
import com.dodgingbullets.core.Texture;
import com.dodgingbullets.core.Bullet;
import com.dodgingbullets.core.ShellCasing;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private List<Bullet> bullets = new ArrayList<>();
    private List<ShellCasing> shells = new ArrayList<>();
    private boolean[] keys = new boolean[4]; // UP, DOWN, LEFT, RIGHT
    private boolean jumpPressed = false;
    private boolean jumpHeld = false;
    private boolean spacePressed = false;
    
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
                case GLFW_KEY_UP: keys[0] = pressed; break;
                case GLFW_KEY_DOWN: keys[1] = pressed; break;
                case GLFW_KEY_LEFT: keys[2] = pressed; break;
                case GLFW_KEY_RIGHT: keys[3] = pressed; break;
                case GLFW_KEY_J: 
                    jumpPressed = (action == GLFW_PRESS);
                    jumpHeld = (action == GLFW_PRESS || action == GLFW_REPEAT);
                    if (action == GLFW_RELEASE) jumpHeld = false;
                    break;
                case GLFW_KEY_SPACE: 
                    spacePressed = (action == GLFW_PRESS);
                    break;
            }
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
        
        player = new Player(320, 180); // Center of screen
        player.loadTextures(renderer);
    }
    
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            player.update(keys, jumpPressed, jumpHeld);
            jumpPressed = false; // Reset jump press after processing
            
            // Handle shooting
            if (spacePressed) {
                float[] gunPos = player.getGunBarrelPosition();
                bullets.add(new Bullet(gunPos[0], gunPos[1], player.getCurrentDirection()));
                // Add shell casing effect
                shells.add(new ShellCasing(player.getX(), player.getY()));
                spacePressed = false;
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
            
            // Render tiled grass background
            renderTiledBackground();
            
            // Render smaller shadow (positioned below player)
            float shadowSize = 40 - (player.getJumpOffset() * 0.3f);
            renderer.render(shadowTexture, 
                player.getX() - shadowSize/2, player.getY() - shadowSize/2 - 20, shadowSize, shadowSize);
            
            // Render player (elevated by jump offset)
            Texture currentTexture = player.getCurrentTexture();
            renderer.render(currentTexture, 
                player.getX() - 32, player.getY() - 32 + player.getJumpOffset(), 64, 64);
            
            // Render bullets (smaller size)
            for (Bullet bullet : bullets) {
                renderer.render(bulletTexture, bullet.getX() - 3, bullet.getY() - 3, 6, 6);
            }
            
            // Render shell casings with rotation and fade (smaller size)
            for (ShellCasing shell : shells) {
                renderer.renderRotatedWithAlpha(shellTexture, shell.getX() - 3, shell.getY() - 1.5f, 6, 3, shell.getRotation(), shell.getAlpha());
            }
            
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    
    private void renderTiledBackground() {
        int tileSize = 64; // Size of each grass tile
        int tilesX = (640 / tileSize) + 1; // Number of tiles horizontally
        int tilesY = (360 / tileSize) + 1; // Number of tiles vertically
        
        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                renderer.render(grassTexture, x * tileSize, y * tileSize, tileSize, tileSize);
            }
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
