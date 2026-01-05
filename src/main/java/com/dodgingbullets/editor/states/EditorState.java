package com.dodgingbullets.editor.states;

import com.dodgingbullets.core.GameConfig;
import com.dodgingbullets.core.GameObjectFactory;
import com.dodgingbullets.core.Vec2;
import com.dodgingbullets.core.Renderer;
import com.dodgingbullets.core.Texture;
import com.dodgingbullets.editor.EditorInputState;
import com.dodgingbullets.editor.LevelData;
import com.dodgingbullets.editor.MapLoader;
import com.dodgingbullets.gameobjects.GameObject;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class EditorState implements EditorGameState {
    public enum EditorMode {
        SELECT, DELETE, PLACE
    }
    
    public enum PlaceableType {
        TURRET, BEAR, FOLIAGE, AMMO_POWERUP, THROWER
    }
    
    public enum FoliageTextureType {
        FOLIAGE("foliage", 50, 50, 50, 30, 50, 25, 0),
        PALM_TREES("palm_trees", 120, 120, 40, 45, 50, 30, 30),
        PALM_TREES_GROUP("palm_trees_group", 500, 190, 380, 120, 430, 120, 50),
        PALM_TREES_GROUP_LONG("palm_trees_group_long", 1403, 199, 1140, 120, 1290, 120, 50),
        PALM_TREES_GROUP_VERTICAL("palm_trees_group_vertical", 500, 557, 380, 480, 430, 480, 50),
        PALM_TREES_GROUP_VERTICAL_LONG("palm_trees_group_vertical_long", 500, 1510, 380, 1440, 430, 1440, 50);
        
        public final String textureKey;
        public final int width, height, spriteCollisionWidth, spriteCollisionHeight;
        public final int movementCollisionWidth, movementCollisionHeight, renderOffset;
        
        FoliageTextureType(String textureKey, int width, int height, int scw, int sch, int mcw, int mch, int renderOffset) {
            this.textureKey = textureKey;
            this.width = width; this.height = height;
            this.spriteCollisionWidth = scw; this.spriteCollisionHeight = sch;
            this.movementCollisionWidth = mcw; this.movementCollisionHeight = mch;
            this.renderOffset = renderOffset;
        }
    }

    private EditorStateManager stateManager;
    private Renderer renderer;
    private String levelFile;
    private EditorLevelSelectState levelSelectState;
    
    private EditorMode currentMode = EditorMode.SELECT;
    private PlaceableType selectedPlaceableType = PlaceableType.TURRET;
    private FoliageTextureType selectedFoliageTexture = FoliageTextureType.FOLIAGE;
    private boolean hasUnsavedChanges = false;
    private boolean showMapConfig = false;
    
    private static final String[] BACKGROUND_TEXTURES = {
        "vibrant_random_grass.png",
        "desert.png",
        "grass02.png"
    };
    private int selectedBackgroundIndex = 0;
    
    private LevelData levelData;
    private List<GameObject> gameObjects;
    private GameObject selectedObject;
    private boolean playerSelected = false;
    private boolean dragging = false;
    private Vec2 dragOffset = new Vec2(0, 0);
    
    private Vec2 cameraOffset = new Vec2(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2);
    private Map<String, Texture> textures = new HashMap<>();
    private Texture backgroundTexture;
    private Map<GameObject, LevelData.FoliageData> originalFoliageData = new HashMap<>();
    private Map<GameObject, String> originalFacingData = new HashMap<>();

    public EditorState(EditorStateManager stateManager, Renderer renderer, String levelFile, EditorLevelSelectState levelSelectState) {
        this.stateManager = stateManager;
        this.renderer = renderer;
        this.levelFile = levelFile;
        this.levelSelectState = levelSelectState;
        this.gameObjects = new ArrayList<>();
    }

    @Override
    public void enter() {
        loadLevel();
        loadTextures();
    }

    @Override
    public void update(float deltaTime, EditorInputState inputState) {
        handleInput(inputState);
        updateCamera(deltaTime, inputState);
    }

    @Override
    public void render(Renderer renderer) {
        renderer.clear();
        
        // Render background texture covering entire map
        if (levelData != null) {
            if (backgroundTexture != null) {
                // Render as single texture covering entire map (like the game)
                float renderX = -cameraOffset.x() + GameConfig.SCREEN_WIDTH / 2;
                float renderY = -cameraOffset.y() + GameConfig.SCREEN_HEIGHT / 2;
                renderer.render(backgroundTexture, renderX, renderY, levelData.mapWidth, levelData.mapHeight);
            } else {
                // Fallback to colored background
                renderer.renderRect(-cameraOffset.x() + GameConfig.SCREEN_WIDTH / 2, -cameraOffset.y() + GameConfig.SCREEN_HEIGHT / 2, 
                    levelData.mapWidth, levelData.mapHeight, 0.2f, 0.6f, 0.2f, 1.0f);
            }
        }
        
        // Render player if position is defined
        if (levelData != null && levelData.player != null) {
            float playerRenderX = levelData.player.x - cameraOffset.x() + GameConfig.SCREEN_WIDTH / 2;
            float playerRenderY = levelData.player.y - cameraOffset.y() + GameConfig.SCREEN_HEIGHT / 2;
            
            // Highlight selected player
            if (playerSelected) {
                renderer.renderRect(playerRenderX - 5, playerRenderY - 5, 74, 74, 1.0f, 1.0f, 0.0f, 0.5f);
            }
            
            Texture playerTexture = textures.get("player");
            if (playerTexture != null) {
                renderer.render(playerTexture, playerRenderX, playerRenderY, 64, 64);
            } else {
                // Fallback to blue rectangle
                renderer.renderRect(playerRenderX, playerRenderY, 64, 64, 0.0f, 0.0f, 1.0f, 1.0f);
            }
        }
        
        // Render game objects
        for (GameObject obj : gameObjects) {
            // Match game's rendering: camera offset + screen center offset
            float renderX = obj.getX() - cameraOffset.x() + GameConfig.SCREEN_WIDTH / 2;
            float renderY = obj.getY() - cameraOffset.y() + GameConfig.SCREEN_HEIGHT / 2;
            
            // Highlight selected object
            if (obj == selectedObject) {
                float highlightWidth = 64;
                float highlightHeight = 64;
                float highlightX = renderX;
                float highlightY = renderY;
                
                // Use actual object dimensions
                if ("gunturret".equals(getObjectType(obj))) {
                    highlightWidth = 128;
                    highlightHeight = 128;
                    highlightX -= 32; // Account for centering offset
                    highlightY -= 32;
                } else if ("foliage".equals(getObjectType(obj)) && obj instanceof com.dodgingbullets.gameobjects.environment.Foliage) {
                    com.dodgingbullets.gameobjects.environment.Foliage foliage = 
                        (com.dodgingbullets.gameobjects.environment.Foliage) obj;
                    highlightWidth = foliage.getSpriteWidth();
                    highlightHeight = foliage.getSpriteHeight();
                    highlightX -= highlightWidth / 2; // Account for centering
                    highlightY -= highlightHeight / 2;
                }
                
                renderer.renderRect(highlightX - 5, highlightY - 5, highlightWidth + 10, highlightHeight + 10, 1.0f, 1.0f, 0.0f, 0.5f);
            }
            
            // Render object with proper texture
            String objectType = getObjectType(obj);
            String textureKey = objectType;
            float renderWidth = 64;
            float renderHeight = 64;
            
            // Match game rendering sizes
            if ("gunturret".equals(objectType)) {
                renderWidth = 128;
                renderHeight = 128;
                renderX -= 32; // Center the larger turret
                renderY -= 32;
            }
            
            // For foliage, get the actual texture key and dimensions from the object
            if ("foliage".equals(objectType) && obj instanceof com.dodgingbullets.gameobjects.environment.Foliage) {
                com.dodgingbullets.gameobjects.environment.Foliage foliage = 
                    (com.dodgingbullets.gameobjects.environment.Foliage) obj;
                textureKey = foliage.getTextureKey();
                renderWidth = foliage.getSpriteWidth();
                renderHeight = foliage.getSpriteHeight();
                // Center foliage like the game does
                renderX -= renderWidth / 2;
                renderY -= renderHeight / 2;
            }
            
            Texture texture = textures.get(textureKey);
            if (texture != null) {
                renderer.render(texture, renderX, renderY, renderWidth, renderHeight);
            } else {
                // Fallback to colored rectangle
                float[] color = getObjectColor(objectType);
                renderer.renderRect(renderX, renderY, 64, 64, color[0], color[1], color[2], 1.0f);
            }
        }
        
        // Draw map border on top of everything
        float borderX = -cameraOffset.x();
        float borderY = -cameraOffset.y();
        float borderWidth = levelData.mapWidth;
        float borderHeight = levelData.mapHeight;
        
        // Draw thick black border (8px thick rectangles forming a frame)
        renderer.renderRect(borderX - 8, borderY - 8, borderWidth + 16, 8, 0.0f, 0.0f, 0.0f, 1.0f); // Top
        renderer.renderRect(borderX - 8, borderY + borderHeight, borderWidth + 16, 8, 0.0f, 0.0f, 0.0f, 1.0f); // Bottom
        renderer.renderRect(borderX - 8, borderY, 8, borderHeight, 0.0f, 0.0f, 0.0f, 1.0f); // Left
        renderer.renderRect(borderX + borderWidth, borderY, 8, borderHeight, 0.0f, 0.0f, 0.0f, 1.0f); // Right
        
        // Render UI
        renderUI(renderer);
        
        renderer.present();
    }

    @Override
    public void exit() {
    }

    private void handleInput(EditorInputState inputState) {
        if (inputState.mousePressed) {
            // Check UI buttons first
            if (handleUIClick(inputState.mouseX, inputState.mouseY)) {
                return;
            }
            
            Vec2 worldPos = screenToWorld(inputState.mouseX, inputState.mouseY);
            
            switch (currentMode) {
                case SELECT:
                    handleSelectMode(worldPos, inputState);
                    break;
                case DELETE:
                    handleDeleteMode(worldPos);
                    break;
                case PLACE:
                    handlePlaceMode(worldPos);
                    break;
            }
        }
        
        // Handle dragging
        if (currentMode == EditorMode.SELECT && dragging) {
            Vec2 worldPos = screenToWorld(inputState.mouseX, inputState.mouseY);
            Vec2 newPos = worldPos.subtract(dragOffset);
            
            // Clamp position to map bounds
            if (levelData != null) {
                newPos = new Vec2(
                    Math.max(0, Math.min(levelData.mapWidth, newPos.x())),
                    Math.max(0, Math.min(levelData.mapHeight, newPos.y()))
                );
            }
            
            if (playerSelected) {
                levelData.player.x = (int) Math.round(newPos.x());
                levelData.player.y = (int) Math.round(newPos.y());
                hasUnsavedChanges = true;
            } else if (selectedObject != null) {
                selectedObject.setPosition(newPos);
                hasUnsavedChanges = true;
            }
        }
        
        if (!inputState.mouseHeld) {
            dragging = false;
            playerSelected = false;
        }
    }

    private boolean handleUIClick(float mouseX, float mouseY) {
        // Mode buttons
        if (isPointInRect(mouseX, mouseY, 10, 350, 80, 30)) {
            currentMode = EditorMode.SELECT;
            return true;
        }
        if (isPointInRect(mouseX, mouseY, 10, 300, 80, 30)) {
            currentMode = EditorMode.DELETE;
            return true;
        }
        if (isPointInRect(mouseX, mouseY, 10, 250, 80, 30)) {
            currentMode = EditorMode.PLACE;
            return true;
        }
        
        // Placeable type buttons (only in PLACE mode)
        if (currentMode == EditorMode.PLACE) {
            if (isPointInRect(mouseX, mouseY, 10, 200, 80, 30)) {
                selectedPlaceableType = PlaceableType.TURRET;
                return true;
            }
            if (isPointInRect(mouseX, mouseY, 10, 150, 80, 30)) {
                selectedPlaceableType = PlaceableType.BEAR;
                return true;
            }
            if (isPointInRect(mouseX, mouseY, 10, 100, 80, 30)) {
                selectedPlaceableType = PlaceableType.FOLIAGE;
                return true;
            }
            
            // Foliage texture selection (only when FOLIAGE is selected)
            if (selectedPlaceableType == PlaceableType.FOLIAGE) {
                if (isPointInRect(mouseX, mouseY, 100, 170, 100, 30)) {
                    selectedFoliageTexture = FoliageTextureType.FOLIAGE;
                    return true;
                }
                if (isPointInRect(mouseX, mouseY, 100, 135, 100, 30)) {
                    selectedFoliageTexture = FoliageTextureType.PALM_TREES;
                    return true;
                }
                if (isPointInRect(mouseX, mouseY, 100, 100, 100, 30)) {
                    selectedFoliageTexture = FoliageTextureType.PALM_TREES_GROUP;
                    return true;
                }
                if (isPointInRect(mouseX, mouseY, 100, 65, 100, 30)) {
                    selectedFoliageTexture = FoliageTextureType.PALM_TREES_GROUP_LONG;
                    return true;
                }
                if (isPointInRect(mouseX, mouseY, 100, 30, 100, 30)) {
                    selectedFoliageTexture = FoliageTextureType.PALM_TREES_GROUP_VERTICAL;
                    return true;
                }
                if (isPointInRect(mouseX, mouseY, 210, 170, 100, 30)) {
                    selectedFoliageTexture = FoliageTextureType.PALM_TREES_GROUP_VERTICAL_LONG;
                    return true;
                }
            }
            if (isPointInRect(mouseX, mouseY, 10, 50, 80, 30)) {
                selectedPlaceableType = PlaceableType.AMMO_POWERUP;
                return true;
            }
            if (isPointInRect(mouseX, mouseY, 10, 10, 80, 30)) {
                selectedPlaceableType = PlaceableType.THROWER;
                return true;
            }
        }
        
        // Save button
        if (isPointInRect(mouseX, mouseY, 600, 350, 80, 30) && hasUnsavedChanges) {
            saveLevel();
            return true;
        }
        
        // Map Config button
        if (isPointInRect(mouseX, mouseY, 600, 310, 80, 30)) {
            showMapConfig = !showMapConfig;
            return true;
        }
        
        // Map config controls (when visible)
        if (showMapConfig) {
            // Width buttons
            if (isPointInRect(mouseX, mouseY, 220, 350, 30, 20)) {
                levelData.mapWidth = Math.max(1000, levelData.mapWidth - 100);
                hasUnsavedChanges = true;
                return true;
            }
            if (isPointInRect(mouseX, mouseY, 370, 350, 30, 20)) {
                levelData.mapWidth = Math.min(5000, levelData.mapWidth + 100);
                hasUnsavedChanges = true;
                return true;
            }
            
            // Height buttons
            if (isPointInRect(mouseX, mouseY, 220, 320, 30, 20)) {
                levelData.mapHeight = Math.max(800, levelData.mapHeight - 100);
                hasUnsavedChanges = true;
                return true;
            }
            if (isPointInRect(mouseX, mouseY, 370, 320, 30, 20)) {
                levelData.mapHeight = Math.min(3000, levelData.mapHeight + 100);
                hasUnsavedChanges = true;
                return true;
            }
            
            // Background texture buttons
            if (isPointInRect(mouseX, mouseY, 220, 290, 30, 20)) {
                selectedBackgroundIndex = (selectedBackgroundIndex - 1 + BACKGROUND_TEXTURES.length) % BACKGROUND_TEXTURES.length;
                levelData.backgroundTexture = BACKGROUND_TEXTURES[selectedBackgroundIndex];
                loadBackgroundTexture();
                hasUnsavedChanges = true;
                return true;
            }
            if (isPointInRect(mouseX, mouseY, 370, 290, 30, 20)) {
                selectedBackgroundIndex = (selectedBackgroundIndex + 1) % BACKGROUND_TEXTURES.length;
                levelData.backgroundTexture = BACKGROUND_TEXTURES[selectedBackgroundIndex];
                loadBackgroundTexture();
                hasUnsavedChanges = true;
                return true;
            }
        }
        
        // Quit button
        if (isPointInRect(mouseX, mouseY, 600, 300, 80, 30)) {
            stateManager.setState(levelSelectState);
            return true;
        }
        
        return false;
    }

    private boolean isPointInRect(float x, float y, float rectX, float rectY, float width, float height) {
        return x >= rectX && x <= rectX + width && y >= rectY && y <= rectY + height;
    }

    private void saveLevel() {
        try {
            LevelData saveData = new LevelData();
            saveData.backgroundTexture = levelData.backgroundTexture;
            saveData.mapWidth = levelData.mapWidth;
            saveData.mapHeight = levelData.mapHeight;
            
            // Initialize lists in the EXACT order expected by core MapLoader
            saveData.turrets = new ArrayList<>();
            saveData.foliage = new ArrayList<>();
            saveData.ammoPowerUps = new ArrayList<>();
            saveData.bears = new ArrayList<>();
            saveData.throwers = new ArrayList<>();
            
            // Process all objects in one pass to maintain order
            for (GameObject obj : gameObjects) {
                String type = getObjectType(obj);
                switch (type) {
                    case "gunturret":
                        LevelData.TurretData turret = new LevelData.TurretData();
                        turret.x = (int) Math.round(obj.getX());
                        turret.y = (int) Math.round(obj.getY());
                        saveData.turrets.add(turret);
                        break;
                    case "foliage":
                        LevelData.FoliageData foliage = new LevelData.FoliageData();
                        foliage.x = (int) Math.round(obj.getX());
                        foliage.y = (int) Math.round(obj.getY());
                        
                        // Use original data if available, otherwise defaults
                        LevelData.FoliageData original = originalFoliageData.get(obj);
                        if (original != null) {
                            foliage.width = original.width;
                            foliage.height = original.height;
                            foliage.spriteCollisionWidth = original.spriteCollisionWidth;
                            foliage.spriteCollisionHeight = original.spriteCollisionHeight;
                            foliage.movementCollisionWidth = original.movementCollisionWidth;
                            foliage.movementCollisionHeight = original.movementCollisionHeight;
                            foliage.textureKey = original.textureKey;
                            foliage.renderOffset = original.renderOffset;
                        } else {
                            foliage.width = 64;
                            foliage.height = 64;
                            foliage.spriteCollisionWidth = 64;
                            foliage.spriteCollisionHeight = 64;
                            foliage.movementCollisionWidth = 64;
                            foliage.movementCollisionHeight = 32;
                            foliage.textureKey = "foliage";
                            foliage.renderOffset = 0;
                        }
                        saveData.foliage.add(foliage);
                        break;
                    case "ammopowerup":
                        LevelData.PowerUpData powerUp = new LevelData.PowerUpData();
                        powerUp.x = (int) Math.round(obj.getX());
                        powerUp.y = (int) Math.round(obj.getY());
                        saveData.ammoPowerUps.add(powerUp);
                        break;
                    case "bear":
                        LevelData.BearData bear = new LevelData.BearData();
                        bear.x = (int) Math.round(obj.getX());
                        bear.y = (int) Math.round(obj.getY());
                        bear.facing = originalFacingData.getOrDefault(obj, "east");
                        saveData.bears.add(bear);
                        break;
                    case "thrower":
                        LevelData.ThrowerData thrower = new LevelData.ThrowerData();
                        thrower.x = (int) Math.round(obj.getX());
                        thrower.y = (int) Math.round(obj.getY());
                        thrower.facing = originalFacingData.getOrDefault(obj, "east");
                        saveData.throwers.add(thrower);
                        break;
                }
            }
            
            // Keep existing player data
            saveData.player = levelData.player;
            
            MapLoader.saveLevel(levelFile, saveData);
            hasUnsavedChanges = false;
            System.out.println("Level saved: " + levelFile);
            
        } catch (Exception e) {
            System.err.println("Failed to save level: " + e.getMessage());
        }
    }

    private void handleSelectMode(Vec2 worldPos, EditorInputState inputState) {
        // Check if player was clicked first
        if (levelData != null && levelData.player != null) {
            if (worldPos.x() >= levelData.player.x && worldPos.x() <= levelData.player.x + 64 &&
                worldPos.y() >= levelData.player.y && worldPos.y() <= levelData.player.y + 64) {
                selectedObject = null;
                playerSelected = true;
                dragging = true;
                dragOffset = worldPos.subtract(new Vec2(levelData.player.x, levelData.player.y));
                return;
            }
        }
        
        // Check game objects
        GameObject clickedObject = findObjectAt(worldPos);
        if (clickedObject != null) {
            selectedObject = clickedObject;
            playerSelected = false;
            dragging = true;
            dragOffset = worldPos.subtract(new Vec2(clickedObject.getX(), clickedObject.getY()));
        } else {
            selectedObject = null;
            playerSelected = false;
        }
    }

    private void handleDeleteMode(Vec2 worldPos) {
        GameObject clickedObject = findObjectAt(worldPos);
        if (clickedObject != null) {
            gameObjects.remove(clickedObject);
            hasUnsavedChanges = true;
            if (selectedObject == clickedObject) {
                selectedObject = null;
            }
        }
    }

    private void handlePlaceMode(Vec2 worldPos) {
        GameObject newObject = createObjectOfType(selectedPlaceableType, worldPos.x(), worldPos.y());
        if (newObject != null) {
            gameObjects.add(newObject);
            hasUnsavedChanges = true;
        }
    }

    private GameObject findObjectAt(Vec2 worldPos) {
        for (GameObject obj : gameObjects) {
            float width = 64;
            float height = 64;
            float objX = obj.getX();
            float objY = obj.getY();
            
            // Use actual object dimensions and centering for foliage
            if ("foliage".equals(getObjectType(obj)) && obj instanceof com.dodgingbullets.gameobjects.environment.Foliage) {
                com.dodgingbullets.gameobjects.environment.Foliage foliage = 
                    (com.dodgingbullets.gameobjects.environment.Foliage) obj;
                width = foliage.getSpriteWidth();
                height = foliage.getSpriteHeight();
                objX -= width / 2; // Account for centering
                objY -= height / 2;
            } else if ("gunturret".equals(getObjectType(obj))) {
                width = 128;
                height = 128;
                objX -= 64; // Account for centering
                objY -= 64;
            }
            
            if (worldPos.x() >= objX && worldPos.x() <= objX + width &&
                worldPos.y() >= objY && worldPos.y() <= objY + height) {
                return obj;
            }
        }
        return null;
    }

    private GameObject createObjectOfType(PlaceableType type, float x, float y) {
        GameObject newObject;
        switch (type) {
            case TURRET:
                return GameObjectFactory.createTurret(x, y);
            case BEAR:
                newObject = GameObjectFactory.createBear(x, y, "east");
                originalFacingData.put(newObject, "east");
                return newObject;
            case FOLIAGE:
                FoliageTextureType texType = selectedFoliageTexture;
                newObject = GameObjectFactory.createFoliage(x, y, texType.width, texType.height,
                    texType.spriteCollisionWidth, texType.spriteCollisionHeight,
                    texType.movementCollisionWidth, texType.movementCollisionHeight,
                    texType.textureKey, texType.renderOffset);
                // Store data for new objects
                LevelData.FoliageData defaultData = new LevelData.FoliageData();
                defaultData.width = texType.width;
                defaultData.height = texType.height;
                defaultData.spriteCollisionWidth = texType.spriteCollisionWidth;
                defaultData.spriteCollisionHeight = texType.spriteCollisionHeight;
                defaultData.movementCollisionWidth = texType.movementCollisionWidth;
                defaultData.movementCollisionHeight = texType.movementCollisionHeight;
                defaultData.textureKey = texType.textureKey;
                defaultData.renderOffset = texType.renderOffset;
                originalFoliageData.put(newObject, defaultData);
                return newObject;
            case AMMO_POWERUP:
                return GameObjectFactory.createAmmoPowerUp(x, y);
            case THROWER:
                newObject = GameObjectFactory.createThrower(x, y);
                originalFacingData.put(newObject, "east");
                return newObject;
            default:
                return null;
        }
    }

    private void renderUI(Renderer renderer) {
        // Mode buttons
        renderModeButton(renderer, "SELECT", 10, 350, currentMode == EditorMode.SELECT);
        renderModeButton(renderer, "DELETE", 10, 300, currentMode == EditorMode.DELETE);
        renderModeButton(renderer, "PLACE", 10, 250, currentMode == EditorMode.PLACE);
        
        // Placeable type buttons (only show in PLACE mode)
        if (currentMode == EditorMode.PLACE) {
            renderPlaceableButton(renderer, "TURRET", 10, 200, selectedPlaceableType == PlaceableType.TURRET);
            renderPlaceableButton(renderer, "BEAR", 10, 150, selectedPlaceableType == PlaceableType.BEAR);
            renderPlaceableButton(renderer, "FOLIAGE", 10, 100, selectedPlaceableType == PlaceableType.FOLIAGE);
            renderPlaceableButton(renderer, "AMMO", 10, 50, selectedPlaceableType == PlaceableType.AMMO_POWERUP);
            renderPlaceableButton(renderer, "THROWER", 10, 10, selectedPlaceableType == PlaceableType.THROWER);
            
            // Foliage texture selection (only when FOLIAGE is selected)
            if (selectedPlaceableType == PlaceableType.FOLIAGE) {
                renderTextureButton(renderer, "foliage", 100, 170, selectedFoliageTexture == FoliageTextureType.FOLIAGE);
                renderTextureButton(renderer, "palm_trees", 100, 135, selectedFoliageTexture == FoliageTextureType.PALM_TREES);
                renderTextureButton(renderer, "palm_group", 100, 100, selectedFoliageTexture == FoliageTextureType.PALM_TREES_GROUP);
                renderTextureButton(renderer, "palm_long", 100, 65, selectedFoliageTexture == FoliageTextureType.PALM_TREES_GROUP_LONG);
                renderTextureButton(renderer, "palm_vert", 100, 30, selectedFoliageTexture == FoliageTextureType.PALM_TREES_GROUP_VERTICAL);
                renderTextureButton(renderer, "vert_long", 210, 170, selectedFoliageTexture == FoliageTextureType.PALM_TREES_GROUP_VERTICAL_LONG);
            }
        }
        
        // Save/Quit buttons
        float[] saveColor = hasUnsavedChanges ? new float[]{0.0f, 0.8f, 0.0f} : new float[]{0.3f, 0.3f, 0.3f};
        renderer.renderRect(600, 350, 80, 30, saveColor[0], saveColor[1], saveColor[2], 1.0f);
        renderer.renderText("SAVE", 610, 360, 1.0f, 1.0f, 1.0f);
        
        // Map Config button
        float[] configColor = showMapConfig ? new float[]{0.0f, 0.0f, 0.8f} : new float[]{0.3f, 0.3f, 0.3f};
        renderer.renderRect(600, 310, 80, 30, configColor[0], configColor[1], configColor[2], 1.0f);
        renderer.renderText("MAP", 615, 320, 1.0f, 1.0f, 1.0f);
        
        renderer.renderRect(600, 270, 80, 30, 0.8f, 0.0f, 0.0f, 1.0f);
        renderer.renderText("QUIT", 610, 280, 1.0f, 1.0f, 1.0f);
        
        // Map config controls (when visible)
        if (showMapConfig) {
            // Width controls
            renderer.renderRect(220, 350, 30, 20, 0.5f, 0.5f, 0.5f, 1.0f);
            renderer.renderText("-", 230, 355, 1.0f, 1.0f, 1.0f);
            renderer.renderText("W:" + levelData.mapWidth, 255, 355, 1.0f, 1.0f, 1.0f);
            renderer.renderRect(370, 350, 30, 20, 0.5f, 0.5f, 0.5f, 1.0f);
            renderer.renderText("+", 380, 355, 1.0f, 1.0f, 1.0f);
            
            // Height controls
            renderer.renderRect(220, 320, 30, 20, 0.5f, 0.5f, 0.5f, 1.0f);
            renderer.renderText("-", 230, 325, 1.0f, 1.0f, 1.0f);
            renderer.renderText("H:" + levelData.mapHeight, 255, 325, 1.0f, 1.0f, 1.0f);
            renderer.renderRect(370, 320, 30, 20, 0.5f, 0.5f, 0.5f, 1.0f);
            renderer.renderText("+", 380, 325, 1.0f, 1.0f, 1.0f);
            
            // Background texture controls
            renderer.renderRect(220, 290, 30, 20, 0.5f, 0.5f, 0.5f, 1.0f);
            renderer.renderText("<", 230, 295, 1.0f, 1.0f, 1.0f);
            String bgName = BACKGROUND_TEXTURES[selectedBackgroundIndex].replace(".png", "").substring(0, Math.min(8, BACKGROUND_TEXTURES[selectedBackgroundIndex].length() - 4));
            renderer.renderText("BG:" + bgName, 255, 295, 1.0f, 1.0f, 1.0f);
            renderer.renderRect(370, 290, 30, 20, 0.5f, 0.5f, 0.5f, 1.0f);
            renderer.renderText(">", 380, 295, 1.0f, 1.0f, 1.0f);
        }
    }

    private void renderModeButton(Renderer renderer, String text, float x, float y, boolean selected) {
        float[] color = selected ? new float[]{0.0f, 0.0f, 0.8f} : new float[]{0.3f, 0.3f, 0.3f};
        renderer.renderRect(x, y, 80, 30, color[0], color[1], color[2], 1.0f);
        renderer.renderText(text, x + 5, y + 10, 1.0f, 1.0f, 1.0f);
    }

    private void renderPlaceableButton(Renderer renderer, String text, float x, float y, boolean selected) {
        float[] color = selected ? new float[]{0.0f, 0.8f, 0.0f} : new float[]{0.3f, 0.3f, 0.3f};
        renderer.renderRect(x, y, 80, 30, color[0], color[1], color[2], 1.0f);
        renderer.renderText(text, x + 5, y + 10, 1.0f, 1.0f, 1.0f);
    }
    
    private void renderTextureButton(Renderer renderer, String text, float x, float y, boolean selected) {
        float[] color = selected ? new float[]{0.8f, 0.8f, 0.0f} : new float[]{0.2f, 0.2f, 0.2f};
        renderer.renderRect(x, y, 100, 30, color[0], color[1], color[2], 1.0f);
        renderer.renderText(text, x + 5, y + 10, 1.0f, 1.0f, 1.0f);
    }
    
    private void loadBackgroundTexture() {
        try {
            backgroundTexture = renderer.loadTexture("assets/" + levelData.backgroundTexture);
            System.out.println("Loaded background texture: " + levelData.backgroundTexture);
        } catch (Exception e) {
            System.err.println("Failed to load background texture: " + levelData.backgroundTexture + " - " + e.getMessage());
            backgroundTexture = null;
        }
    }

    private Vec2 screenToWorld(float screenX, float screenY) {
        // Match the game's coordinate system: add camera offset but subtract screen center
        return new Vec2(screenX + cameraOffset.x() - GameConfig.SCREEN_WIDTH / 2, 
                       screenY + cameraOffset.y() - GameConfig.SCREEN_HEIGHT / 2);
    }

    private void updateCamera(float deltaTime, EditorInputState inputState) {
        float cameraSpeed = 300f; // pixels per second
        float moveDistance = cameraSpeed * deltaTime;
        
        if (inputState.keys[0]) { // W - up
            cameraOffset = cameraOffset.add(new Vec2(0, moveDistance));
        }
        if (inputState.keys[2]) { // S - down
            cameraOffset = cameraOffset.add(new Vec2(0, -moveDistance));
        }
        if (inputState.keys[1]) { // A - left
            cameraOffset = cameraOffset.add(new Vec2(-moveDistance, 0));
        }
        if (inputState.keys[3]) { // D - right
            cameraOffset = cameraOffset.add(new Vec2(moveDistance, 0));
        }
        
        // Clamp camera to map bounds - allow camera to show entire map
        if (levelData != null) {
            // Camera can scroll from showing top-left corner to showing bottom-right corner
            float minX = GameConfig.SCREEN_WIDTH / 2;
            float maxX = levelData.mapWidth - GameConfig.SCREEN_WIDTH / 2;
            float minY = GameConfig.SCREEN_HEIGHT / 2;
            float maxY = levelData.mapHeight - GameConfig.SCREEN_HEIGHT / 2;
            
            cameraOffset = new Vec2(
                Math.max(minX, Math.min(maxX, cameraOffset.x())),
                Math.max(minY, Math.min(maxY, cameraOffset.y()))
            );
        }
    }

    private void loadLevel() {
        try {
            levelData = MapLoader.loadLevel(levelFile);
            gameObjects.clear();
            
            // Find background texture index
            for (int i = 0; i < BACKGROUND_TEXTURES.length; i++) {
                if (BACKGROUND_TEXTURES[i].equals(levelData.backgroundTexture)) {
                    selectedBackgroundIndex = i;
                    break;
                }
            }
            
            // Load turrets
            if (levelData.turrets != null) {
                for (LevelData.TurretData turret : levelData.turrets) {
                    gameObjects.add(GameObjectFactory.createTurret(turret.x, turret.y));
                }
            }
            
            // Load bears
            if (levelData.bears != null) {
                for (LevelData.BearData bear : levelData.bears) {
                    GameObject bearObj = GameObjectFactory.createBear(bear.x, bear.y, bear.facing);
                    gameObjects.add(bearObj);
                    originalFacingData.put(bearObj, bear.facing);
                }
            }
            
            // Load foliage
            if (levelData.foliage != null) {
                for (LevelData.FoliageData foliage : levelData.foliage) {
                    GameObject foliageObj = GameObjectFactory.createFoliage(foliage.x, foliage.y, 
                        foliage.width, foliage.height, foliage.spriteCollisionWidth, foliage.spriteCollisionHeight,
                        foliage.movementCollisionWidth, foliage.movementCollisionHeight, foliage.textureKey, foliage.renderOffset);
                    gameObjects.add(foliageObj);
                    originalFoliageData.put(foliageObj, foliage);
                }
            }
            
            // Load ammo power-ups
            if (levelData.ammoPowerUps != null) {
                for (LevelData.PowerUpData powerUp : levelData.ammoPowerUps) {
                    gameObjects.add(GameObjectFactory.createAmmoPowerUp(powerUp.x, powerUp.y));
                }
            }
            
            // Load throwers
            if (levelData.throwers != null) {
                for (LevelData.ThrowerData thrower : levelData.throwers) {
                    GameObject throwerObj = GameObjectFactory.createThrower(thrower.x, thrower.y);
                    gameObjects.add(throwerObj);
                    originalFacingData.put(throwerObj, thrower.facing);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to load level: " + e.getMessage());
            levelData = new LevelData();
        }
    }

    private String getObjectType(GameObject obj) {
        String className = obj.getClass().getSimpleName();
        return className.toLowerCase();
    }

    private float[] getObjectColor(String objectType) {
        switch (objectType) {
            case "gunturret": return new float[]{1.0f, 0.0f, 0.0f}; // Red
            case "bear": return new float[]{0.6f, 0.3f, 0.0f}; // Brown
            case "foliage": return new float[]{0.0f, 0.8f, 0.0f}; // Green
            case "ammopowerup": return new float[]{0.0f, 0.0f, 1.0f}; // Blue
            case "thrower": return new float[]{1.0f, 0.5f, 0.0f}; // Orange
            default: return new float[]{0.5f, 0.5f, 0.5f}; // Gray
        }
    }

    private void loadTextures() {
        // Load textures for different object types with exception handling
        loadTextureWithFallback("player", "assets/mcupidle.png");
        loadTextureWithFallback("gunturret", "assets/gunturret_n.png");
        loadTextureWithFallback("bear", "assets/bear/rotations/east.png");
        loadTextureWithFallback("foliage", "assets/foliage01.png");
        loadTextureWithFallback("palm_trees", "assets/palm_trees01.png");
        loadTextureWithFallback("palm_trees_group", "assets/palm_trees_group.png");
        loadTextureWithFallback("palm_trees_group_long", "assets/palm_trees_group_long.png");
        loadTextureWithFallback("palm_trees_group_vertical", "assets/palm_trees_group_vertical.png");
        loadTextureWithFallback("palm_trees_group_vertical_long", "assets/palm_trees_group_vertical_long.png");
        loadTextureWithFallback("ammopowerup", "assets/ammocratefull.png");
        loadTextureWithFallback("thrower", "assets/thrower/rotations/east.png");
        
        // Load background texture
        if (levelData != null && levelData.backgroundTexture != null) {
            try {
                backgroundTexture = renderer.loadTexture("assets/" + levelData.backgroundTexture);
                System.out.println("Loaded background texture: " + levelData.backgroundTexture);
            } catch (Exception e) {
                System.err.println("Failed to load background texture: " + levelData.backgroundTexture + " - " + e.getMessage());
            }
        }
    }

    private void loadTextureWithFallback(String key, String path) {
        try {
            Texture texture = renderer.loadTexture(path);
            textures.put(key, texture);
            System.out.println("Loaded texture: " + key + " from " + path);
        } catch (Exception e) {
            System.err.println("Failed to load texture: " + key + " from " + path + " - " + e.getMessage());
            // Continue without this texture - will use colored rectangles as fallback
        }
    }

    private Texture getTextureForObject(String objectType) {
        return textures.get(objectType);
    }
}
