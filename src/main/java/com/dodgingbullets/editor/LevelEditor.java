package com.dodgingbullets.editor;

import com.dodgingbullets.core.GameConfig;
import com.dodgingbullets.desktop.DesktopRenderer;
import com.dodgingbullets.editor.states.EditorLevelSelectState;
import com.dodgingbullets.editor.states.EditorStateManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class LevelEditor {
    private long window;
    private DesktopRenderer renderer;
    private EditorStateManager stateManager;
    private EditorInputState inputState;

    public static void main(String[] args) {
        new LevelEditor().run();
    }

    public void run() {
        System.out.println("Starting Level Editor...");
        init();
        System.out.println("Initialization complete, starting main loop...");
        loop();
        cleanup();
        System.out.println("Level Editor shutdown complete.");
    }

    private void init() {
        System.out.println("Initializing GLFW...");
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        System.out.println("Creating window...");
        window = glfwCreateWindow((int)GameConfig.WINDOW_WIDTH, (int)GameConfig.WINDOW_HEIGHT, "DodgingBullets Level Editor", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        setupInputCallbacks();

        // Position window 400 pixels from left edge
        glfwSetWindowPos(window, 400, 100);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        System.out.println("Initializing OpenGL...");
        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        System.out.println("Initializing renderer...");
        renderer = new DesktopRenderer();
        renderer.initialize();
        
        inputState = new EditorInputState();
        stateManager = new EditorStateManager();
        
        System.out.println("Creating initial state...");
        EditorLevelSelectState levelSelectState = new EditorLevelSelectState(stateManager, renderer);
        stateManager.setState(levelSelectState);
        System.out.println("Initialization complete!");
    }

    private void setupInputCallbacks() {
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            inputState.mouseX = (float)(xpos * (GameConfig.SCREEN_WIDTH / (double)GameConfig.WINDOW_WIDTH));
            inputState.mouseY = (float)((GameConfig.WINDOW_HEIGHT - ypos) * (GameConfig.SCREEN_HEIGHT / (double)GameConfig.WINDOW_HEIGHT));
        });

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                inputState.mousePressed = (action == GLFW_PRESS);
                inputState.mouseHeld = (action == GLFW_PRESS || action == GLFW_REPEAT);
            }
        });

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }
            
            // WASD keys for camera movement
            boolean pressed = (action == GLFW_PRESS || action == GLFW_REPEAT);
            if (key == GLFW_KEY_W) inputState.keys[0] = pressed;
            if (key == GLFW_KEY_A) inputState.keys[1] = pressed;
            if (key == GLFW_KEY_S) inputState.keys[2] = pressed;
            if (key == GLFW_KEY_D) inputState.keys[3] = pressed;
        });
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            
            stateManager.update(0.016f, inputState);
            stateManager.render(renderer);
            
            glfwSwapBuffers(window);
            inputState.mousePressed = false; // Reset single-press events
        }
    }

    private void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
