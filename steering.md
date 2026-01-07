# DodgingBullets Project Steering Guide

## Critical Asset Management Rule
**NEVER create, copy, move, rename, or delete asset files unless explicitly requested by the user.**

Assets include all files in the `assets/` directory: textures (.png, .jpg), audio (.wav, .mp3), models, etc.

When implementing features requiring assets:
1. Reference asset paths in code only
2. Let the user provide the actual asset files
3. Ask permission before creating any placeholder assets

## Project Overview
DodgingBullets is a 2D top-down Java game built with LWJGL/OpenGL featuring:
- Player character with 8-directional movement and mouse-controlled shooting
- Multiple intelligent turret enemies with AI behavior
- Health/ammo systems with regeneration mechanics
- Grid-based collision detection with dual hitbox system
- Interface-based GameObject architecture for extensibility

## State Machine System

### Purpose
The state machine manages different screens and game modes (level select, gameplay, pause, etc.) with clean separation and easy transitions. Each state handles its own input, rendering, and logic independently.

### Core Components
- **GameState**: Interface for all game states (update, render, enter, exit)
- **StateManager**: Manages state transitions and current state execution
- **Concrete States**: LevelSelectState, GamePlayState, PauseState (example)

### How It Works

#### State Lifecycle
1. **enter()**: Called when transitioning INTO this state (initialization)
2. **update()**: Called every frame while state is active (input handling, logic)
3. **render()**: Called every frame while state is active (drawing)
4. **exit()**: Called when transitioning OUT of this state (cleanup)

#### State Transitions
```java
// From any state, request transition to another state
stateManager.setState(newState);
```

#### StateManager Logic
1. **Transition Frame**: When setState() is called, StateManager:
   - Calls currentState.exit()
   - Sets new state as current
   - Calls newState.enter()
   - **Skips update() for this frame** (prevents input bleeding)

2. **Normal Frame**: 
   - Calls currentState.update() with input
   - Calls currentState.render()

### Current Implementation

#### LevelSelectState
- **Purpose**: Display level selection screen
- **Input**: Mouse clicks on level buttons
- **Transitions**: To GamePlayState when level selected
- **Key Feature**: Loads level data and updates background texture

#### GamePlayState  
- **Purpose**: Main game loop (player, enemies, shooting, etc.)
- **Input**: All game controls (WASD, mouse, J, G, Q)
- **Transitions**: To LevelSelectState when Q pressed
- **Key Feature**: Reinitializes GameLoop with new level data on enter()

### Adding New States
1. **Create State Class**: Implement GameState interface
```java
public class MapState implements GameState {
    private StateManager stateManager;
    private GameState previousState;
    
    public MapState(StateManager stateManager, GameState previousState) {
        this.stateManager = stateManager;
        this.previousState = previousState;
    }
    
    @Override
    public void update(float deltaTime, InputState inputState) {
        if (inputState.keys[0]) { // W key
            stateManager.setState(previousState);
        }
        // Handle map navigation input
    }
    
    @Override
    public void render(Renderer renderer) {
        // Render map screen
        renderer.clear();
        renderer.renderText("MAP SCREEN", 100, 100, 1.0f, 1.0f, 1.0f);
        renderer.present();
    }
    
    @Override
    public void enter() { /* Initialize map data */ }
    
    @Override
    public void exit() { /* Cleanup map resources */ }
}
```

2. **Create State Instance**: In Game.java initialization
```java
MapState mapState = new MapState(stateManager, gamePlayState);
```

3. **Add Transition Logic**: In existing states
```java
// In GamePlayState.update()
if (inputState.keys[someKey]) {
    stateManager.setState(mapState);
}
```

### Circular Reference Pattern
For states that transition back and forth (like GamePlay ↔ LevelSelect):

```java
// Create first state with null reference
GamePlayState gamePlayState = new GamePlayState(..., null);
// Create second state with reference to first
LevelSelectState levelSelectState = new LevelSelectState(..., gamePlayState);
// Update first state's reference to second
gamePlayState.setLevelSelectState(levelSelectState);
```

### Best Practices

#### Input Handling
- **Single-press events**: Use separate boolean fields (qPressed, jumpPressed)
- **Continuous events**: Use keys[] array (WASD movement)
- **State-specific input**: Only handle relevant inputs in each state

#### State Transitions
- **Immediate return**: After calling setState(), return from update() to prevent further processing
- **Frame delay**: StateManager skips update() on transition frame to prevent input bleeding
- **Clean references**: Ensure circular references are properly established

#### Resource Management
- **enter()**: Load state-specific resources, reinitialize data
- **exit()**: Clean up resources, save state if needed
- **Shared resources**: Keep in Game.java, pass via constructor or setter

### Common Patterns

#### Pause Overlay
```java
public class PauseState implements GameState {
    private GameState previousState;
    
    @Override
    public void render(Renderer renderer) {
        // Render previous state first (frozen game)
        previousState.render(renderer);
        // Render pause overlay on top
        renderer.renderRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 0, 0, 0.5f);
        renderer.renderText("PAUSED", centerX, centerY, 1, 1, 1);
    }
}
```

#### Settings Menu
```java
public class SettingsState implements GameState {
    private List<Setting> settings;
    private int selectedIndex = 0;
    
    @Override
    public void update(float deltaTime, InputState inputState) {
        if (inputState.keys[0]) selectedIndex--; // W
        if (inputState.keys[1]) selectedIndex++; // S
        if (inputState.mousePressed) applySettings();
    }
}
```

### Debugging State Issues

#### Add Logging
```java
// In StateManager.update()
System.out.println("State transition: " + 
    (currentState != null ? currentState.getClass().getSimpleName() : "null") + 
    " -> " + nextState.getClass().getSimpleName());
```

#### Common Issues
- **Input bleeding**: Fixed by StateManager frame delay
- **Broken references**: Use setter pattern for circular references
- **Multiple instances**: Don't recreate states, reuse existing instances
- **Missing transitions**: Ensure setState() is called and references are correct

## Architecture Principles

### Core Design Patterns
1. **Interface-Based Design**: All game objects implement specific interfaces (Renderable, Collidable, Damageable, etc.)
2. **Separation of Concerns**: Clear separation between core logic, rendering, and platform-specific code
3. **Vec2 for Coordinates**: All position data uses Vec2 class instead of separate x,y variables
4. **Centralized Configuration**: All constants in GameConfig class

