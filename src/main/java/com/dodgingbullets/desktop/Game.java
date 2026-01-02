package com.dodgingbullets.desktop;

import com.dodgingbullets.core.*;
import com.dodgingbullets.gameobjects.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    private long window;
    private Renderer renderer;
    private GameLoop gameLoop;
    private Texture grassTexture;
    private Texture shadowTexture;
    private Texture bulletTexture;
    private Texture shellTexture;
    private Texture brokenTurretTexture;
    private Texture vignetteTexture;
    private Texture foliageTexture;
    private Map<Direction, Texture> turretTextures = new HashMap<>();
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
        
        // Android phone aspect ratio (16:9) landscape - 10% zoom out
        window = glfwCreateWindow(640, 360, "Dodging Bullets", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        setupInputCallbacks();
        
        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            var pWidth = stack.mallocInt(1);
            var pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            var vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            glfwSetWindowPos(window, 
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2);
        }
        
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        
        renderer = new DesktopRenderer();
        renderer.initialize();
        
        loadTextures();
        
        gameLoop = new GameLoop();
        gameLoop.initialize(renderer);
    }
    
    private void setupInputCallbacks() {
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
            
            boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;
            switch (key) {
                case GLFW_KEY_W: keys[0] = pressed; break;
                case GLFW_KEY_S: keys[1] = pressed; break;
                case GLFW_KEY_A: keys[2] = pressed; break;
                case GLFW_KEY_D: keys[3] = pressed; break;
                case GLFW_KEY_J: 
                    jumpPressed = (action == GLFW_PRESS);
                    jumpHeld = (action == GLFW_PRESS || action == GLFW_REPEAT);
                    if (action == GLFW_RELEASE) jumpHeld = false;
                    break;
            }
        });
        
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                mousePressed = true;
            }
        });
        
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = 360 - ypos; // Flip Y coordinate
        });
    }
    
    private void loadTextures() {
        grassTexture = renderer.loadTexture("src/main/resources/textures/grass.png");
        shadowTexture = renderer.loadTexture("src/main/resources/textures/shadow.png");
        bulletTexture = renderer.loadTexture("src/main/resources/textures/bullet.png");
        shellTexture = renderer.loadTexture("src/main/resources/textures/shell.png");
        brokenTurretTexture = renderer.loadTexture("src/main/resources/textures/broken_turret.png");
        vignetteTexture = renderer.loadTexture("src/main/resources/textures/vignette.png");
        foliageTexture = renderer.loadTexture("src/main/resources/textures/foliage.png");
        
        // Load turret textures for all 8 directions
        turretTextures.put(Direction.UP, renderer.loadTexture("src/main/resources/textures/turret_n.png"));
        turretTextures.put(Direction.UP_RIGHT, renderer.loadTexture("src/main/resources/textures/turret_ne.png"));
        turretTextures.put(Direction.RIGHT, renderer.loadTexture("src/main/resources/textures/turret_e.png"));
        turretTextures.put(Direction.DOWN_RIGHT, renderer.loadTexture("src/main/resources/textures/turret_se.png"));
        turretTextures.put(Direction.DOWN, renderer.loadTexture("src/main/resources/textures/turret_s.png"));
        turretTextures.put(Direction.DOWN_LEFT, renderer.loadTexture("src/main/resources/textures/turret_sw.png"));
        turretTextures.put(Direction.LEFT, renderer.loadTexture("src/main/resources/textures/turret_w.png"));
        turretTextures.put(Direction.UP_LEFT, renderer.loadTexture("src/main/resources/textures/turret_nw.png"));
    }
    
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            // Update game logic
            gameLoop.update(keys, jumpPressed, jumpHeld, mousePressed, mouseX, mouseY);
            jumpPressed = false; // Reset jump press after processing
            mousePressed = false; // Reset mouse press after processing
            
            // Render everything
            render();
            
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    
    private void render() {
        renderer.clear();
        
        Player player = gameLoop.getPlayer();
        float cameraX = gameLoop.getCameraX();
        float cameraY = gameLoop.getCameraY();
        
        // Render tiled grass background
        renderTiledBackground();
        
        // Render player shadow
        float shadowSize = 40 - (player.getJumpOffset() * 0.3f);
        renderer.render(shadowTexture, 
            player.getX() - shadowSize/2 - cameraX, player.getY() - shadowSize/2 - 20 - cameraY, shadowSize, shadowSize);
        
        // Render all objects with depth sorting
        renderGameObjects();
        
        // Render bullets
        for (Bullet bullet : gameLoop.getBullets()) {
            renderer.render(bulletTexture, bullet.getX() - 3 - cameraX, bullet.getY() - 3 - cameraY, 6, 6);
        }
        
        // Render shell casings
        for (ShellCasing shell : gameLoop.getShells()) {
            renderer.renderRotatedWithAlpha(shellTexture, shell.getX() - 3 - cameraX, shell.getY() - 1.5f - cameraY, 6, 3, shell.getRotation(), shell.getAlpha());
        }
        
        // Render UI
        renderHealthBar();
        renderAmmoBar();
        
        // Render vignette overlay
        Player p = gameLoop.getPlayer();
        float flashIntensity = p.getDamageFlashIntensity();
        float red = 1.0f + flashIntensity * 0.5f;
        float green = 1.0f - flashIntensity * 0.3f;
        float blue = 1.0f - flashIntensity * 0.3f;
        renderer.renderTextureWithColor(vignetteTexture, 0, 0, 704, 396, red, green, blue, 0.15f);
    }
    
    private void renderGameObjects() {
        Player player = gameLoop.getPlayer();
        float cameraX = gameLoop.getCameraX();
        float cameraY = gameLoop.getCameraY();
        
        // Create list of all GameObjects for depth sorting
        List<GameObject> allGameObjects = new ArrayList<>();
        allGameObjects.addAll(gameLoop.getTurrets());
        allGameObjects.addAll(gameLoop.getFoliages());
        
        // Sort GameObjects by Y position for depth (higher Y renders first/behind)
        allGameObjects.sort((a, b) -> {
            float aY = (a instanceof Renderable) ? ((Renderable) a).getRenderY() : a.getY();
            float bY = (b instanceof Renderable) ? ((Renderable) b).getRenderY() : b.getY();
            return Float.compare(bY, aY); // Reversed for proper depth
        });
        
        // Render player and GameObjects in correct depth order
        boolean playerRendered = false;
        for (GameObject gameObj : allGameObjects) {
            // Render player if we've reached the correct depth
            if (!playerRendered && player.getY() <= gameObj.getY()) {
                Texture currentTexture = player.getCurrentTexture();
                renderer.render(currentTexture, 
                    player.getX() - 32 - cameraX, player.getY() - 32 + player.getJumpOffset() - cameraY, 64, 64);
                playerRendered = true;
            }
            
            // Render GameObject
            if (gameObj instanceof Trackable && gameObj instanceof Damageable) {
                Trackable trackable = (Trackable) gameObj;
                Damageable damageable = (Damageable) gameObj;
                Texture turretTexture = damageable.isDestroyed() ? brokenTurretTexture : 
                                       turretTextures.get(trackable.getFacingDirection());
                renderer.render(turretTexture, gameObj.getX() - 64 - cameraX, gameObj.getY() - 64 - cameraY, 128, 128);
            } else if (gameObj.getClass().getSimpleName().equals("Foliage")) {
                renderer.render(foliageTexture, gameObj.getX() - 25 - cameraX, gameObj.getY() - 25 - cameraY, 50, 50);
            }
        }
        
        // Render player if not rendered yet (in front of all objects)
        if (!playerRendered) {
            Texture currentTexture = player.getCurrentTexture();
            renderer.render(currentTexture, 
                player.getX() - 32 - cameraX, player.getY() - 32 + player.getJumpOffset() - cameraY, 64, 64);
        }
    }
    
    private void renderHealthBar() {
        Player player = gameLoop.getPlayer();
        float cameraX = gameLoop.getCameraX();
        float cameraY = gameLoop.getCameraY();
        
        float healthBarWidth = 40;
        float healthBarHeight = 6;
        float healthBarX = player.getX() - healthBarWidth/2 - cameraX;
        float healthBarY = player.getY() - 45 - cameraY;
        
        float healthPercent = player.getHealth() / 100.0f;
        float red = 1.0f - healthPercent;
        float green = healthPercent;
        
        renderer.renderRectOutline(healthBarX, healthBarY, healthBarWidth, healthBarHeight, 1.0f, 1.0f, 1.0f, 1.0f);
        
        float fillWidth = healthBarWidth * healthPercent;
        if (fillWidth > 0) {
            renderer.renderRect(healthBarX, healthBarY, fillWidth, healthBarHeight, red, green, 0.0f, 1.0f);
        }
    }
    
    private void renderAmmoBar() {
        Player player = gameLoop.getPlayer();
        float cameraX = gameLoop.getCameraX();
        float cameraY = gameLoop.getCameraY();
        
        float ammoBarWidth = 40;
        float ammoBarHeight = 6;
        float ammoBarX = player.getX() - ammoBarWidth/2 - cameraX;
        float ammoBarY = player.getY() - 55 - cameraY;
        
        float ammoPercent = player.getAmmo() / 5.0f;
        
        renderer.renderRectOutline(ammoBarX, ammoBarY, ammoBarWidth, ammoBarHeight, 1.0f, 1.0f, 1.0f, 1.0f);
        
        float fillWidth = ammoBarWidth * ammoPercent;
        if (fillWidth > 0) {
            renderer.renderRect(ammoBarX, ammoBarY, fillWidth, ammoBarHeight, 0.0f, 0.5f, 1.0f, 1.0f);
        }
    }
    
    private void renderTiledBackground() {
        float cameraX = gameLoop.getCameraX();
        float cameraY = gameLoop.getCameraY();
        int tileSize = 64;
        float mapWidth = gameLoop.getMapWidth();
        float mapHeight = gameLoop.getMapHeight();
        
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
