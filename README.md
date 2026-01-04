# DodgingBullets Game Project

## Overview
A Java OpenGL 2D top-down game featuring a character that can move in 8 directions with animated sprites. Built with LWJGL for desktop and designed for future Android compatibility using an object-oriented GameObject architecture.

# DodgingBullets

A 2D top-down Java game built with LWJGL/OpenGL featuring grid-based collision detection, mouse-controlled shooting, enemy combat mechanics, health/ammo systems, and intelligent AI turrets. The game now uses a modular GameObject system with interface-based design for easy extensibility and level loading.

## Game Overview

DodgingBullets is an isometric-style action game where the player navigates a tiled grass environment, shoots at multiple intelligent turret enemies, manages health and ammunition, and survives enemy fire. The game features sophisticated collision systems, camera controls, visual effects, and tactical AI behavior with a clean object-oriented architecture.

## Core Gameplay Features

### Player Systems
- **Health System**: 100 HP starting health, loses 5 HP per enemy hit
- **Health Regeneration**: After 3 seconds without damage, regenerates 15 HP per second
- **Ammo System**: 10 starting ammo, consumes 1 per shot, regenerates 1 per second
- **Visual Feedback**: Health bar (green→red) and ammo bar (blue) displayed above player
- **Damage Flash**: Screen vignette flashes red when taking damage (0.5 second fade)

### Combat Mechanics
- **Bullet Ownership**: Player bullets (damage enemies) vs Enemy bullets (damage player)
- **Collision Detection**: Separate hitboxes for sprite collision vs movement blocking
- **Shell Casings**: Realistic ejected shell physics with fade-out animations
- **Directional Shooting**: Mouse-controlled aiming with angle-based trajectories
- **Multiple Enemies**: 3 turrets at different map locations (800,150), (1200,400), (600,600)

### Enemy AI - Intelligent Turrets
- **Sight Range**: 320 pixel detection radius (reduced from 400)
- **Directional Vision**: 45-degree field of view cone
- **Idle Scanning**: Rotates clockwise every 2 seconds when no player detected
- **Alert System**: Immediately tracks player when shot, exits idle state
- **Line of Sight**: Only engages when player is within view cone AND range
- **Health**: 100 HP, takes 10 damage per hit, requires 10 shots to destroy
- **Fire Rate**: Shoots every 0.5 seconds when player is in sight
- **Independent AI**: Each turret operates independently with its own state

### State Machine System (NEW)
The game now uses a state machine to manage different screens and game modes:

#### Core Components
- **GameState Interface**: Common interface for all game states (update, render, enter, exit)
- **StateManager**: Handles state transitions and manages current state
- **Concrete States**: LevelSelectState, GamePlayState, and extensible for pause/map screens

#### Current States
- **LevelSelectState**: Level selection screen with clickable level buttons
- **GamePlayState**: Main gameplay state (wraps existing GameLoop + GameRenderer)
- **PauseState**: Example implementation for pause functionality

#### Benefits
- **Clean Separation**: Each screen/mode is isolated in its own state class
- **Easy Extension**: Adding new states (pause, map, settings) requires only implementing GameState
- **Centralized Management**: StateManager handles all transitions and state lifecycle
- **Consistent Interface**: All states use same InputState and Renderer interfaces

## Technical Architecture

### GameObject System (NEW)
The game now uses a modular object-oriented architecture:

#### Core Classes
- **GameObject** (abstract): Base class for all game entities with position and update logic
- **EnemyObject** (abstract): Extends GameObject, base for all enemies with health system
- **GameLoop**: Platform-independent game logic and state management
- **GameRenderer**: Platform-independent rendering logic with depth sorting

#### Interface System
- **Renderable**: Objects that can be rendered with depth sorting
- **Collidable**: Objects with collision detection (sprite and movement hitboxes)
- **Damageable**: Objects that can take damage and be destroyed
- **Shooter**: Objects that can shoot projectiles with cooldown management
- **Trackable**: Objects that can track and see the player (AI vision system)
- **Positionable**: Objects with barrel positions and sprite hitbox detection

#### Concrete Implementations
- **GunTurret**: Implements EnemyObject + Shooter + Trackable + Positionable
  - All turret functionality moved from old Turret class
  - Proper interface-based design for extensibility

#### Game Management
- **GameObjectManager**: Handles collections of game objects with filtering by interface
- **Interface-based interactions**: GameLoop only uses interfaces, not concrete classes
- **Polymorphic arrays**: Ready for level loading with mixed enemy types

