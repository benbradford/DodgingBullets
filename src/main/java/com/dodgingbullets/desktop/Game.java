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
    private GameRenderer gameRenderer;
    private LevelSelectScreen levelSelectScreen;
    private boolean inLevelSelect = true;
    private Texture grassTexture;
    private Texture shadowTexture;
    private Texture bulletTexture;
    private Texture shellTexture;
    private Texture brokenTurretTexture;
    private Texture vignetteTexture;
    private Texture foliageTexture;
    private Texture palmTreesTexture;
    private Texture palmTreesGroupTexture;
    private Texture ammoFullTexture;
    private Texture ammoEmptyTexture;
    private Texture grenadeTexture;
    private Map<Direction, Texture> turretTextures = new HashMap<>();
    private Map<String, Texture> explosionTextures = new HashMap<>();
    private Map<String, Texture> foliageTextures = new HashMap<>();
    private boolean[] keys = new boolean[5]; // W, A, S, D, R
    private boolean jumpPressed = false;
    private boolean jumpHeld = false;
    private boolean mousePressed = false;
    private boolean mouseHeld = false;
    private boolean grenadePressed = false;
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
        
        levelSelectScreen = new LevelSelectScreen();
        
        loadTextures();
        
        gameRenderer = new GameRenderer();
        gameRenderer.setTextures(turretTextures, grassTexture, shadowTexture, bulletTexture, 
                                 shellTexture, brokenTurretTexture, vignetteTexture, foliageTextures,
                                 explosionTextures, ammoFullTexture, ammoEmptyTexture, grenadeTexture);
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
                case GLFW_KEY_R: keys[4] = pressed; break;
                case GLFW_KEY_J: 
                    jumpPressed = (action == GLFW_PRESS);
                    jumpHeld = (action == GLFW_PRESS || action == GLFW_REPEAT);
                    if (action == GLFW_RELEASE) jumpHeld = false;
                    break;
                case GLFW_KEY_G:
                    grenadePressed = (action == GLFW_PRESS);
                    break;
            }
        });
        
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    mousePressed = true;
                    mouseHeld = true;
                    
                    // Handle level select clicks
                    if (inLevelSelect) {
                        levelSelectScreen.handleClick(mouseX, mouseY);
                        if (levelSelectScreen.isLevelSelected()) {
                            // Initialize game with selected level
                            GameObjectFactory.loadLevel(levelSelectScreen.getChosenLevel());
                            
                            // Load the background texture for this level
                            String backgroundTexture = GameObjectFactory.getBackgroundTexture();
                            grassTexture = renderer.loadTexture("assets/" + backgroundTexture);
                            
                            // Update renderer with new background
                            gameRenderer.setTextures(turretTextures, grassTexture, shadowTexture, bulletTexture, 
                                                     shellTexture, brokenTurretTexture, vignetteTexture, foliageTextures,
                                                     explosionTextures, ammoFullTexture, ammoEmptyTexture, grenadeTexture);
                            
                            gameLoop = new GameLoop();
                            gameLoop.initialize(renderer);
                            inLevelSelect = false;
                        }
                    }
                } else if (action == GLFW_RELEASE) {
                    mouseHeld = false;
                }
            }
        });
        
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = 360 - ypos; // Flip Y coordinate
        });
    }
    
    private void loadTextures() {
        grassTexture = renderer.loadTexture("assets/vibrant_random_grass.png");
        shadowTexture = renderer.loadTexture("src/main/resources/textures/shadow.png");
        bulletTexture = renderer.loadTexture("src/main/resources/textures/bullet.png");
        shellTexture = renderer.loadTexture("src/main/resources/textures/shell.png");
        brokenTurretTexture = renderer.loadTexture("src/main/resources/textures/broken_turret.png");
        vignetteTexture = renderer.loadTexture("src/main/resources/textures/vignette.png");
        foliageTexture = renderer.loadTexture("src/main/resources/textures/foliage.png");
        palmTreesTexture = renderer.loadTexture("assets/palm_trees01.png");
        palmTreesGroupTexture = renderer.loadTexture("assets/palm_trees_group.png");
        ammoFullTexture = renderer.loadTexture("assets/ammocratefull.png");
        ammoEmptyTexture = renderer.loadTexture("assets/ammocrateempty.png");
        grenadeTexture = renderer.loadTexture("assets/grenade2_alpha.png");
        
        // Load turret textures for all 8 directions
        turretTextures.put(Direction.UP, renderer.loadTexture("src/main/resources/textures/turret_n.png"));
        turretTextures.put(Direction.UP_RIGHT, renderer.loadTexture("src/main/resources/textures/turret_ne.png"));
        turretTextures.put(Direction.RIGHT, renderer.loadTexture("src/main/resources/textures/turret_e.png"));
        turretTextures.put(Direction.DOWN_RIGHT, renderer.loadTexture("src/main/resources/textures/turret_se.png"));
        turretTextures.put(Direction.DOWN, renderer.loadTexture("src/main/resources/textures/turret_s.png"));
        turretTextures.put(Direction.DOWN_LEFT, renderer.loadTexture("src/main/resources/textures/turret_sw.png"));
        turretTextures.put(Direction.LEFT, renderer.loadTexture("src/main/resources/textures/turret_w.png"));
        turretTextures.put(Direction.UP_LEFT, renderer.loadTexture("src/main/resources/textures/turret_nw.png"));
        
        // Load explosion textures
        explosionTextures.put("explosionanim_1_0.png", renderer.loadTexture("assets/explosionanim_1_0.png"));
        explosionTextures.put("explosionanim_1_17.png", renderer.loadTexture("assets/explosionanim_1_17.png"));
        explosionTextures.put("explosionanim_1_34.png", renderer.loadTexture("assets/explosionanim_1_34.png"));
        explosionTextures.put("explosionanim_1_51.png", renderer.loadTexture("assets/explosionanim_1_51.png"));
        explosionTextures.put("explosionanim_1_68.png", renderer.loadTexture("assets/explosionanim_1_68.png"));
        explosionTextures.put("explosionanim_1_85.png", renderer.loadTexture("assets/explosionanim_1_85.png"));
        explosionTextures.put("explosionanim_1_102.png", renderer.loadTexture("assets/explosionanim_1_102.png"));
        explosionTextures.put("explosionanim_1_119.png", renderer.loadTexture("assets/explosionanim_1_119.png"));
        explosionTextures.put("explosionanim_1_136.png", renderer.loadTexture("assets/explosionanim_1_136.png"));
        
        // Load foliage textures
        foliageTextures.put("foliage", foliageTexture);
        foliageTextures.put("palm_trees", palmTreesTexture);
        foliageTextures.put("palm_trees_group", palmTreesGroupTexture);
    }
    
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            if (inLevelSelect) {
                // Render level select screen
                renderLevelSelect();
            } else {
                // Update game logic
                gameLoop.update(keys, jumpPressed, jumpHeld, mousePressed, mouseHeld, grenadePressed, mouseX, mouseY);
                jumpPressed = false; // Reset jump press after processing
                mousePressed = false; // Reset mouse press after processing
                grenadePressed = false; // Reset grenade press after processing
                
                // Render everything
                gameRenderer.render(renderer, gameLoop);
            }
            
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    
    private void renderLevelSelect() {
        renderer.clear();
        
        // Render level buttons
        List<String> levels = levelSelectScreen.getAvailableLevels();
        float buttonWidth = 200;
        float buttonHeight = 40;
        float startX = (GameConfig.SCREEN_WIDTH - buttonWidth) / 2;
        float startY = 100;
        
        for (int i = 0; i < levels.size(); i++) {
            float buttonY = startY + i * 60;
            
            // Render button background
            renderer.renderRect(startX, buttonY, buttonWidth, buttonHeight, 0.3f, 0.3f, 0.3f, 0.8f);
            
            // Render button outline
            renderer.renderRectOutline(startX, buttonY, buttonWidth, buttonHeight, 1.0f, 1.0f, 1.0f, 1.0f);
            
            // Render level text
            String levelName = levels.get(i).replace(".json", "").toUpperCase();
            renderer.renderText(levelName, startX + 10, buttonY + 15, 1.0f, 1.0f, 1.0f);
        }
        
        renderer.present();
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
