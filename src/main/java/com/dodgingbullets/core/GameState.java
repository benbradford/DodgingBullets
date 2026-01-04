package com.dodgingbullets.core;

public interface GameState {
    void update(float deltaTime, InputState inputState);
    void render(Renderer renderer);
    void enter();
    void exit();
}
