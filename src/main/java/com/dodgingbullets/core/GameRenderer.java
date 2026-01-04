package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.effects.Explosion;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.enemies.Bear;
import com.dodgingbullets.gameobjects.environment.AmmoPowerUp;
import com.dodgingbullets.gameobjects.environment.Foliage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRenderer {
    private Map<Direction, Texture> turretTextures;
    private Texture grassTexture;
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
    
    public void setTextures(Map<Direction, Texture> turretTextures, Texture grassTexture, 
                           Texture shadowTexture, Texture bulletTexture, Texture shellTexture,
                           Texture brokenTurretTexture, Texture vignetteTexture, Map<String, Texture> foliageTextures,
                           Map<String, Texture> explosionTextures, 
                           Texture ammoFullTexture, Texture ammoEmptyTexture, Texture grenadeTexture,
                           Map<String, Texture> bearTextures) {
        this.turretTextures = turretTextures;
        this.grassTexture = grassTexture;
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
                } else if (gameObj instanceof Trackable && gameObj instanceof Damageable) {
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
        // Render single large background texture covering entire map
        renderer.render(grassTexture, -cameraX, -cameraY, mapWidth, mapHeight);
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
            } else {
                // Normal rendering
                renderer.render(bearTexture, bear.getX() - 32 - cameraX, bear.getY() - 32 - cameraY, 64, 64);
            }
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
            case UP_LEFT: return "north-west";
            case UP_RIGHT: return "north-east";
            case DOWN_LEFT: return "south-west";
            case DOWN_RIGHT: return "south-east";
            default: return "east";
        }
    }
}
