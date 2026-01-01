# DodgingBullets Game Project

## Overview
A Java OpenGL 2D top-down game featuring a character that can move in 8 directions with animated sprites. Built with LWJGL for desktop and designed for future Android compatibility.

# DodgingBullets

A 2D top-down Java game built with LWJGL/OpenGL featuring grid-based collision detection, mouse-controlled shooting, and enemy combat mechanics.

## Game Overview

DodgingBullets is an isometric-style action game where the player navigates a tiled grass environment, shoots at turret enemies, and avoids obstacles. The game features a sophisticated collision system, camera controls, and visual effects.

## Technical Architecture

### Grid System & Collision Detection
- **Grid Size**: 16x16 pixel cells for spatial partitioning
- **Map Dimensions**: 2560x1440 pixels (160x90 cells)
- **Collision Types**: Axis-independent detection allowing sliding along obstacles
- **Spatial Queries**: Efficient grid-based lookups for collision detection

### Dual Hitbox System
The game uses two types of hitboxes for different collision purposes:

#### Sprite Hitboxes (Visual Collision)
- **Player**: 12 pixels wide, full sprite height
- **Turret**: 64x64 pixels (full sprite size)
- Used for bullet impacts and visual collision detection

#### Movement Hitboxes (Terrain Blocking)
- **Player**: 12 pixels wide, bottom 1/5th of sprite height
- **Turret**: 64x32 pixels (lower half of sprite)
- Used for terrain collision and movement blocking
- Prevents sprites from overlapping solid objects

### Movement System
- **Controls**: WASD keys for directional movement
- **Jumping**: Variable height control with spacebar
- **Physics**: Smooth movement with collision response
- **Sliding**: Players can slide along obstacles when hitting them at angles

### Shooting System
- **Control**: Mouse-based aiming and shooting
- **Trajectory**: Bullets travel in straight lines toward mouse cursor position
- **Angle Calculation**: Uses mouse position relative to player for bullet direction
- **Damage**: 10 damage per bullet hit
- **Visual**: Bright-colored bullets for gameplay clarity

### Shell Casing System
- **Physics**: Ejected shell casings with realistic trajectories
- **Particles**: Fade-out animations over time
- **Visual Effects**: Adds realism to shooting mechanics

### Enemy System - Turrets
- **Health**: 100 HP per turret
- **Damage**: Takes 10 damage per bullet hit
- **Destruction**: Requires 10 shots to destroy
- **States**: Intact and destroyed visual states
- **Hitboxes**: 64x64 sprite hitbox, 64x32 movement hitbox

### Camera System
- **Centering**: Camera follows player position
- **Boundary Clamping**: Prevents camera from showing areas beyond map edges
- **Map Bounds**: Camera clamped to 2560x1440 map dimensions
- **Smooth Tracking**: Player remains centered in viewport

### Rendering System
- **Depth Sorting**: Sprites rendered based on Y-position for proper layering
- **Isometric Layering**: Objects with higher Y-values render in front
- **Background**: Seamlessly tiled grass textures
- **Visual Contrast**: Darker backgrounds with brighter projectiles for clarity

### Texture System
- **Grass Background**: Procedurally generated seamless tiling textures
- **Sprites**: Player, turret, bullet, and shell casing graphics
- **Shadows**: Visual depth effects for sprites
- **Optimization**: Efficient texture loading and rendering

## File Structure
- `src/main/java/` - Java source code
- `src/main/resources/textures/` - Game textures and sprites
- `pom.xml` - Maven build configuration
- Generated textures created via Python scripts for seamless tiling

## Build & Run
```bash
mvn compile exec:java
```

## Key Features
- Grid-based collision detection with 16x16 pixel cells
- Dual hitbox system for sprite and movement collision
- Mouse-controlled shooting with angle-based trajectories
- Camera system with boundary clamping
- Turret enemies with health/damage mechanics
- Shell casing particle effects
- Depth-sorted isometric rendering
- Seamless tiled background textures
- WASD movement controls with jumping mechanics

## Technical Notes
- Uses LWJGL for OpenGL rendering
- Efficient spatial partitioning via grid system
- Axis-independent collision allows natural movement
- Visual improvements include darker backgrounds and brighter bullets
- Proper isometric depth sorting based on Y-position coordinates


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