### Key Classes & Responsibilities
- **GameLoop**: Core game logic, orchestrates updates
- **GameRenderer**: Platform-independent rendering logic
- **CollisionSystem**: Handles all collision detection
- **InputHandler**: Processes and transforms input
- **GameObject**: Base class for all game entities
- **Vec2**: Immutable 2D vector for positions and math operations

## Adding New Game Objects

### 1. Create the Class
```java
public class NewEnemy extends EnemyObject implements Shooter, Trackable {
    public NewEnemy(float x, float y) {
        super(x, y, 50); // 50 health
    }
    
    @Override
    public void update(float deltaTime) {
        // Update logic here
    }
    
    // Implement required interface methods
}
```

### 2. Add to GameObjectFactory
```java
public static GameObject createNewEnemy(float x, float y) {
    return new NewEnemy(x, y);
}
```

### 3. Update Initialization
Add creation calls in `GameObjectFactory.createTurrets()` or create new factory method.

### 4. Add Textures (if needed)
- Place texture files in `assets/` directory
- Load in `Game.loadTextures()` method
- Pass to GameRenderer via `setTextures()`

## Interface Guidelines

### Available Interfaces
- **Renderable**: Objects that can be rendered (`getRenderY()` for depth sorting)
- **Collidable**: Objects with collision (`checkSpriteCollision()`, `checkMovementCollision()`)
- **Damageable**: Objects that can take damage (`takeDamage()`, `isDestroyed()`)
- **Shooter**: Objects that can shoot (`canShoot()`, `shoot()`)
- **Trackable**: Objects that can track player (`canSeePlayer()`, `getFacingDirection()`)
- **Positionable**: Objects with barrel positions (`getBarrelPosition()`, `isInSpriteHitbox()`)

### Implementation Rules
1. **Use Interfaces, Not Concrete Classes**: GameLoop only interacts via interfaces
2. **Polymorphic Collections**: `List<GameObject>` can contain any game object type
3. **Interface Casting**: Cast to interfaces when needed: `(Trackable) gameObject`

## Collision System

### Dual Hitbox Design
1. **Sprite Hitbox**: For visual collisions (bullets, interactions)
2. **Movement Hitbox**: For terrain blocking (usually smaller, bottom portion)

### Adding Collision
```java
@Override
public boolean checkSpriteCollision(float x, float y, float width, float height) {
    Vec2 bulletPos = new Vec2(x, y);
    Vec2 halfSize = new Vec2(32, 32);
    Vec2 min = position.subtract(halfSize);
    Vec2 max = position.add(halfSize);
    return bulletPos.x() >= min.x() && bulletPos.x() <= max.x() && 
           bulletPos.y() >= min.y() && bulletPos.y() <= max.y();
}
```

## Vec2 Usage

### Always Use Vec2 for Positions
```java
// ✅ Correct
private Vec2 position = new Vec2(x, y);
Vec2 newPos = position.add(movement);

// ❌ Avoid
private float x, y;
x += deltaX; y += deltaY;
```

### Vec2 Operations
- `add(Vec2)`, `subtract(Vec2)`, `multiply(float)`
- `distance(Vec2)`, `angle()`, `clamp(Vec2, Vec2)`
- `Vec2.fromAngle(angle, magnitude)` for directional vectors

## Configuration Management

### Add Constants to GameConfig
```java
public class GameConfig {
    // Gameplay constants
    public static final int NEW_ENEMY_DAMAGE = 15;
    public static final float NEW_ENEMY_SPEED = 1.5f;
}
```

### Use Constants, Not Magic Numbers
```java
// ✅ Correct
damageable.takeDamage(GameConfig.PLAYER_DAMAGE);

// ❌ Avoid
damageable.takeDamage(10);
```

## Rendering Guidelines

### Depth Sorting
- Objects render based on Y position (higher Y = in front)
- Implement `getRenderY()` in Renderable interface
- Special cases: explosions render on top (`position.y() + 1000`)

### Texture Loading
1. Place textures in `assets/` directory
2. Load in `Game.loadTextures()`
3. Pass to GameRenderer via `setTextures()`
4. Reference by name in rendering code

## Input Handling

### Adding New Input
1. Add to `Game.setupInputCallbacks()` for raw input
2. Process in `InputHandler.processInput()` for game logic
3. Add fields to `InputState` if needed
4. Handle in `GameLoop.handleShooting()` or similar methods

## Testing & Building

### Build Commands
```bash
# Compile and run
/bin/zsh /Users/bebradfo/code/DodgingBullets/DodgingBullets/play.sh

# Just compile
mvn compile
```

### File Structure
```
src/main/java/com/dodgingbullets/
├── core/           # Platform-independent game logic
├── desktop/        # Desktop-specific (LWJGL) implementation
└── gameobjects/    # GameObject implementations
    ├── enemies/    # Enemy implementations
    ├── effects/    # Visual effects
    └── environment/# Environmental objects
```

## Common Patterns

### Adding AI Behavior
1. Implement `Trackable` interface
2. Add state management (idle, tracking, etc.)
3. Use `Vec2` for distance/angle calculations
4. Update in `GameLoop.updateTurrets()`

### Adding Visual Effects
1. Extend `GameObject`, implement `Renderable`
2. Use animation frames with timers
3. Set `active = false` when complete
4. Add to explosions list or similar collection

### Adding Weapons/Projectiles
1. Implement `Shooter` interface on source object
2. Create bullet in `GameLoop.handleShooting()`
3. Handle collision in `CollisionSystem`
4. Use `Vec2` for trajectory calculations

## Best Practices

1. **Interface First**: Design interfaces before implementations
2. **Immutable Vec2**: Always create new Vec2 instances, don't modify existing ones
3. **Centralized Constants**: All magic numbers go in GameConfig
4. **Polymorphic Design**: Use GameObject collections, cast to interfaces as needed
5. **Separation of Concerns**: Keep rendering, logic, and input handling separate
6. **Consistent Naming**: Use descriptive names like `gameObjects` not `turrets`
7. **Test After Each Change**: Always run `mvn compile` after significant changes to catch errors early
8. **Interface Completeness**: Verify all interface methods are implemented before testing

## Development Lessons Learned

### Interface Implementation Requirements
- **Missing Methods**: When implementing interfaces, ALL methods must be implemented, including empty ones
- **Renderable Interface**: Requires both `render()` and `getRenderY()` methods - missing `render()` causes compilation errors
- **Empty Implementation**: Use `// Rendering handled by GameRenderer` for objects rendered externally

### Input State Management
- **Mouse Input**: Distinguish between `mousePressed` (single click) and `mouseHeld` (continuous hold)
- **State Tracking**: Use separate boolean fields for press events vs hold states
- **Event Handling**: GLFW callbacks need both PRESS and RELEASE event handling for proper state management
- **Parameter Propagation**: Input state changes require updates through entire call chain (Game → GameLoop → InputHandler → InputState)