#### Platform Architecture
- **GameLoop** (core): Contains all platform-independent game logic (player updates, AI, physics, collision detection)
- **GameRenderer** (core): Contains all platform-independent rendering logic (depth sorting, UI, effects)
- **Game** (desktop): Only handles desktop-specific concerns (GLFW window, input callbacks, texture loading)
- **DesktopRenderer** (desktop): OpenGL-specific rendering implementation

### Collision Detection
- **Map Dimensions**: 2560x1440 pixels (160x90 cells)
- **Collision Types**: Axis-independent detection allowing sliding along obstacles

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

### Enemy System - Multiple Turrets
- **Health**: 100 HP per turret
- **Damage**: Takes 10 damage per bullet hit
- **Destruction**: Requires 10 shots to destroy
- **States**: Intact and destroyed visual states
- **Hitboxes**: 64x64 sprite hitbox, 64x32 movement hitbox
- **AI Behavior**: Intelligent scanning, tracking, and engagement
- **Directional Sprites**: 8-directional turret sprites (N, NE, E, SE, S, SW, W, NW)
- **Multiple Instances**: 3 turrets at different locations with independent AI
- **Damage Flash**: Red flash effect for 300ms when taking damage (alternates every 50ms)
- **Explosion Effect**: Creates explosion animation when destroyed

### Camera System
- **Centering**: Camera follows player position
- **Boundary Clamping**: Prevents camera from showing areas beyond map edges
- **Map Bounds**: Camera clamped to 2560x1440 map dimensions
- **Smooth Tracking**: Player remains centered in viewport

### Rendering System
- **Depth Sorting**: All objects (player + turrets) rendered based on Y-position for proper layering
- **Isometric Layering**: Objects with higher Y-values render in front
- **Background**: Seamlessly tiled grass textures
- **Visual Contrast**: Darker backgrounds with brighter projectiles for clarity
- **Primitive Rendering**: Health/ammo bars use OpenGL primitives for clean UI
- **Vignette Effect**: Subtle screen darkening with damage flash feedback
- **Polymorphic Rendering**: Uses interface casting for flexible object rendering

### Texture System
- **Grass Background**: Procedurally generated seamless tiling textures
- **Sprites**: Player, turret, bullet, and shell casing graphics
- **Shadows**: Visual depth effects for sprites
- **Optimization**: Efficient texture loading and rendering
- **Vignette Overlay**: Screen edge darkening effect with damage feedback

## File Structure
- `src/main/java/com/dodgingbullets/core/` - Core game classes (Player, Bullet, GameLoop, GameRenderer)
- `src/main/java/com/dodgingbullets/gameobjects/` - GameObject system interfaces and base classes
- `src/main/java/com/dodgingbullets/gameobjects/enemies/` - Enemy implementations (GunTurret)
- `src/main/java/com/dodgingbullets/gameobjects/effects/` - Visual effects (Explosion)
- `src/main/java/com/dodgingbullets/desktop/` - Desktop-specific implementation (Game, DesktopRenderer)
- `src/main/resources/textures/` - Game textures and sprites
- `assets/` - Original texture assets including explosion animation frames
- `pom.xml` - Maven build configuration

## Build & Run
```bash
mvn compile exec:java
```

## Key Features
- **Object-oriented GameObject system** with interface-based design
- **Multiple enemy support** with 3 independent turrets
- Grid-based collision detection with 16x16 pixel cells
- Dual hitbox system for sprite and movement collision
- Mouse-controlled shooting with angle-based trajectories
- Camera system with boundary clamping
- Intelligent turret enemies with sight-based AI
- Health and ammo management systems
- Visual feedback through UI bars and screen effects
- Shell casing particle effects
- Depth-sorted isometric rendering for all objects
- Seamless tiled background textures
- WASD movement controls with jumping mechanics
- Tactical combat requiring ammo and health management

## Technical Notes
- Uses LWJGL for OpenGL rendering
- **Interface-based architecture** ready for level loading
- **Polymorphic object management** supports mixed enemy types
- Efficient spatial partitioning via grid system
- Axis-independent collision allows natural movement
- Visual improvements include darker backgrounds and brighter bullets
- Proper isometric depth sorting based on Y-position coordinates
- Primitive rendering for UI elements (health/ammo bars)
- Bullet ownership system prevents friendly fire
- AI state management for realistic enemy behavior
- **Modular design** allows easy addition of new enemy types

## Running the Game
```bash
/bin/zsh /Users/bebradfo/code/DodgingBullets/DodgingBullets/play.sh
```

