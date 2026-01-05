package com.dodgingbullets.editor.states;

import com.dodgingbullets.core.GameConfig;
import com.dodgingbullets.core.Renderer;
import com.dodgingbullets.editor.EditorInputState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EditorLevelSelectState implements EditorGameState {
    private EditorStateManager stateManager;
    private Renderer renderer;
    private List<String> levelFiles;
    private List<Button> levelButtons;

    public EditorLevelSelectState(EditorStateManager stateManager, Renderer renderer) {
        this.stateManager = stateManager;
        this.renderer = renderer;
        this.levelFiles = new ArrayList<>();
        this.levelButtons = new ArrayList<>();
    }

    @Override
    public void enter() {
        loadLevelFiles();
        createButtons();
    }

    @Override
    public void update(float deltaTime, EditorInputState inputState) {
        if (inputState.mousePressed) {
            for (int i = 0; i < levelButtons.size(); i++) {
                Button button = levelButtons.get(i);
                if (isPointInButton(inputState.mouseX, inputState.mouseY, button)) {
                    String levelFile = levelFiles.get(i);
                    EditorState editorState = new EditorState(stateManager, renderer, levelFile, this);
                    stateManager.setState(editorState);
                    return;
                }
            }
        }
    }

    @Override
    public void render(Renderer renderer) {
        renderer.clear();
        
        // Render title
        renderer.renderText("Level Editor", 250, 350, 1.0f, 1.0f, 1.0f);
        
        // Render level buttons
        for (int i = 0; i < levelButtons.size(); i++) {
            Button button = levelButtons.get(i);
            renderer.renderRect(button.x, button.y, button.width, button.height, 0.3f, 0.3f, 0.8f, 1.0f);
            
            String levelName = levelFiles.get(i).replace(".json", "");
            renderer.renderText(levelName, button.x + 10, button.y + button.height/2, 1.0f, 1.0f, 1.0f);
        }
        
        renderer.present();
    }

    @Override
    public void exit() {
    }

    private void loadLevelFiles() {
        levelFiles.clear();
        File mapsDir = new File("src/main/resources/maps");
        if (mapsDir.exists() && mapsDir.isDirectory()) {
            File[] files = mapsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    levelFiles.add(file.getName());
                }
            }
        }
        
        // Add default levels if none found
        if (levelFiles.isEmpty()) {
            levelFiles.add("level1.json");
            levelFiles.add("level2.json");
            levelFiles.add("level3.json");
        }
    }

    private void createButtons() {
        levelButtons.clear();
        float startY = 270;
        for (int i = 0; i < levelFiles.size(); i++) {
            float buttonY = startY - i * 60;
            levelButtons.add(new Button(200, buttonY, 300, 40));
        }
    }

    private boolean isPointInButton(float x, float y, Button button) {
        return x >= button.x && x <= button.x + button.width &&
               y >= button.y && y <= button.y + button.height;
    }

    private static class Button {
        float x, y, width, height;
        
        Button(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
