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
    private StateManager stateManager;
    private GameLoop gameLoop;
    private GameRenderer gameRenderer;
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
    private Map<String, Texture> bearTextures = new HashMap<>();
    private Map<String, Texture> throwerTextures = new HashMap<>();
    private Texture petrolBombTexture;
    private boolean[] keys = new boolean[5]; // W, A, S, D, R
    private boolean jumpPressed = false;
    private boolean jumpHeld = false;
    private boolean spacePressed = false;
    private boolean spaceHeld = false;
    private boolean mousePressed = false;
    private boolean mouseHeld = false;
    private boolean grenadePressed = false;
    private boolean qPressed = false;
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

        gameRenderer = new GameRenderer();
        gameRenderer.setTextures(turretTextures, grassTexture, shadowTexture, bulletTexture, 
                                 shellTexture, brokenTurretTexture, vignetteTexture, foliageTextures,
                                 explosionTextures, ammoFullTexture, ammoEmptyTexture, grenadeTexture, 
                                 bearTextures, throwerTextures, petrolBombTexture);
        
        // Initialize state machine
        stateManager = new StateManager();
        gameLoop = new GameLoop();
        
        // Create background updater
        Runnable backgroundUpdater = () -> {
            String backgroundTexture = GameObjectFactory.getBackgroundTexture();
            grassTexture = renderer.loadTexture("assets/" + backgroundTexture);
            gameRenderer.setTextures(turretTextures, grassTexture, shadowTexture, bulletTexture, 
                                   shellTexture, brokenTurretTexture, vignetteTexture, foliageTextures,
                                   explosionTextures, ammoFullTexture, ammoEmptyTexture, grenadeTexture, 
                                   bearTextures, throwerTextures, petrolBombTexture);
        };
        
        // Create states - we'll set the circular reference after
        GamePlayState gamePlayState = new GamePlayState(gameLoop, gameRenderer, renderer, stateManager, null);
        LevelSelectState levelSelectState = new LevelSelectState(stateManager, gamePlayState, renderer, backgroundUpdater);
        
        // Now update the GamePlayState with the correct LevelSelectState reference
        gamePlayState.setLevelSelectState(levelSelectState);
        
        stateManager.setState(levelSelectState);
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
                case GLFW_KEY_Q: 
                    qPressed = (action == GLFW_PRESS);
                    break;
                case GLFW_KEY_J: 
                    jumpPressed = (action == GLFW_PRESS);
                    jumpHeld = (action == GLFW_PRESS || action == GLFW_REPEAT);
                    if (action == GLFW_RELEASE) jumpHeld = false;
                    break;
                case GLFW_KEY_SPACE:
                    spacePressed = (action == GLFW_PRESS);
                    spaceHeld = (action == GLFW_PRESS || action == GLFW_REPEAT);
                    if (action == GLFW_RELEASE) spaceHeld = false;
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
                } else if (action == GLFW_RELEASE) {
                    mouseHeld = false;
                }
            }
        });
        
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            mouseX = xpos * (GameConfig.SCREEN_WIDTH / GameConfig.WINDOW_WIDTH);
            mouseY = (360 - ypos) * (GameConfig.SCREEN_HEIGHT / GameConfig.WINDOW_HEIGHT);
        });
    }
    
    private void loadTextures() {
        grassTexture = renderer.loadTexture("assets/vibrant_random_grass.png");
        shadowTexture = renderer.loadTexture("assets/shadow.png");
        bulletTexture = renderer.loadTexture("assets/bullet.png");
        shellTexture = renderer.loadTexture("assets/shell.png");
        brokenTurretTexture = renderer.loadTexture("assets/gunturret_broken.png");
        vignetteTexture = renderer.loadTexture("assets/vignette.png");
        foliageTexture = renderer.loadTexture("assets/foliage01.png");
        palmTreesTexture = renderer.loadTexture("assets/palm_trees01.png");
        palmTreesGroupTexture = renderer.loadTexture("assets/palm_trees_group.png");
        Texture palmTreesGroupLongTexture = renderer.loadTexture("assets/palm_trees_group_long.png");
        Texture palmTreesGroupVerticalTexture = renderer.loadTexture("assets/palm_trees_group_vertical.png");
        Texture palmTreesGroupVerticalLongTexture = renderer.loadTexture("assets/palm_trees_group_vertical_long.png");
        ammoFullTexture = renderer.loadTexture("assets/ammocratefull.png");
        ammoEmptyTexture = renderer.loadTexture("assets/ammocrateempty.png");
        grenadeTexture = renderer.loadTexture("assets/grenade2_alpha.png");
        
        // Load turret textures for all 8 directions
        turretTextures.put(Direction.UP, renderer.loadTexture("assets/gunturret_n.png"));
        turretTextures.put(Direction.UP_RIGHT, renderer.loadTexture("assets/gunturret_ne.png"));
        turretTextures.put(Direction.RIGHT, renderer.loadTexture("assets/gunturret_e.png"));
        turretTextures.put(Direction.DOWN_RIGHT, renderer.loadTexture("assets/gunturret_se.png"));
        turretTextures.put(Direction.DOWN, renderer.loadTexture("assets/gunturret_s.png"));
        turretTextures.put(Direction.DOWN_LEFT, renderer.loadTexture("assets/gunturret_sw.png"));
        turretTextures.put(Direction.LEFT, renderer.loadTexture("assets/gunturret_w.png"));
        turretTextures.put(Direction.UP_LEFT, renderer.loadTexture("assets/gunturret_nw.png"));
        
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
        foliageTextures.put("palm_trees_group_long", palmTreesGroupLongTexture);
        foliageTextures.put("palm_trees_group_vertical", palmTreesGroupVerticalTexture);
        foliageTextures.put("palm_trees_group_vertical_long", palmTreesGroupVerticalLongTexture);
        
        // Load bear textures
        loadBearTextures();
        
        // Load thrower textures
        loadThrowerTextures();
        
        // Load petrol bomb texture
        petrolBombTexture = renderer.loadTexture("assets/petrol_bomb.png");
    }
    
    private void loadBearTextures() {
        // Load idle animations
        for (int i = 0; i <= 9; i++) {
            bearTextures.put("bear_idle_east_" + String.format("%03d", i), 
                renderer.loadTexture("assets/bear/animations/idle/east/frame_" + String.format("%03d", i) + ".png"));
            bearTextures.put("bear_idle_west_" + String.format("%03d", i), 
                renderer.loadTexture("assets/bear/animations/idle/west/frame_" + String.format("%03d", i) + ".png"));
        }
        
        // Load waking up animations
        for (int i = 0; i <= 6; i++) {
            bearTextures.put("bear_wakingUp_east_" + String.format("%03d", i), 
                renderer.loadTexture("assets/bear/animations/wakingUp/east/frame_" + String.format("%03d", i) + ".png"));
            bearTextures.put("bear_wakingUp_west_" + String.format("%03d", i), 
                renderer.loadTexture("assets/bear/animations/wakingUp/west/frame_" + String.format("%03d", i) + ".png"));
        }
        
        // Load running animations for all 8 directions
        String[] directions = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
        for (String dir : directions) {
            for (int i = 0; i <= 3; i++) {
                bearTextures.put("bear_running_" + dir + "_" + String.format("%03d", i), 
                    renderer.loadTexture("assets/bear/animations/running/" + dir + "/frame_" + String.format("%03d", i) + ".png"));
            }
        }
        
        // Load hit animations for all 8 directions
        for (String dir : directions) {
            for (int i = 0; i <= 11; i++) {
                bearTextures.put("bear_hit_" + dir + "_" + String.format("%03d", i), 
                    renderer.loadTexture("assets/bear/animations/hit/" + dir + "/frame_" + String.format("%03d", i) + ".png"));
            }
        }
    }
    
    private void loadThrowerTextures() {
        // Load rotation textures (idle, hit, dying states)
        String[] directions = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
        for (String dir : directions) {
            String path = "assets/thrower/rotations/" + dir + ".png";
            try {
                throwerTextures.put("thrower_rotation_" + dir, renderer.loadTexture(path));
            } catch (Exception e) {
                System.err.println("Failed to load texture: " + path);
                throw e;
            }
        }
        
        // Load walking animations for all 8 directions (4 frames each)
        for (String dir : directions) {
            for (int i = 0; i <= 3; i++) {
                String path = "assets/thrower/animations/walking/" + dir + "/frame_" + String.format("%03d", i) + ".png";
                try {
                    throwerTextures.put("thrower_walking_" + dir + "_" + String.format("%03d", i), renderer.loadTexture(path));
                } catch (Exception e) {
                    System.err.println("Failed to load texture: " + path);
                    throw e;
                }
            }
        }
        
        // Load throwing animations for all 8 directions (6 frames each)
        for (String dir : directions) {
            for (int i = 0; i <= 6; i++) {
                String path = "assets/thrower/animations/throw/" + dir + "/frame_" + String.format("%03d", i) + ".png";
                try {
                    throwerTextures.put("thrower_throw_" + dir + "_" + String.format("%03d", i), renderer.loadTexture(path));
                } catch (Exception e) {
                    System.err.println("Failed to load texture: " + path);
                    throw e;
                }
            }
        }
    }
    
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            // Create input state
            double worldMouseX = mouseX + (gameLoop != null ? gameLoop.getCamera().x() : 0) - GameConfig.SCREEN_WIDTH / 2.0;
            double worldMouseY = mouseY + (gameLoop != null ? gameLoop.getCamera().y() : 0) - GameConfig.SCREEN_HEIGHT / 2.0;
            
            InputState inputState = new InputState(keys, jumpPressed, jumpHeld, mousePressed, mouseHeld, 
                                                  grenadePressed, spacePressed, spaceHeld, qPressed, mouseX, mouseY, worldMouseX, worldMouseY);
            
            // Update state machine
            stateManager.update(0.016f, inputState);
            stateManager.render(renderer);
            
            // Reset single-frame inputs
            jumpPressed = false;
            mousePressed = false;
            grenadePressed = false;
            qPressed = false;
            
            glfwSwapBuffers(window);
            glfwPollEvents();
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
