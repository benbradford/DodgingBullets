package com.dodgingbullets.core;

import java.util.List;

public class LevelSelectState implements GameState {
    private LevelSelectScreen levelSelectScreen;
    private StateManager stateManager;
    private GamePlayState gamePlayState;
    private Renderer renderer;
    private Runnable backgroundUpdater;
    
    public LevelSelectState(StateManager stateManager, GamePlayState gamePlayState, Renderer renderer, Runnable backgroundUpdater) {
        this.stateManager = stateManager;
        this.gamePlayState = gamePlayState;
        this.renderer = renderer;
        this.backgroundUpdater = backgroundUpdater;
        this.levelSelectScreen = new LevelSelectScreen();
    }
    
    @Override
    public void update(float deltaTime, InputState inputState) {
        // Only handle mouse clicks in level select, ignore Q key
        if (inputState.mousePressed) {
            levelSelectScreen.handleClick(inputState.mouseX, inputState.mouseY);
            if (levelSelectScreen.isLevelSelected()) {
                GameObjectFactory.loadLevel(levelSelectScreen.getChosenLevel());
                backgroundUpdater.run(); // Update background texture
                stateManager.setState(gamePlayState);
            }
        }
    }
    
    @Override
    public void render(Renderer renderer) {
        renderer.clear();
        
        List<String> levels = levelSelectScreen.getAvailableLevels();
        float buttonWidth = 200;
        float buttonHeight = 40;
        float startX = (GameConfig.SCREEN_WIDTH - buttonWidth) / 2;
        float startY = 270;
        
        for (int i = 0; i < levels.size(); i++) {
            float buttonY = startY - i * 60;
            renderer.renderRect(startX, buttonY, buttonWidth, buttonHeight, 0.3f, 0.3f, 0.3f, 0.8f);
            renderer.renderRectOutline(startX, buttonY, buttonWidth, buttonHeight, 1.0f, 1.0f, 1.0f, 1.0f);
            
            String levelName = levels.get(i).replace(".json", "").toUpperCase();
            renderer.renderText(levelName, startX + 10, buttonY + 15, 1.0f, 1.0f, 1.0f);
        }
        
        renderer.present();
    }
    
    @Override
    public void enter() {}
    
    @Override
    public void exit() {}
}
