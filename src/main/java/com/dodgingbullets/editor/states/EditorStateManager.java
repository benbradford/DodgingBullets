package com.dodgingbullets.editor.states;

import com.dodgingbullets.core.Renderer;
import com.dodgingbullets.editor.EditorInputState;

public class EditorStateManager {
    private EditorGameState currentState;
    private EditorGameState nextState;

    public void setState(EditorGameState newState) {
        this.nextState = newState;
    }

    public void update(float deltaTime, EditorInputState inputState) {
        if (nextState != null) {
            if (currentState != null) {
                currentState.exit();
            }
            currentState = nextState;
            currentState.enter();
            nextState = null;
            return; // Skip update on transition frame
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