**Note**: Currently using placeholder colored block textures. The game shows:
- Blue blocks: Player character
- Red blocks: Turrets  
- Green blocks: Foliage
- Yellow blocks: Bullets
- Other colored blocks: Various game elements

## Technical Details
- **Resolution**: 360x640 (Android phone aspect ratio 9:16)
- **Framework**: LWJGL 3.3.3 with OpenGL
- **Build System**: Maven
- **Platform**: macOS ARM64 (Apple Silicon)
- **JVM Args**: Requires `-XstartOnFirstThread` for macOS GLFW compatibility

## Gameplay Mechanics Summary
1. **Survival**: Manage health (regenerates after 3s) and ammo (regenerates continuously)
2. **Combat**: Shoot multiple turrets while avoiding their line of sight and range
3. **Tactics**: Use turret scanning patterns to approach from blind spots
4. **Resource Management**: Limited ammo requires strategic shooting
5. **Visual Feedback**: Health/ammo bars and damage flash effects guide gameplay
6. **Multiple Threats**: Navigate between 3 turrets with independent AI behavior

## Current Development State

### Recently Completed
- **GameObject Architecture**: Implemented complete interface-based system
- **Multiple Turrets**: Added 3 turrets at different map locations
- **Interface Design**: Game.java uses only interfaces, no concrete class dependencies
- **Polymorphic Rendering**: Depth-sorted rendering for mixed object types
- **Modular AI**: Each turret operates independently with proper state management
- **Explosion Effects**: Added animated explosion effects when turrets are destroyed
- **Damage Feedback**: Implemented red flash effects for turret damage indication
- **Mouse Scaling**: Fixed shooting direction accuracy with proper coordinate scaling

### Ready for Next Steps
- **Level Loading**: Architecture supports loading different enemy types from files
- **GameObjectManager**: Ready to manage collections of mixed game objects
- **Extensible Design**: Easy to add new enemy types by implementing interfaces
- **File-based Configuration**: GameObject system ready for data-driven level design

## Future Expansion Plans
- **Level Loading System**: Load enemy positions and types from configuration files
- Add more enemy types with different AI behaviors:
  - Moving enemies (implement Trackable without Shooter)
  - Static damage traps (implement Collidable + Damageable only)
  - Moving shooters (implement all interfaces)
- Implement grenade throwing mechanics
- Add power-ups and weapon upgrades
- Port to Android using same core logic with AndroidRenderer
- Expand map with multiple levels and objectives
- Add sound effects and background music

## Architecture Benefits
The abstracted Renderer interface allows easy swapping between desktop OpenGL and future Android OpenGL implementations without changing core game logic. The modular GameObject system with interface-based design makes it trivial to add new enemy types with different behavior combinations. The separation of GameLoop (logic) and GameRenderer (rendering) from platform-specific code means the same core game can run on desktop and Android with only platform-specific input/rendering implementations. The system is now ready for level loading where different enemy types can be instantiated from file data and managed polymorphically.

## Development Lessons Learned

### Mouse Coordinate Scaling
When implementing viewport scaling/zooming, mouse coordinates must be properly scaled to match the game world coordinates. Use constants for window size and screen size to ensure shooting direction calculations remain accurate regardless of zoom level:
```java
// Scale mouse coordinates from window size to game world size
double scaledMouseX = mouseX * (SCREEN_WIDTH / WINDOW_WIDTH);
double scaledMouseY = mouseY * (SCREEN_HEIGHT / WINDOW_HEIGHT);
```

### Visual Effects Implementation
- **Explosion System**: Implemented as GameObject with Renderable and Collidable interfaces for consistent architecture
- **Animation Timing**: 0.2 seconds per frame provides smooth explosion animation
- **Render Depth**: Explosions render on top of all other objects regardless of Y position for maximum visual impact
- **Collision Damage**: Continuous damage (1 HP per frame) while player is in explosion area

### Damage Flash Effects
- **Color Multiplication**: `renderTextureWithColor` uses multiplicative blending, making bright textures difficult to brighten further
- **Effective Flash**: Red color tint (1.0f, 0.0f, 0.0f, 1.0f) alternating with normal colors provides clear damage feedback
- **Timing**: 50ms intervals for 300ms total duration creates noticeable but not overwhelming flash effect
- **Shape Preservation**: Using texture color tinting maintains original sprite shape better than overlay rectangles

## Next Development Priority
**Level Loading System**: The GameObject architecture is complete and ready for implementing a level loading system that can read enemy positions and types from configuration files, instantiate the appropriate GameObject implementations, and manage them through the existing GameObjectManager.
