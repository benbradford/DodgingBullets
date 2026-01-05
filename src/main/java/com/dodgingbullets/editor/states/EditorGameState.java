package com.dodgingbullets.editor.states;

import com.dodgingbullets.core.Renderer;
import com.dodgingbullets.editor.EditorInputState;

public interface EditorGameState {
    void update(float deltaTime, EditorInputState inputState);
    void render(Renderer renderer);
    void enter();
    void exit();
}
