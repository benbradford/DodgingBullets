package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.effects.Explosion;
import com.dodgingbullets.gameobjects.effects.PetrolBomb;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.enemies.Bear;
import com.dodgingbullets.gameobjects.enemies.Thrower;
import com.dodgingbullets.gameobjects.enemies.Mortar;
import com.dodgingbullets.gameobjects.environment.AmmoPowerUp;
import com.dodgingbullets.gameobjects.environment.Foliage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRenderer {
    private Map<Direction, Texture> turretTextures;
    private Map<String, Texture> tileTextures = new HashMap<>();
    private Texture shadowTexture;
    private Texture bulletTexture;
    private Texture shellTexture;
    private Texture brokenTurretTexture;
    private Texture vignetteTexture;
    private Map<String, Texture> foliageTextures = new HashMap<>();
    private Texture ammoFullTexture;
    private Texture ammoEmptyTexture;
    private Texture grenadeTexture;
    private Map<String, Texture> explosionTextures;
    private Map<String, Texture> bearTextures = new HashMap<>();
    private Map<String, Texture> throwerTextures = new HashMap<>();
    private Map<String, Texture> mortarTextures = new HashMap<>();
    private Texture petrolBombTexture;
    private Texture bombTexture;
    
    public void setTextures(Map<Direction, Texture> turretTextures, Map<String, Texture> tileTextures, 
                           Texture shadowTexture, Texture bulletTexture, Texture shellTexture,
                           Texture brokenTurretTexture, Texture vignetteTexture, Map<String, Texture> foliageTextures,
                           Map<String, Texture> explosionTextures, 
                           Texture ammoFullTexture, Texture ammoEmptyTexture, Texture grenadeTexture,
                           Map<String, Texture> bearTextures, Map<String, Texture> throwerTextures, 
                           Map<String, Texture> mortarTextures, Texture petrolBombTexture, Texture bombTexture) {
        this.turretTextures = turretTextures;
        this.tileTextures = tileTextures;
        this.shadowTexture = shadowTexture;
        this.bulletTexture = bulletTexture;
        this.shellTexture = shellTexture;
        this.brokenTurretTexture = brokenTurretTexture;
        this.vignetteTexture = vignetteTexture;
        this.foliageTextures = foliageTextures;
        this.ammoFullTexture = ammoFullTexture;
        this.ammoEmptyTexture = ammoEmptyTexture;
        this.grenadeTexture = grenadeTexture;
        this.explosionTextures = explosionTextures;
        this.bearTextures = bearTextures;
        this.throwerTextures = throwerTextures;
        this.mortarTextures = mortarTextures;
        this.petrolBombTexture = petrolBombTexture;
        this.bombTexture = bombTexture;
    }
    
    public void render(Renderer renderer, GameLoop gameLoop) {
        renderer.clear();
        
        Player player = gameLoop.getPlayer();
        float cameraX = gameLoop.getCameraX();
        float cameraY = gameLoop.getCameraY();
        
        // Render tiled grass background
        renderTiledBackground(renderer, cameraX, cameraY, gameLoop.getMapWidth(), gameLoop.getMapHeight());
        
        // Render player shadow
        float shadowSize = 40 - (player.getJumpOffset() * 0.3f);
        renderer.render(shadowTexture, 
            player.getX() - shadowSize/2 - cameraX, player.getY() - shadowSize/2 - 20 - cameraY, shadowSize, shadowSize);
        
        // Render all objects with depth sorting
        renderGameObjects(renderer, gameLoop, cameraX, cameraY);
        
        // Render bullets as rounded circles with white outline and colored center
        for (Bullet bullet : gameLoop.getBullets()) {
            float bulletX = bullet.getX() - cameraX;
            float bulletY = bullet.getY() - cameraY;
            // White outline (larger circle)
            renderer.renderCircle(bulletX, bulletY, 4, 1.0f, 1.0f, 1.0f, 1.0f);
            // Colored center (smaller circle)
            if (bullet.isSpecialBullet()) {
                renderer.renderCircle(bulletX, bulletY, 2, 1.0f, 0.0f, 0.0f, 1.0f); // Red for special
            } else {
                renderer.renderCircle(bulletX, bulletY, 2, 0.0f, 0.0f, 0.0f, 1.0f); // Black for normal
            }
        }
        
        // Render shell casings
        for (ShellCasing shell : gameLoop.getShells()) {
            renderer.renderRotatedWithAlpha(shellTexture, shell.getX() - 3 - cameraX, shell.getY() - 1.5f - cameraY, 6, 3, shell.getRotation(), shell.getAlpha());
        }
        
        // Render grenades
        for (Grenade grenade : gameLoop.getGrenades()) {
            float grenadeX = grenade.getX() - cameraX;
            float grenadeY = grenade.getY() - cameraY;
            float scale = grenade.getScale();
            float size = 24 * scale; // 1.5x bigger (16 * 1.5 = 24)
            renderer.renderRotatedWithAlpha(grenadeTexture, grenadeX - size/2, grenadeY - size/2, size, size, grenade.getRotation(), 1.0f);
        }
        
        // Render petrol bombs
        for (PetrolBomb bomb : gameLoop.getPetrolBombs()) {
            // Render shadow on ground
            float shadowX = bomb.getShadowPosition().x() - cameraX;
            float shadowY = bomb.getShadowPosition().y() - cameraY;
            float bombShadowScale = bomb.getShadowScale();
            float bombShadowSize = 30 * bombShadowScale; // Increased from 20 to 30 for better visibility
            renderer.renderTextureWithColor(shadowTexture, shadowX - bombShadowSize/2, shadowY - bombShadowSize/2, bombShadowSize, bombShadowSize, 0.0f, 0.0f, 0.0f, 0.8f); // Increased alpha from 0.5f to 0.8f
            
            // Render bomb in air
            float bombX = bomb.getX() - cameraX;
            float bombY = bomb.getY() + bomb.getHeight() - cameraY; // Changed from minus to plus for upward trajectory
            float bombSize = 32; // Increased from 16 to 32 for better visibility
            renderer.renderRotatedWithAlpha(petrolBombTexture, bombX - bombSize/2, bombY - bombSize/2, bombSize, bombSize, bomb.getRotation(), 1.0f);
        }
        
        // Render mortar bombs
        for (Bomb bomb : gameLoop.getBombs()) {
            // Render shadow on ground
            float shadowX = bomb.getShadowPosition().x() - cameraX;
            float shadowY = bomb.getShadowPosition().y() - cameraY;
            float bombShadowSize = 20;
            renderer.renderTextureWithColor(shadowTexture, shadowX - bombShadowSize/2, shadowY - bombShadowSize/2, bombShadowSize, bombShadowSize, 0.0f, 0.0f, 0.0f, 0.6f);
            
            // Render bomb in air
            float bombX = bomb.getPosition().x() - cameraX;
            float bombY = bomb.getPosition().y() - cameraY + bomb.getHeight();
            float bombSize = 24;
            renderer.render(bombTexture, bombX - bombSize/2, bombY - bombSize/2, bombSize, bombSize);
        }
        
        // Render explosions on top of everything
        for (Explosion explosion : gameLoop.getExplosions()) {
            String textureName = explosion.getCurrentTexture();
            if (textureName != null && explosionTextures.containsKey(textureName)) {
                Texture explosionTexture = explosionTextures.get(textureName);
                float size = explosion.getSize();
                renderer.renderTextureWithColor(explosionTexture, explosion.getX() - size/2 - cameraX, explosion.getY() - size/2 - cameraY, size, size, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        
        // Render UI
        renderHealthBar(renderer, player, cameraX, cameraY);
        renderAmmoBar(renderer, player, cameraX, cameraY);
        
        // Render vignette overlay
        float flashIntensity = player.getDamageFlashIntensity();
        float red = 1.0f + flashIntensity * 0.5f;
        float green = 1.0f - flashIntensity * 0.3f;
        float blue = 1.0f - flashIntensity * 0.3f;
        renderer.renderTextureWithColor(vignetteTexture, 0, 0, gameLoop.getScreenWidth(), gameLoop.getScreenHeight(), red, green, blue, 0.33f);
    }
    
    private void renderGameObjects(Renderer renderer, GameLoop gameLoop, float cameraX, float cameraY) {
        Player player = gameLoop.getPlayer();
        
        // Create list of all objects for depth sorting (including player)
        List<Object> allObjects = new ArrayList<>();
        allObjects.add(player);
        allObjects.addAll(gameLoop.getGameObjects());
        allObjects.addAll(gameLoop.getFoliages());
        allObjects.addAll(gameLoop.getAmmoPowerUps());
        
        // Sort all objects by Y position for depth (lower Y renders first/behind, higher Y renders last/on top)
        allObjects.sort((a, b) -> {
            float aY = (a instanceof Player) ? ((Player) a).getY() :
                      (a instanceof Renderable) ? ((Renderable) a).getRenderY() : ((GameObject) a).getY();
            float bY = (b instanceof Player) ? ((Player) b).getY() :
                      (b instanceof Renderable) ? ((Renderable) b).getRenderY() : ((GameObject) b).getY();
            return Float.compare(bY, aY); // Lower Y renders first (behind), higher Y renders last (on top)
        });
        
        // Render all objects in depth order
        for (Object obj : allObjects) {
            if (obj instanceof Player) {
                Player p = (Player) obj;
                Texture currentTexture = p.getCurrentTexture();
                renderer.render(currentTexture, 
                    p.getX() - 32 - cameraX, p.getY() - 32 + p.getJumpOffset() - cameraY, 64, 64);
            } else if (obj instanceof GameObject) {
                GameObject gameObj = (GameObject) obj;
                if (gameObj instanceof Bear) {
                    Bear bear = (Bear) gameObj;
                    renderBear(renderer, bear, cameraX, cameraY);
                } else if (gameObj instanceof Thrower) {
                    Thrower thrower = (Thrower) gameObj;
                    renderThrower(renderer, thrower, cameraX, cameraY);
                } else if (gameObj instanceof Mortar) {
                    Mortar mortar = (Mortar) gameObj;
                    renderMortar(renderer, mortar, cameraX, cameraY);
                } else if (gameObj instanceof Trackable && gameObj instanceof Damageable && !(gameObj instanceof Mortar)) {
                    Trackable trackable = (Trackable) gameObj;
                    Damageable damageable = (Damageable) gameObj;
                    Texture turretTexture = damageable.isDestroyed() ? brokenTurretTexture : 
                                           turretTextures.get(trackable.getFacingDirection());
                    
                    // Check if turret is flashing from damage
                    if (gameObj instanceof GunTurret && ((GunTurret) gameObj).isDamageFlashing()) {
                        // Flash red for damage
                        renderer.renderTextureWithColor(turretTexture, gameObj.getX() - 64 - cameraX, gameObj.getY() - 64 - cameraY, 128, 128, 1.0f, 0.0f, 0.0f, 1.0f);
                    } else {
                        // Normal rendering
                        renderer.renderTextureWithColor(turretTexture, gameObj.getX() - 64 - cameraX, gameObj.getY() - 64 - cameraY, 128, 128, 1.0f, 1.0f, 1.0f, 1.0f);
                    }
                } else if (gameObj.getClass().getSimpleName().equals("Foliage")) {
                    Foliage foliage = (Foliage) gameObj;
                    Texture texture = foliageTextures.get(foliage.getTextureKey());
                    float width = foliage.getSpriteWidth();
                    float height = foliage.getSpriteHeight();
                    renderer.render(texture, gameObj.getX() - width/2 - cameraX, gameObj.getY() - height/2 - cameraY, width, height);
                } else if (gameObj.getClass().getSimpleName().equals("AmmoPowerUp")) {
                    AmmoPowerUp ammo = (AmmoPowerUp) gameObj;
                    Texture texture = ammo.isCollected() ? ammoEmptyTexture : ammoFullTexture;
                    renderer.render(texture, gameObj.getX() - 32 - cameraX, gameObj.getY() - 32 - cameraY, 64, 64);
                }
            }
        }
    }
    
    private void renderHealthBar(Renderer renderer, Player player, float cameraX, float cameraY) {
        float healthBarWidth = 40;
        float healthBarHeight = 6;
        float healthBarX = player.getX() - healthBarWidth/2 - cameraX;
        float healthBarY = player.getY() - 45 - cameraY;
        
        float healthPercent = player.getHealth() / 100.0f;
        float red = 1.0f - healthPercent;
        float green = healthPercent;
        
        renderer.renderRectOutline(healthBarX, healthBarY, healthBarWidth, healthBarHeight, 1.0f, 1.0f, 1.0f, 1.0f);
        
        float fillWidth = healthBarWidth * healthPercent;
        if (fillWidth > 0) {
            renderer.renderRect(healthBarX, healthBarY, fillWidth, healthBarHeight, red, green, 0.0f, 1.0f);
        }
    }
    
    private void renderAmmoBar(Renderer renderer, Player player, float cameraX, float cameraY) {
        float ammoBarWidth = 40;
        float ammoBarHeight = 6;
        float ammoBarX = player.getX() - ammoBarWidth/2 - cameraX;
        float ammoBarY = player.getY() - 55 - cameraY;
        
        boolean hasSpecial = player.hasSpecialBullets();
        float maxAmmo = hasSpecial ? 100.0f : 10.0f;
        float ammoPercent = player.getAmmo() / maxAmmo;
        
        renderer.renderRectOutline(ammoBarX, ammoBarY, ammoBarWidth, ammoBarHeight, 1.0f, 1.0f, 1.0f, 1.0f);
        
        float fillWidth = ammoBarWidth * ammoPercent;
        if (fillWidth > 0) {
            if (hasSpecial) {
                // Flashing red/blue for special bullets
                long time = System.currentTimeMillis();
                float flash = (float) Math.sin(time * 0.01) * 0.5f + 0.5f; // 0-1 oscillation
                float red = 1.0f * flash;
                float blue = 1.0f * (1.0f - flash);
                renderer.renderRect(ammoBarX, ammoBarY, fillWidth, ammoBarHeight, red, 0.0f, blue, 1.0f);
            } else {
                // Normal blue for regular ammo
                renderer.renderRect(ammoBarX, ammoBarY, fillWidth, ammoBarHeight, 0.0f, 0.5f, 1.0f, 1.0f);
            }
        }
    }
    
    private void renderTiledBackground(Renderer renderer, float cameraX, float cameraY, float mapWidth, float mapHeight) {
        String[][] mapGrid = GameObjectFactory.getMapGrid();
        int tileSize = 128; // Fixed 128x128 tile size
        
        // Calculate visible tile range based on camera position
        int startTileX = Math.max(0, (int)(cameraX / tileSize));
        int startTileY = Math.max(0, (int)(cameraY / tileSize));
        int endTileX = Math.min(mapGrid[0].length - 1, (int)Math.ceil((cameraX + GameConfig.SCREEN_WIDTH) / tileSize));
        int endTileY = Math.min(mapGrid.length - 1, (int)Math.ceil((cameraY + GameConfig.SCREEN_HEIGHT) / tileSize));
        
        // Render tiles from the grid
        for (int tileX = startTileX; tileX <= endTileX; tileX++) {
            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
                String tileTextureName = mapGrid[tileY][tileX];
                Texture tileTexture = tileTextures.get(tileTextureName);
                
                if (tileTexture != null) {
                    float x = tileX * tileSize - cameraX;
                    float y = tileY * tileSize - cameraY;
                    renderer.render(tileTexture, x, y, tileSize, tileSize);
                }
            }
        }
    }
    
    private void renderBear(Renderer renderer, Bear bear, float cameraX, float cameraY) {
        if (!bear.isActive()) return;
        
        // Get the appropriate texture based on bear state and direction
        String textureKey = getBearTextureKey(bear);
        Texture bearTexture = bearTextures.get(textureKey);
        
        if (bearTexture != null) {
            float alpha = bear.getFadeAlpha();
            float rotation = bear.getRotationAngle();
            
            if (bear.getState() == Bear.BearState.DYING) {
                // Render with fade effect and rotation
                renderer.renderRotatedWithAlpha(bearTexture, bear.getX() - 32 - cameraX, bear.getY() - 32 - cameraY, 64, 64, rotation, alpha);
            } else if (bear.shouldFlash()) {
                // Flash red for damage
                renderer.renderTextureWithColor(bearTexture, bear.getX() - 32 - cameraX, bear.getY() - 32 - cameraY, 64, 64, 1.0f, 0.0f, 0.0f, 1.0f);
            } else {
                // Normal rendering
                renderer.render(bearTexture, bear.getX() - 32 - cameraX, bear.getY() - 32 - cameraY, 64, 64);
            }
        }
    }
    
    private void renderThrower(Renderer renderer, Thrower thrower, float cameraX, float cameraY) {
        if (!thrower.isActive()) return;
        
        // Get the appropriate texture based on thrower state and direction
        String textureKey = getThrowerTextureKey(thrower);
        Texture throwerTexture = throwerTextures.get(textureKey);
        
        // Debug logging
        if (throwerTexture == null) {
            System.err.println("Missing thrower texture: " + textureKey + 
                " (State: " + thrower.getState() + ", Frame: " + thrower.getCurrentFrame() + 
                ", Direction: " + thrower.getFacingDirection() + ")");
            return;
        }
        
        if (throwerTexture != null) {
            float alpha = thrower.getAlpha();
            float rotation = thrower.getRotation();
            
            if (thrower.getState() == Thrower.ThrowerState.DYING) {
                // Render with fade effect and rotation
                renderer.renderRotatedWithAlpha(throwerTexture, thrower.getX() - 32 - cameraX, thrower.getY() - 32 - cameraY, 64, 64, rotation, alpha);
            } else if (thrower.shouldFlash()) {
                // Flash red for damage
                renderer.renderTextureWithColor(throwerTexture, thrower.getX() - 32 - cameraX, thrower.getY() - 32 - cameraY, 64, 64, 1.0f, 0.0f, 0.0f, 1.0f);
            } else {
                // Normal rendering
                renderer.render(throwerTexture, thrower.getX() - 32 - cameraX, thrower.getY() - 32 - cameraY, 64, 64);
            }
        }
    }
    
    private String getThrowerTextureKey(Thrower thrower) {
        Thrower.ThrowerState state = thrower.getState();
        Direction direction = thrower.getFacingDirection();
        int frame = thrower.getCurrentFrame();
        
        String directionStr = getDirectionString(direction);
        
        switch (state) {
            case IDLE:
                return "thrower_rotation_" + directionStr;
            case CHASE:
            case BACKING_OFF:
                return "thrower_walking_" + directionStr + "_" + String.format("%03d", frame);
            case THROWING:
                return "thrower_throw_" + directionStr + "_" + String.format("%03d", frame);
            case HIT:
            case DYING:
                return "thrower_rotation_" + directionStr;
            default:
                return "thrower_rotation_east";
        }
    }
    
    private void renderMortar(Renderer renderer, Mortar mortar, float cameraX, float cameraY) {
        String textureKey = getMortarTextureKey(mortar);
        Texture mortarTexture = mortarTextures.get(textureKey);
        
        if (mortarTexture == null) {
            System.err.println("Missing mortar texture: " + textureKey + " - using turret texture as fallback");
            System.err.println("Available keys: " + mortarTextures.keySet());
            // Use turret texture as fallback
            mortarTexture = turretTextures.get(mortar.getLookDirection());
        }
        
        if (mortarTexture != null) {
            if (mortar.shouldFlash()) {
                renderer.renderTextureWithColor(mortarTexture, mortar.getX() - 32 - cameraX, mortar.getY() - 32 - cameraY, 64, 64, 1.0f, 0.0f, 0.0f, 1.0f);
            } else {
                renderer.render(mortarTexture, mortar.getX() - 32 - cameraX, mortar.getY() - 32 - cameraY, 64, 64);
            }
        }
    }
    
    private String getMortarTextureKey(Mortar mortar) {
        Mortar.MortarState state = mortar.getState();
        Direction direction = mortar.getLookDirection();
        String directionStr = getDirectionString(direction);
        
        switch (state) {
            case PATROL:
                return "mortar_rotation_" + directionStr;
            case ENGAGED:
                return "mortar_sitting_" + directionStr + "_009";
            case FIRING:
                int frame = mortar.getCurrentFrame();
                return "mortar_sitting_" + directionStr + "_" + String.format("%03d", frame + 1);
            default:
                return "mortar_rotation_" + directionStr;
        }
    }
    
    private String getBearTextureKey(Bear bear) {
        Bear.BearState state = bear.getState();
        Direction direction = bear.getFacingDirection();
        int frame = bear.getCurrentFrame();
        
        String directionStr = getDirectionString(direction);
        
        switch (state) {
            case IDLE:
                return "bear_idle_" + directionStr + "_" + String.format("%03d", frame);
            case WAKING_UP:
                return "bear_wakingUp_" + directionStr + "_" + String.format("%03d", frame);
            case RUNNING:
                return "bear_running_" + directionStr + "_" + String.format("%03d", frame);
            case HIT:
            case DYING:
                return "bear_hit_" + directionStr + "_" + String.format("%03d", frame);
            default:
                return "bear_idle_east_000";
        }
    }
    
    private String getDirectionString(Direction direction) {
        switch (direction) {
            case UP: return "north";
            case DOWN: return "south";
            case LEFT: return "west";
            case RIGHT: return "east";
            case UP_LEFT: return "northwest";
            case UP_RIGHT: return "northeast";
            case DOWN_LEFT: return "southwest";
            case DOWN_RIGHT: return "southeast";
            default: return "east";
        }
    }
}
