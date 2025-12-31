# DodgingBullets Game Project

## Overview
A Java OpenGL 2D top-down game featuring a character that can move in 8 directions with animated sprites. Built with LWJGL for desktop and designed for future Android compatibility.

## Project Structure
- **Core Game Logic**: `src/main/java/com/dodgingbullets/core/`
  - `Player.java` - Character movement and animation system
  - `Direction.java` - 8-direction movement enum
  - `Renderer.java` - Abstract rendering interface for platform independence
  - `Texture.java` - Texture data container

- **Desktop Implementation**: `src/main/java/com/dodgingbullets/desktop/`
  - `Game.java` - Main game loop and GLFW window management
  - `DesktopRenderer.java` - OpenGL rendering implementation

- **Assets**: `assets/` folder contains character sprites
  - 8 directions: up, down, left, right, upleft, upright, downleft, downright
  - Each direction has: 3 animation frames (01, 02, 03) + 1 idle frame
  - Format: mc{direction}{frame}.png (e.g., mcup01.png, mcupidle.png)

## Features
- **8-Direction Movement**: Arrow keys control character movement
- **Ping-Pong Animation**: Cycles through frames 01→02→03→02→01 when moving
- **Idle States**: Shows appropriate idle frame based on last movement direction
- **Android-Ready Architecture**: Abstracted renderer for easy platform porting
- **Orthogonal Projection**: Perfect for 2D sprite rendering
- **Alpha Transparency**: Properly handles PNG transparency

## Controls
- Arrow keys: Move character in 8 directions
- ESC: Quit game

## Running the Game
```bash
cd DodgingBullets
./play.sh
```

## Technical Details
- **Resolution**: 360x640 (Android phone aspect ratio 9:16)
- **Framework**: LWJGL 3.3.3 with OpenGL
- **Build System**: Maven
- **Platform**: macOS ARM64 (Apple Silicon)
- **JVM Args**: Requires `-XstartOnFirstThread` for macOS GLFW compatibility

## Future Expansion Plans
- Add enemies and bullet mechanics
- Implement grenade throwing
- Port to Android using same core logic with AndroidRenderer
- Add collision detection and game mechanics

## Architecture Benefits
The abstracted Renderer interface allows easy swapping between desktop OpenGL and future Android OpenGL implementations without changing core game logic.