### Bullet System Architecture
- **Polymorphic Bullets**: Use constructor overloading and boolean flags for different bullet types
- **Spread Implementation**: Add randomness to angle calculation for special bullets: `angle += (Math.random() - 0.5) * 0.3`
- **Rate Limiting**: Use timestamps and intervals for controlled rapid fire (200ms = 5 bullets/second)
- **Visual Distinction**: Different colors for bullet types while maintaining same collision logic

### UI State Management
- **Dynamic UI**: Ammo bars can change appearance based on game state (normal blue vs flashing red/blue)
- **Smooth Animations**: Use `Math.sin(time * frequency)` for smooth oscillating effects
- **State-Dependent Rendering**: Check game state before rendering UI elements differently

### Power-Up System Implementation
- **Collection Logic**: Use collision detection with player hitbox for power-up collection
- **State Management**: Track collected state and change textures accordingly (full → empty crate)
- **Special Mechanics**: Implement temporary power-ups that modify player behavior (special bullets, rapid fire)
- **Visual Feedback**: Use flashing UI elements to indicate special states

## Coordinate Systems & Screen Space

### Window vs Screen Coordinates
The game uses two different coordinate systems that must be properly handled:

#### Window Coordinates (GLFW Input)
- **Size**: 640x360 pixels (GameConfig.WINDOW_WIDTH/HEIGHT)
- **Source**: Raw mouse input from GLFW callbacks
- **Y-Origin**: Top-left corner (Y=0 at top, increases downward)
- **Usage**: Direct GLFW mouse position callbacks

#### Screen Coordinates (Game Logic)
- **Size**: 704x396 pixels (GameConfig.SCREEN_WIDTH/HEIGHT) 
- **Source**: Scaled from window coordinates for game logic
- **Y-Origin**: Bottom-left corner (Y=0 at bottom, increases upward)
- **Usage**: All game object positions, collision detection, rendering

### Coordinate Transformation Rules

#### Mouse Input Scaling
Mouse coordinates MUST be scaled from window space to screen space:

```java
// ✅ CORRECT: Scale mouse coordinates
glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
    mouseX = xpos * (GameConfig.SCREEN_WIDTH / GameConfig.WINDOW_WIDTH);
    mouseY = (360 - ypos) * (GameConfig.SCREEN_HEIGHT / GameConfig.WINDOW_HEIGHT);
});

// ❌ WRONG: Direct window coordinates
mouseX = xpos;
mouseY = 360 - ypos;
```

#### Y-Coordinate Flipping
OpenGL uses bottom-left origin, but GLFW uses top-left:

```java
// Window Y (top-left origin) → Screen Y (bottom-left origin)
screenY = WINDOW_HEIGHT - windowY;

// Then scale to screen space
screenY = screenY * (SCREEN_HEIGHT / WINDOW_HEIGHT);
```

### UI Positioning Guidelines

#### Button Positioning
When positioning UI elements, account for OpenGL's bottom-left origin:

```java
// Position buttons from top of screen downward
float startY = 270; // Start near top of 396px screen
for (int i = 0; i < buttons.size(); i++) {
    float buttonY = startY - i * 60; // Subtract to go downward
    // Level 1: Y=270-310, Level 2: Y=210-250, Level 3: Y=150-190
}
```

#### Common Y-Position Issues
- **Buttons Off-Screen**: If startY + buttonHeight > SCREEN_HEIGHT, buttons render above visible area
- **Click Detection Mismatch**: Mouse coordinates must use same coordinate system as button positions
- **Visual vs Logical Position**: What appears "at the top" visually is at higher Y values in OpenGL

### Debugging Coordinate Issues

#### Add Debug Logging
```java
System.out.println("Click at: " + mouseX + ", " + mouseY);
System.out.println("Button at Y: " + buttonY + " to " + (buttonY + buttonHeight));
```

#### Check Coordinate Ranges
- **Screen bounds**: X: 0-704, Y: 0-396
- **Window bounds**: X: 0-640, Y: 0-360
- **Mouse scaling**: Should multiply by ~1.1 for both axes

#### Validation Checklist
1. ✅ Mouse coordinates scaled from window to screen space
2. ✅ Y-coordinates flipped (GLFW top-left → OpenGL bottom-left)
3. ✅ UI elements positioned within screen bounds (0-396 for Y)
4. ✅ Click detection uses same coordinate system as rendering
5. ✅ Button positions account for OpenGL bottom-left origin

### Development Lessons Learned

#### Coordinate System Debugging
- **Symptom**: Clicking on visible buttons doesn't register
- **Cause**: Mouse coordinates in window space, game logic in screen space
- **Solution**: Scale mouse input by width/height ratios
- **Prevention**: Always use GameConfig constants for coordinate transformations

#### Y-Axis Orientation
- **OpenGL Convention**: Y=0 at bottom, increases upward
- **GLFW Convention**: Y=0 at top, increases downward  
- **UI Positioning**: Higher Y values appear "higher" on screen in OpenGL
- **Button Layout**: Subtract spacing to position buttons "downward" visually

## Image Composition with ImageMagick

### Creating Overlapping Sprite Groups

When combining multiple sprites into a single image while preserving color and allowing overlap:

#### ✅ CORRECT Method (Preserves Color)
Use single-command composition with `convert` and `canvas:transparent`:

```bash
# Two sprites with 400px overlap
convert -size 1282x798 canvas:transparent \
  sprite.png -geometry +0+0 -composite \
  sprite.png -geometry +441+0 -composite \
  output.png

# Five sprites horizontally with 400px overlap
convert -size 2605x798 canvas:transparent \
  sprite.png -geometry +0+0 -composite \
  sprite.png -geometry +441+0 -composite \
  sprite.png -geometry +882+0 -composite \
  sprite.png -geometry +1323+0 -composite \
  sprite.png -geometry +1764+0 -composite \
  output.png

# 2x5 grid with 400px horizontal, 600px vertical overlap
convert -size 2605x996 canvas:transparent \
  sprite.png -geometry +0+0 -composite \
  sprite.png -geometry +441+0 -composite \
  sprite.png -geometry +882+0 -composite \
  sprite.png -geometry +1323+0 -composite \
  sprite.png -geometry +1764+0 -composite \
  sprite.png -geometry +0+198 -composite \
  sprite.png -geometry +441+198 -composite \
  sprite.png -geometry +882+198 -composite \
  sprite.png -geometry +1323+198 -composite \
  sprite.png -geometry +1764+198 -composite \
  output.png
```

