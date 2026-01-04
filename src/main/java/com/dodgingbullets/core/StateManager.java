package com.dodgingbullets.core;

public class StateManager {
    private GameState currentState;
    private GameState nextState;
    
    public void setState(GameState newState) {
        nextState = newState;
    }
    
    public void update(float deltaTime, InputState inputState) {
        if (nextState != null) {
            System.out.println("State transition: " + (currentState != null ? currentState.getClass().getSimpleName() : "null") + " -> " + nextState.getClass().getSimpleName());
            if (currentState != null) {
                currentState.exit();
            }
            currentState = nextState;
            currentState.enter();
            nextState = null;
            return; // Don't update the new state in the same frame
        }
        
        if (currentState != null) {
            currentState.update(deltaTime, inputState);
        }
    }
    
    public void render(Renderer renderer) {
        if (currentState != null) {
            currentState.render(renderer);
        }
    }
}
