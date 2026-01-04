package com.dodgingbullets.core;

public class GamePlayState implements GameState {
    private GameLoop gameLoop;
    private GameRenderer gameRenderer;
    private Renderer renderer;
    private StateManager stateManager;
    private GameState levelSelectState;
    
    public GamePlayState(GameLoop gameLoop, GameRenderer gameRenderer, Renderer renderer, StateManager stateManager, GameState levelSelectState) {
        this.gameLoop = gameLoop;
        this.gameRenderer = gameRenderer;
        this.renderer = renderer;
        this.stateManager = stateManager;
        this.levelSelectState = levelSelectState;
    }
    
    public void setLevelSelectState(GameState levelSelectState) {
        this.levelSelectState = levelSelectState;
    }
    
    @Override
    public void update(float deltaTime, InputState inputState) {
        if (inputState.qPressed) {
            System.out.println("GamePlayState: Q pressed, transitioning to level select");
            stateManager.setState(levelSelectState);
            return;
        }
        
        gameLoop.update(inputState.keys, inputState.jumpPressed, inputState.jumpHeld, 
                       inputState.mousePressed, inputState.mouseHeld, inputState.grenadePressed,
                       inputState.spacePressed, inputState.spaceHeld,
                       inputState.mouseX, inputState.mouseY);
    }
    
    @Override
    public void render(Renderer renderer) {
        gameRenderer.render(renderer, gameLoop);
    }
    
    @Override
    public void enter() {
        // Reinitialize GameLoop with new level data
        gameLoop.initialize(renderer);
    }
    
    @Override
    public void exit() {}
}