#### ❌ AVOID (Causes Grayscale/Color Loss)
- Multiple separate composite operations
- Using `magick` instead of `convert` for complex compositions
- Using `-compose over` in separate commands
- Canvas creation with `xc:none` or `xc:transparent` in separate steps

#### Key Principles
1. **Single Command**: Do entire composition in one `convert` command
2. **Canvas First**: Start with `canvas:transparent` of final size
3. **Calculate Positions**: For N-pixel overlap with sprite width W: spacing = W - N
4. **Preserve Format**: Original RGBA sprites will maintain color in output
5. **Size Calculation**: 
   - Width: `(sprite_width * count) - (overlap * (count-1))`
   - Height: `(sprite_height * rows) - (vertical_overlap * (rows-1))`

#### Example Calculations
- Sprite: 841x798 pixels
- 400px horizontal overlap: spacing = 841 - 400 = 441px
- 600px vertical overlap: spacing = 798 - 600 = 198px
- 5 sprites width: (841 × 5) - (400 × 4) = 4205 - 1600 = 2605px
- 2 rows height: (798 × 2) - (600 × 1) = 1596 - 600 = 996px

## Bear Enemy System - Advanced Multi-State AI

The Bear class demonstrates a sophisticated enemy implementation with complex state management, 8-directional animations, and intelligent pathfinding. This serves as a template for creating advanced enemies with multiple behavioral states.

### Bear Architecture Overview

#### Core Components
- **State Machine**: 5 distinct states (IDLE, WAKING_UP, RUNNING, HIT, DYING)
- **8-Directional Movement**: Full directional awareness and animation
- **Smart Pathfinding**: Obstacle avoidance with alternative route finding
- **Complex Animation System**: State-specific frame counts and timing
- **Physics Integration**: Knockback, gravity, and collision detection

#### Asset Structure
```
assets/bear/
├── animations/
│   ├── idle/
│   │   ├── east/          # 11 frames (000-010)
│   │   └── west/          # 11 frames (000-010)
│   ├── wakingUp/
│   │   ├── east/          # 8 frames (000-007)
│   │   └── west/          # 8 frames (000-007)
│   ├── running/
│   │   ├── north/         # 4 frames (000-003)
│   │   ├── north-east/    # 4 frames (000-003)
│   │   ├── east/          # 4 frames (000-003)
│   │   ├── south-east/    # 4 frames (000-003)
│   │   ├── south/         # 4 frames (000-003)
│   │   ├── south-west/    # 4 frames (000-003)
│   │   ├── west/          # 4 frames (000-003)
│   │   └── north-west/    # 4 frames (000-003)
│   ├── hit/
│   │   ├── north/         # 12 frames (000-011)
│   │   ├── north-east/    # 12 frames (000-011)
│   │   ├── east/          # 12 frames (000-011)
│   │   ├── south-east/    # 12 frames (000-011)
│   │   ├── south/         # 12 frames (000-011)
│   │   ├── south-west/    # 12 frames (000-011)
│   │   ├── west/          # 12 frames (000-011)
│   │   └── north-west/    # 12 frames (000-011)
│   ├── attack-left/
│   │   └── west/          # 8 frames (000-007)
│   └── attack-right/
│       └── east/          # 8 frames (000-007)
└── rotations/             # Static directional sprites
    ├── north.png
    ├── north-east.png
    ├── east.png
    ├── south-east.png
    ├── south.png
    ├── south-west.png
    ├── west.png
    └── north-west.png
```

**Important Limitation**: Bears can only be initialized facing **east** or **west** directions. While they have full 8-directional movement and animation during gameplay, the initial facing direction in level configuration must be either "east" or "west" due to asset availability constraints.

### State Machine Implementation

#### State Definitions
```java
public enum BearState {
    IDLE,       // Sleeping, waiting for player detection
    WAKING_UP,  // Transition animation when player detected
    RUNNING,    // Active pursuit of player
    HIT,        // Damage reaction animation
    DYING       // Death animation with physics effects
}
```

#### State Transitions
- **IDLE → WAKING_UP**: Player enters sight range (250px) with line of sight
- **WAKING_UP → RUNNING**: After 0.9s wakeup animation completes
- **RUNNING → HIT**: When taking damage (if health > 0)
- **RUNNING → DYING**: When taking fatal damage (health ≤ 0)
- **HIT → RUNNING**: After 0.825s hit animation (if alive)
- **HIT → DYING**: After hit animation (if dead)

#### State-Specific Behavior

**IDLE State:**
- Ping-pong animation through 6 frames (0-5)
- Continuous player detection within sight range
- No movement or collision

**WAKING_UP State:**
- Linear animation through 6 frames once
- Fixed duration: 0.9 seconds
- Holds on final frame until transition

**RUNNING State:**
- Ping-pong animation through 4 frames (0-3)
- Active pathfinding and movement
- Player tracking and attack detection
- Smart obstacle avoidance

**HIT State:**
- Linear animation through 11 frames once
- Faster frame rate (0.075s vs 0.15s)
- Knockback physics applied
- Duration: 0.825 seconds

**DYING State:**
- Animation to frame 7, then fade over 2 seconds
- Complex physics: rotation, gravity, arc movement
- Gradual alpha fade after animation completes

### Animation System

#### Frame Management
```java
// State-specific frame counts
IDLE: 6 frames (ping-pong 0-5)
WAKING_UP: 6 frames (linear 0-5, hold on 5)
RUNNING: 4 frames (ping-pong 0-3)  
HIT: 11 frames (linear 0-11)
DYING: 8 frames (linear 0-7, hold on 7)
```

#### Timing Constants
```java
private static final float FRAME_DURATION = 0.15f;        // Normal speed
private static final float HIT_FRAME_DURATION = 0.075f;   // 2x faster for hit
private static final float WAKEUP_DURATION = 0.9f;        // 6 frames * 0.15s
private static final float HIT_DURATION = 0.825f;         // 11 frames * 0.075s
private static final float FADE_DURATION = 2.0f;          // Death fade time
```

#### Texture Loading Pattern
```java
// Texture naming convention: bear_{state}_{direction}_{frameNumber}
// Examples:
bearTextures.put("bear_idle_east_000", texture);
bearTextures.put("bear_running_north_002", texture);
bearTextures.put("bear_hit_south-west_007", texture);
```

### Movement and Pathfinding

