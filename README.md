# DodgingBullets Game Project

## Overview
A Java OpenGL 2D top-down game featuring a character that can move in 8 directions with animated sprites. Built with LWJGL for desktop and designed for future Android compatibility.

# DodgingBullets

A 2D top-down Java game built with LWJGL/OpenGL featuring grid-based collision detection, mouse-controlled shooting, enemy combat mechanics, health/ammo systems, and intelligent AI turrets.

## Game Overview

DodgingBullets is an isometric-style action game where the player navigates a tiled grass environment, shoots at intelligent turret enemies, manages health and ammunition, and survives enemy fire. The game features sophisticated collision systems, camera controls, visual effects, and tactical AI behavior.

## Core Gameplay Features

### Player Systems
- **Health System**: 100 HP starting health, loses 5 HP per enemy hit
- **Health Regeneration**: After 3 seconds without damage, regenerates 15 HP per second
- **Ammo System**: 5 starting ammo, consumes 1 per shot, regenerates 1 per second
- **Visual Feedback**: Health bar (green→red) and ammo bar (blue) displayed above player
- **Damage Flash**: Screen vignette flashes red when taking damage (0.5 second fade)

### Combat Mechanics
- **Bullet Ownership**: Player bullets (damage enemies) vs Enemy bullets (damage player)
- **Collision Detection**: Separate hitboxes for sprite collision vs movement blocking
- **Shell Casings**: Realistic ejected shell physics with fade-out animations
- **Directional Shooting**: Mouse-controlled aiming with angle-based trajectories

### Enemy AI - Intelligent Turrets
- **Sight Range**: 400 pixel detection radius
- **Directional Vision**: 45-degree field of view cone
- **Idle Scanning**: Rotates clockwise every 2 seconds when no player detected
- **Alert System**: Immediately tracks player when shot, exits idle state
- **Line of Sight**: Only engages when player is within view cone AND range
- **Health**: 100 HP, takes 10 damage per hit, requires 10 shots to destroy
- **Fire Rate**: Shoots every 0.5 seconds when player is in sight

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
- **Damage**: Player bullets deal 10 damage, enemy bullets deal 5 damage
- **Visual**: Bright-colored bullets for gameplay clarity
- **Ammo Management**: Limited ammunition with regeneration system

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
- **AI Behavior**: Intelligent scanning, tracking, and engagement
- **Directional Sprites**: 8-directional turret sprites (N, NE, E, SE, S, SW, W, NW)

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
- **Primitive Rendering**: Health/ammo bars use OpenGL primitives for clean UI
- **Vignette Effect**: Subtle screen darkening with damage flash feedback

### Texture System
- **Grass Background**: Procedurally generated seamless tiling textures
- **Sprites**: Player, turret, bullet, and shell casing graphics
- **Shadows**: Visual depth effects for sprites
- **Optimization**: Efficient texture loading and rendering
- **Vignette Overlay**: Screen edge darkening effect with damage feedback

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
- Intelligent turret enemies with sight-based AI
- Health and ammo management systems
- Visual feedback through UI bars and screen effects
- Shell casing particle effects
- Depth-sorted isometric rendering
- Seamless tiled background textures
- WASD movement controls with jumping mechanics
- Tactical combat requiring ammo and health management

## Technical Notes
- Uses LWJGL for OpenGL rendering
- Efficient spatial partitioning via grid system
- Axis-independent collision allows natural movement
- Visual improvements include darker backgrounds and brighter bullets
- Proper isometric depth sorting based on Y-position coordinates
- Primitive rendering for UI elements (health/ammo bars)
- Bullet ownership system prevents friendly fire
- AI state management for realistic enemy behavior

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

## Gameplay Mechanics Summary
1. **Survival**: Manage health (regenerates after 3s) and ammo (regenerates continuously)
2. **Combat**: Shoot turrets while avoiding their line of sight and range
3. **Tactics**: Use turret scanning patterns to approach from blind spots
4. **Resource Management**: Limited ammo requires strategic shooting
5. **Visual Feedback**: Health/ammo bars and damage flash effects guide gameplay

## Future Expansion Plans
- Add more enemy types with different AI behaviors
- Implement grenade throwing mechanics
- Add power-ups and weapon upgrades
- Port to Android using same core logic with AndroidRenderer
- Expand map with multiple levels and objectives
- Add sound effects and background music

## Architecture Benefits
The abstracted Renderer interface allows easy swapping between desktop OpenGL and future Android OpenGL implementations without changing core game logic. The modular AI system makes it easy to add new enemy types with different behaviors.
