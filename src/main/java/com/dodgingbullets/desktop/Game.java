package com.dodgingbullets.desktop;

import com.dodgingbullets.core.Player;
import com.dodgingbullets.core.Renderer;
import com.dodgingbullets.core.Texture;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    private long window;
    private Renderer renderer;
    private Player player;
    private boolean[] keys = new boolean[4]; // UP, DOWN, LEFT, RIGHT
    
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
        
        // Android phone aspect ratio (9:16) scaled up
        window = glfwCreateWindow(360, 640, "Dodging Bullets", NULL, NULL);
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
        
        player = new Player(180, 320); // Center of screen
        player.loadTextures(renderer);
    }
    
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            player.update(keys);
            
            renderer.clear();
            
            Texture currentTexture = player.getCurrentTexture();
            renderer.render(currentTexture, 
                player.getX() - 32, player.getY() - 32, 64, 64);
            
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