#### Smart Pathfinding Algorithm
1. **Direct Path Check**: Test straight line to player
2. **Axis Separation**: If blocked, test X and Y movement separately
3. **Alternative Routes**: Choose unblocked axis for movement
4. **Perpendicular Fallback**: Try cardinal directions if both axes blocked
5. **Direction Commitment**: Stick to chosen direction for minimum time

#### Movement Constants
```java
private static final float SIGHT_RANGE = 250f;           // Detection range
private static final float ATTACK_RANGE = 30f;           // Damage range
private static final float MOVE_SPEED = 150f;            // Pixels per second
private static final float RANDOM_MOVE_INTERVAL = 1.0f;  // Path recalculation
private static final float MIN_DIRECTION_COMMIT_TIME = 0.3f; // Direction stability
```

#### Collision Detection
- **Sprite Hitbox**: 64x64 pixels (full sprite)
- **Movement Hitbox**: 64x32 pixels (bottom half)
- **Line of Sight**: Ray casting with 8-pixel steps

### Physics System

#### Knockback Mechanics
```java
// Normal hit knockback
Vec2 knockbackDirection = position.subtract(playerPosition);
knockbackVelocity = normalizedKnockback.multiply(KNOCKBACK_FORCE);

// Death knockback (3x stronger)
knockbackVelocity = normalizedKnockback.multiply(KNOCKBACK_FORCE * 3f);
```

#### Death Physics
```java
// Rotation based on facing direction
rotationSpeed = (facingDirection == Direction.RIGHT) ? 60f : -60f;

// Arc trajectory
verticalVelocity = INITIAL_JUMP_VELOCITY; // 150f
verticalVelocity -= GRAVITY * deltaTime;   // 300f gravity

// Friction effects
knockbackVelocity = knockbackVelocity.multiply(FRICTION); // 0.85f
rotationSpeed *= ROTATION_FRICTION; // 0.95f
```

### Damage System

#### Time-Based Damage
```java
// Applied in GameLoop.checkBearAttacks()
if (bear.isAttackingPlayer()) {
    float damageThisFrame = DAMAGE_PER_SECOND * GameConfig.DELTA_TIME;
    player.takeDamage((int)damageThisFrame); // 20 DPS
}
```

#### Damage States
- **Normal Hit**: Enter HIT state, apply knockback
- **Fatal Hit**: Skip HIT state, go directly to DYING
- **Attack Range**: 30 pixels from bear center

### Creating Similar Enemies

#### 1. Define Enemy State Enum
```java
public enum WolfState {
    SLEEPING, STALKING, POUNCING, HOWLING, RETREATING
}
```

#### 2. Set Up Asset Structure
```
assets/wolf/
├── animations/
│   ├── sleeping/
│   │   └── any/           # Omnidirectional
│   ├── stalking/
│   │   ├── north/         # 8 directions
│   │   ├── north-east/
│   │   └── ...
│   └── pouncing/
│       ├── north/
│       └── ...
└── rotations/             # Static sprites
```

#### 3. Implement State Machine
```java
private void updateState(float deltaTime) {
    switch (state) {
        case SLEEPING:
            // Detection logic
            break;
        case STALKING:
            // Stealth movement
            break;
        case POUNCING:
            // Attack animation
            break;
    }
}
```

#### 4. Configure Animation System
```java
// State-specific frame counts and timing
private static final int SLEEPING_FRAMES = 4;
private static final int STALKING_FRAMES = 6;
private static final int POUNCING_FRAMES = 8;
private static final float STALK_FRAME_DURATION = 0.2f;
private static final float POUNCE_FRAME_DURATION = 0.1f;
```

#### 5. Implement Movement Behavior
```java
private void updateMovement(float deltaTime) {
    switch (state) {
        case STALKING:
            // Slow, careful movement
            moveTowardsPlayer(deltaTime, STALK_SPEED);
            break;
        case POUNCING:
            // Fast, direct attack
            lungeAtPlayer(deltaTime, POUNCE_SPEED);
            break;
    }
}
```

#### 6. Add Texture Loading
```java
// In Game.loadTextures()
for (String state : Arrays.asList("sleeping", "stalking", "pouncing")) {
    for (String direction : getDirectionsForState(state)) {
        int frameCount = getFrameCountForState(state);
        for (int i = 0; i < frameCount; i++) {
            String key = String.format("wolf_%s_%s_%03d", state, direction, i);
            wolfTextures.put(key, renderer.loadTexture(
                String.format("assets/wolf/animations/%s/%s/%03d.png", 
                             state, direction, i)));
        }
    }
}
```

### Advanced Features

#### Multi-Directional Animation Support
- **8-Direction System**: Full directional awareness
- **State-Specific Directions**: Some states may use fewer directions
- **Fallback Directions**: Use closest available direction if exact match missing

#### Intelligent Pathfinding
- **Obstacle Avoidance**: Dynamic route calculation
- **Player Prediction**: Anticipate player movement
- **Group Coordination**: Multiple enemies can coordinate (future feature)

#### Physics Integration
- **Knockback Vectors**: Direction-based force application
- **Gravity Effects**: Realistic arc trajectories
- **Collision Response**: Proper collision handling during physics

#### Performance Optimization
- **State-Based Updates**: Only update relevant systems per state
- **Animation Caching**: Efficient texture lookup
- **Collision Culling**: Skip unnecessary collision checks

### Best Practices for Enemy Creation

1. **Start Simple**: Begin with 2-3 states, add complexity gradually
2. **Asset Organization**: Use consistent naming conventions
3. **State Transitions**: Keep transition logic clear and predictable
4. **Animation Timing**: Use constants for easy tweaking
5. **Physics Consistency**: Apply same physics rules across all enemies
6. **Performance**: Profile complex enemies, optimize as needed
7. **Debugging**: Add state visualization for development
8. **Modularity**: Design for easy parameter adjustment

### Common Patterns

#### Detection Systems
```java
// Range-based detection
float distance = position.distance(playerPosition);
if (distance <= DETECTION_RANGE && hasLineOfSight()) {
    triggerAlert();
}

// Angle-based detection (cone vision)
Vec2 toPlayer = playerPosition.subtract(position);
float angleToPlayer = Math.atan2(toPlayer.y(), toPlayer.x());
float angleDiff = Math.abs(angleToPlayer - facingAngle);
if (angleDiff <= VISION_CONE_ANGLE) {
    detectPlayer();
}
```

#### Attack Patterns
```java
// Melee attack (proximity-based)
if (distanceToPlayer <= ATTACK_RANGE) {
    dealDamageOverTime();
}

// Ranged attack (projectile-based)
if (canSeePlayer() && attackCooldownReady()) {
    fireProjectile(calculateLeadTarget());
}

// Area attack (explosion-based)
if (shouldExplode()) {
    createExplosion(position, EXPLOSION_RADIUS);
}
```

