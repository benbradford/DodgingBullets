# DodgingBullets Project Steering Guide

## Project Overview
DodgingBullets is a 2D top-down Java game built with LWJGL/OpenGL featuring:
- Player character with 8-directional movement and mouse-controlled shooting
- Multiple intelligent turret enemies with AI behavior
- Health/ammo systems with regeneration mechanics
- Grid-based collision detection with dual hitbox system
- Interface-based GameObject architecture for extensibility

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
