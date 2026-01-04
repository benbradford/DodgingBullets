package com.dodgingbullets.core;

public class PauseState implements GameState {
    private StateManager stateManager;
    private GameState previousState;
    
    public PauseState(StateManager stateManager, GameState previousState) {
        this.stateManager = stateManager;
        this.previousState = previousState;
    }
    
    @Override
    public void update(float deltaTime, InputState inputState) {
        // Resume game on any key press
        if (inputState.keys[0] || inputState.keys[1] || inputState.keys[2] || inputState.keys[3] || inputState.mousePressed) {
            stateManager.setState(previousState);
        }
    }
    
    @Override
    public void render(Renderer renderer) {
        // Render pause overlay
        renderer.renderRect(0, 0, GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT, 0.0f, 0.0f, 0.0f, 0.5f);
        
        // Render pause text
        float textX = GameConfig.SCREEN_WIDTH / 2 - 50;
        float textY = GameConfig.SCREEN_HEIGHT / 2;
        renderer.renderText("PAUSED", textX, textY, 1.0f, 1.0f, 1.0f);
        renderer.renderText("Press any key to resume", textX - 30, textY - 30, 0.8f, 0.8f, 0.8f);
        
        renderer.present();
    }
    
    @Override
    public void enter() {}
    
    @Override
    public void exit() {}
}