#### Movement Behaviors
```java
// Pursuit (direct chase)
Vec2 direction = playerPosition.subtract(position).normalize();
velocity = direction.multiply(CHASE_SPEED);

// Patrol (predefined path)
if (reachedWaypoint()) {
    currentWaypoint = getNextWaypoint();
}
moveTowards(currentWaypoint);

// Flee (escape behavior)
Vec2 fleeDirection = position.subtract(playerPosition).normalize();
velocity = fleeDirection.multiply(FLEE_SPEED);
```

The Bear enemy system provides a comprehensive template for creating sophisticated AI enemies with complex behaviors, smooth animations, and realistic physics interactions.

## Auto-Aim System

The auto-aim system provides intelligent targeting assistance for the player, automatically aiming at the closest enemy within range when using spacebar shooting. This system integrates seamlessly with the existing shooting mechanics while providing tactical advantages.

### Auto-Aim Architecture

#### Input System Integration
- **Spacebar**: Auto-aim shooting (separate from J key jumping)
- **Mouse**: Manual aiming (unchanged)
- **Dual Mode Support**: Normal bullets (single shot) vs Special bullets (rapid fire)

#### Core Components
```java
// Input handling chain
Game.java (GLFW callbacks) → InputHandler → InputState → GameLoop → handleShooting()

// Key input variables
private boolean spacePressed = false;  // Single press detection
private boolean spaceHeld = false;     // Continuous hold detection
```

### Auto-Aim Algorithm

#### Target Selection Process
1. **Scan All Enemies**: Iterate through all GameObjects with Trackable + Damageable interfaces
2. **Range Filtering**: Only consider enemies within 320 pixel range (same as turret sight range)
3. **Distance Calculation**: Use Vec2.distance() for accurate measurement
4. **Closest Selection**: Choose enemy with minimum distance to player
5. **Fallback Behavior**: If no enemies in range, shoot in player's current facing direction

#### Implementation
```java
private void handleShooting(InputState input) {
    boolean canRapidFire = player.hasSpecialBullets();
    boolean shouldAutoAim = canRapidFire ? input.spaceHeld : input.spacePressed;
    
    if (shouldAutoAim && player.canShoot()) {
        // Cooldown management
        long now = System.currentTimeMillis();
        if (canRapidFire) {
            if (now - lastPlayerShootTime < 100) return; // 10 shots/sec
        } else {
            if (now - lastPlayerShootTime < 200) return; // Prevent multi-shot
        }
        lastPlayerShootTime = now;
        
        // Target acquisition
        GameObject closestEnemy = null;
        float closestDistance = Float.MAX_VALUE;
        
        for (GameObject gameObject : gameObjects) {
            if (gameObject instanceof Trackable && gameObject instanceof Damageable) {
                Damageable damageable = (Damageable) gameObject;
                
                if (!damageable.isDestroyed()) {
                    float distance = new Vec2(gameObject.getX(), gameObject.getY())
                        .distance(new Vec2(player.getX(), player.getY()));
                    if (distance <= 320 && distance < closestDistance) {
                        closestDistance = distance;
                        closestEnemy = gameObject;
                    }
                }
            }
        }
        
        // Angle calculation
        double angle;
        Direction shootDirection;
        
        if (closestEnemy != null) {
            // Aim at enemy
            double deltaX = closestEnemy.getX() - player.getX();
            double deltaY = closestEnemy.getY() - player.getY();
            angle = Math.atan2(deltaY, deltaX);
            shootDirection = Player.calculateDirectionFromAngle(angle);
        } else {
            // Fallback to facing direction
            shootDirection = player.getCurrentDirection();
            angle = getAngleFromDirection(shootDirection);
        }
        
        // Execute shot
        player.setShootingDirection(shootDirection);
        player.shoot();
        float[] gunPos = player.getGunBarrelPosition();
        bullets.add(new Bullet(gunPos[0], gunPos[1], angle, true, player.hasSpecialBullets()));
        shells.add(new ShellCasing(player.getX(), player.getY()));
    }
}
```

### Direction-to-Angle Conversion

#### Helper Method for Fallback Shooting
```java
private double getAngleFromDirection(Direction direction) {
    switch (direction) {
        case UP: return Math.PI / 2;           // 90 degrees
        case UP_RIGHT: return Math.PI / 4;     // 45 degrees
        case RIGHT: return 0;                  // 0 degrees
        case DOWN_RIGHT: return -Math.PI / 4;  // -45 degrees
        case DOWN: return -Math.PI / 2;        // -90 degrees
        case DOWN_LEFT: return -3 * Math.PI / 4; // -135 degrees
        case LEFT: return Math.PI;             // 180 degrees
        case UP_LEFT: return 3 * Math.PI / 4;  // 135 degrees
        default: return 0;
    }
}
```

### Cooldown Management

#### Preventing Multiple Shots
- **Normal Bullets**: 200ms cooldown prevents multiple shots on single spacebar press
- **Special Bullets**: 100ms cooldown allows 10 shots per second rapid fire
- **Shared Timing**: Uses same `lastPlayerShootTime` as mouse shooting for consistency

#### Rate Limiting Logic
```java
// Different timing for different bullet types
if (canRapidFire) {
    if (now - lastPlayerShootTime < 100) return; // Rapid fire
} else {
    if (now - lastPlayerShootTime < 200) return; // Single shot protection
}
```

### Auto-Aim vs Manual Aiming

#### Coexistence Design
- **Auto-aim (Spacebar)**: Intelligent targeting with fallback
- **Manual aim (Mouse)**: Precise directional control
- **Independent Systems**: Both can be used interchangeably
- **Shared Resources**: Same ammo, cooldown, and bullet creation

#### Usage Patterns
- **Close Combat**: Auto-aim for quick enemy engagement
- **Precision Shots**: Mouse for specific targeting
- **Rapid Fire**: Hold spacebar with special bullets for sustained auto-targeting

## Line of Sight System

The line of sight system determines whether enemies can detect and engage the player, and whether the player's auto-aim can target enemies. Multiple implementations exist for different enemy types and use cases.

### Line of Sight Implementations

#### 1. Simple Range-Based (GunTurret)
```java
@Override
public boolean canSeePlayer(float playerX, float playerY) {
    return position.distance(new Vec2(playerX, playerY)) <= MAX_SIGHT; // 320 pixels
}
```
- **Use Case**: Simple enemies that don't need obstacle awareness
- **Performance**: Very fast, single distance calculation
- **Behavior**: Ignores walls and obstacles

#### 2. Directional Vision Cone (GunTurret)
```java
@Override
public boolean canSeePlayerInCurrentDirection(float playerX, float playerY) {
    Vec2 playerPos = new Vec2(playerX, playerY);
    Vec2 delta = playerPos.subtract(position);
    double angle = delta.angle();
    double degrees = Math.toDegrees(angle);
    if (degrees < 0) degrees += 360;
    
    switch (facingDirection) {
        case RIGHT: return degrees >= 337.5 || degrees < 22.5;      // 45° cone
        case UP_RIGHT: return degrees >= 22.5 && degrees < 67.5;
        case UP: return degrees >= 67.5 && degrees < 112.5;
        case UP_LEFT: return degrees >= 112.5 && degrees < 157.5;
        case LEFT: return degrees >= 157.5 && degrees < 202.5;
        case DOWN_LEFT: return degrees >= 202.5 && degrees < 247.5;
        case DOWN: return degrees >= 247.5 && degrees < 292.5;
        case DOWN_RIGHT: return degrees >= 292.5 && degrees < 337.5;
        default: return false;
    }
}
```
- **Use Case**: Directional enemies like turrets with limited field of view
- **Vision Cone**: 45-degree field of view per direction
- **Behavior**: Only detects player within facing direction

#### 3. Ray Casting with Obstacles (Bear)
```java
private boolean hasLineOfSight() {
    if (collidableObjects == null) return true;
    
    // Cast a ray from bear to player
    Vec2 direction = playerPosition.subtract(position);
    float distance = direction.distance(new Vec2(0, 0));
    
    if (distance == 0) return true;
    
    Vec2 normalizedDirection = direction.multiply(1.0f / distance);
    
    // Step along the ray and check for collisions
    float stepSize = 8.0f; // Check every 8 pixels
    int steps = (int) (distance / stepSize);
    
    for (int i = 1; i < steps; i++) {
        Vec2 rayPoint = position.add(normalizedDirection.multiply(i * stepSize));
        
        // Check if this point collides with any blocking object
        for (GameObject obj : collidableObjects) {
            if (obj != this && obj instanceof Collidable && 
                ((Collidable) obj).checkMovementCollision(rayPoint.x() - 1, rayPoint.y() - 1, 2, 2)) {
                return false; // Line of sight blocked
            }
        }
    }
    
    return true; // Clear line of sight
}
```
- **Use Case**: Advanced enemies that respect obstacles and terrain
- **Ray Casting**: Steps along line from enemy to player
- **Obstacle Detection**: Checks collision with all Collidable objects
- **Performance**: More expensive but realistic

### Line of Sight Parameters

#### Vision Range Constants
```java
// Different ranges for different enemy types
private static final float SIGHT_RANGE = 250f;    // Bear detection range
private static final float MAX_SIGHT = 320f;      // Turret sight range
private static final float ATTACK_RANGE = 30f;    // Bear attack range
```

#### Ray Casting Configuration
```java
private static final float STEP_SIZE = 8.0f;      // Ray casting precision
// Smaller step size = more accurate but slower
// Larger step size = faster but may miss thin obstacles
```

#### Vision Cone Angles
```java
// 8-directional vision cones (45 degrees each)
RIGHT: 337.5° - 22.5°     // East
UP_RIGHT: 22.5° - 67.5°   // Northeast  
UP: 67.5° - 112.5°        // North
UP_LEFT: 112.5° - 157.5°  // Northwest
LEFT: 157.5° - 202.5°     // West
DOWN_LEFT: 202.5° - 247.5° // Southwest
DOWN: 247.5° - 292.5°     // South
DOWN_RIGHT: 292.5° - 337.5° // Southeast
```

### Angle Calculation System

#### Vec2.angle() Method
```java
// Calculates angle from vector components
double angle = Math.atan2(deltaY, deltaX);
double degrees = Math.toDegrees(angle);
if (degrees < 0) degrees += 360; // Normalize to 0-360 range
```

#### Direction Mapping
- **0°**: East (RIGHT)
- **45°**: Northeast (UP_RIGHT)
- **90°**: North (UP)
- **135°**: Northwest (UP_LEFT)
- **180°**: West (LEFT)
- **225°**: Southwest (DOWN_LEFT)
- **270°**: South (DOWN)
- **315°**: Southeast (DOWN_RIGHT)

### Performance Considerations

#### Optimization Strategies
1. **Range Check First**: Always check distance before expensive operations
2. **Early Exit**: Return false immediately when obstacle found
3. **Step Size Tuning**: Balance accuracy vs performance
4. **Collision Culling**: Only check relevant collidable objects
5. **Update Frequency**: Don't recalculate every frame if not needed

#### Performance Comparison
```java
// Performance ranking (fastest to slowest)
1. Simple Range: O(1) - single distance calculation
2. Directional Cone: O(1) - distance + angle calculation  
3. Ray Casting: O(n*m) - n steps × m collidable objects
```

### Integration with Auto-Aim

#### Simplified Auto-Aim Line of Sight
The auto-aim system uses a simplified approach for performance:
```java
// Auto-aim uses distance-only for performance
if (distance <= 320 && distance < closestDistance) {
    closestDistance = distance;
    closestEnemy = gameObject;
}
```

#### Why Simplified?
- **Performance**: Auto-aim runs every frame during shooting
- **User Experience**: Players expect responsive targeting
- **Game Balance**: Obstacles shouldn't completely disable auto-aim
- **Consistency**: Matches turret sight range (320 pixels)

### Implementing Custom Line of Sight

#### Basic Template
```java
@Override
public boolean canSeePlayer(float playerX, float playerY) {
    // 1. Range check
    float distance = position.distance(new Vec2(playerX, playerY));
    if (distance > SIGHT_RANGE) return false;
    
    // 2. Additional checks (choose one or combine)
    
    // Option A: Simple (always true if in range)
    return true;
    
    // Option B: Directional cone
    return isPlayerInVisionCone(playerX, playerY);
    
    // Option C: Ray casting
    return hasLineOfSightToPlayer(playerX, playerY);
    
    // Option D: Combined
    return isPlayerInVisionCone(playerX, playerY) && hasLineOfSightToPlayer(playerX, playerY);
}
```

#### Custom Vision Cone
```java
private boolean isPlayerInVisionCone(float playerX, float playerY) {
    Vec2 toPlayer = new Vec2(playerX, playerY).subtract(position);
    double angleToPlayer = Math.atan2(toPlayer.y(), toPlayer.x());
    double facingAngle = getFacingAngleRadians();
    
    double angleDiff = Math.abs(angleToPlayer - facingAngle);
    if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff; // Handle wrap-around
    
    return angleDiff <= VISION_CONE_HALF_ANGLE; // e.g., Math.PI / 4 for 90° cone
}
```

The line of sight system provides flexible detection mechanics that can be customized for different enemy types, from simple range-based detection to complex ray casting with obstacle awareness.

## Level Configuration System

The game uses a JSON-based level loading system that allows easy creation and modification of game levels without code changes. Each level defines enemy positions, foliage placement, power-ups, and environmental settings.

### Level File Structure

#### Basic Level Template
```json
{
  "backgroundTexture": "desert.png",
  "mapWidth": 3200,
  "mapHeight": 1800,
  "turrets": [
    {"x": 500, "y": 300},
    {"x": 1500, "y": 200}
  ],
  "foliage": [
    {
      "x": 700, "y": 500,
      "width": 120, "height": 120,
      "spriteCollisionWidth": 40, "spriteCollisionHeight": 45,
      "movementCollisionWidth": 50, "movementCollisionHeight": 30,
      "textureKey": "palm_trees",
      "renderOffset": 30
    }
  ],
  "ammoPowerUps": [
    {"x": 800, "y": 400}
  ],
  "bears": [
    {"x": 1200, "y": 1000, "facing": "west"},
    {"x": 2200, "y": 300, "facing": "east"}
  ],
  "player": {
    "x": 200, "y": 200
  }
}
```

### Configuration Parameters

#### Map Settings
- **backgroundTexture**: Texture file name for level background
- **mapWidth**: Level width in pixels
- **mapHeight**: Level height in pixels

#### Enemy Configuration

**Turrets**:
```json
{"x": 500, "y": 300}
```
- Simple position-only configuration
- Auto-initialized with default settings

**Bears**:
```json
{"x": 1200, "y": 1000, "facing": "west"}
```
- **x, y**: Position coordinates
- **facing**: Initial direction ("east" or "west" only)
- **Limitation**: Bears can only start facing east or west due to asset constraints

#### Foliage Configuration
```json
{
  "x": 700, "y": 500,                    // Position
  "width": 120, "height": 120,           // Sprite dimensions
  "spriteCollisionWidth": 40,            // Bullet collision width
  "spriteCollisionHeight": 45,           // Bullet collision height
  "movementCollisionWidth": 50,          // Movement blocking width
  "movementCollisionHeight": 30,         // Movement blocking height
  "textureKey": "palm_trees",            // Texture identifier
  "renderOffset": 30                     // Y-offset for depth sorting
}
```

#### Available Foliage Types
- **"foliage"**: Small bushes (50x50 pixels)
- **"palm_trees"**: Individual palm trees (120x120 pixels)
- **"palm_trees_group"**: Large palm tree clusters (500x190 pixels)

#### Power-Up Configuration
```json
{"x": 800, "y": 400}
```
- **ammoPowerUps**: Provides special bullets when collected

### Level Examples

#### Level 2 (Desert Theme)
- **Background**: Desert terrain
- **Size**: 3200x1800 pixels (large map)
- **Enemies**: 3 turrets + 3 bears
- **Features**: Multiple palm tree groups, varied foliage
- **Difficulty**: High (multiple bears + large area)

#### Level 3 (Grass Theme)
- **Background**: Vibrant grass
- **Size**: 800x500 pixels (small map)
- **Enemies**: 2 turrets + 1 bear
- **Features**: Minimal foliage, close quarters
- **Difficulty**: Medium (confined space + bear)

### Level Loading System

#### File Location
```
src/main/resources/maps/
├── level1.json
├── level2.json
└── level3.json
```

#### Loading Process
1. **LevelSelectState**: User selects level
2. **GameObjectFactory.loadLevel()**: Loads JSON file
3. **MapLoader.parseJson()**: Parses configuration
4. **GameLoop**: Initializes objects from parsed data

#### Error Handling
- **File Not Found**: Falls back to hardcoded default level
- **Parse Errors**: Uses default values for missing fields
- **Invalid Data**: Skips malformed entries, continues loading

### Creating New Levels

#### Step 1: Create JSON File
```bash
# Create new level file
touch src/main/resources/maps/level4.json
```

#### Step 2: Define Level Structure
```json
{
  "backgroundTexture": "vibrant_random_grass.png",
  "mapWidth": 1600,
  "mapHeight": 900,
  "turrets": [
    {"x": 400, "y": 200},
    {"x": 1200, "y": 700}
  ],
  "foliage": [],
  "ammoPowerUps": [
    {"x": 800, "y": 450}
  ],
  "bears": [
    {"x": 600, "y": 300, "facing": "east"}
  ],
  "player": {
    "x": 100, "y": 100
  }
}
```

#### Step 3: Update Level Selection
Add level button to LevelSelectState for new level access.

### Design Guidelines

#### Enemy Placement
- **Turret Spacing**: Minimum 300 pixels apart to avoid overlap
- **Bear Positioning**: Place away from player spawn (minimum 400 pixels)
- **Line of Sight**: Consider obstacles when placing enemies
- **Difficulty Scaling**: More enemies = higher difficulty

#### Foliage Placement
- **Strategic Blocking**: Use large foliage to create cover and chokepoints
- **Visual Variety**: Mix different foliage types for visual interest
- **Movement Flow**: Don't block all paths, allow multiple routes
- **Collision Considerations**: Ensure movement hitboxes don't create impossible passages

#### Map Sizing
- **Small Maps** (800x600): Close combat, high intensity
- **Medium Maps** (1600x1200): Balanced gameplay
- **Large Maps** (3200x1800): Exploration, strategic positioning

#### Power-Up Distribution
- **Ammo Crates**: Place strategically to encourage exploration
- **Risk/Reward**: Position near enemies or in dangerous areas
- **Accessibility**: Ensure all power-ups are reachable

### Level Configuration Best Practices

1. **Test Early**: Load and test levels frequently during creation
2. **Player Spawn**: Always place player away from immediate danger
3. **Escape Routes**: Provide multiple paths for tactical movement
4. **Visual Balance**: Distribute foliage evenly across the map
5. **Performance**: Large maps with many objects may impact performance
6. **Asset Validation**: Ensure all textureKey references exist
7. **Coordinate Validation**: Keep all positions within map bounds

The level configuration system provides flexible, data-driven level design that supports rapid iteration and easy content creation without code modifications.
