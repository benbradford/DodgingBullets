package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.effects.Explosion;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import java.util.ArrayList;
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
    private Texture foliageTexture;
    private Map<String, Texture> explosionTextures;
    
    public void setTextures(Map<Direction, Texture> turretTextures, Texture grassTexture, 
                           Texture shadowTexture, Texture bulletTexture, Texture shellTexture,
                           Texture brokenTurretTexture, Texture vignetteTexture, Texture foliageTexture,
                           Map<String, Texture> explosionTextures) {
        this.turretTextures = turretTextures;
        this.grassTexture = grassTexture;
        this.shadowTexture = shadowTexture;
        this.bulletTexture = bulletTexture;
        this.shellTexture = shellTexture;
        this.brokenTurretTexture = brokenTurretTexture;
        this.vignetteTexture = vignetteTexture;
        this.foliageTexture = foliageTexture;
        this.explosionTextures = explosionTextures;
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
        
        // Render bullets
        for (Bullet bullet : gameLoop.getBullets()) {
            renderer.render(bulletTexture, bullet.getX() - 3 - cameraX, bullet.getY() - 3 - cameraY, 6, 6);
        }
        
        // Render shell casings
        for (ShellCasing shell : gameLoop.getShells()) {
            renderer.renderRotatedWithAlpha(shellTexture, shell.getX() - 3 - cameraX, shell.getY() - 1.5f - cameraY, 6, 3, shell.getRotation(), shell.getAlpha());
        }
        
        // Render explosions on top of everything
        for (Explosion explosion : gameLoop.getExplosions()) {
            String textureName = explosion.getCurrentTexture();
            if (textureName != null && explosionTextures.containsKey(textureName)) {
                Texture explosionTexture = explosionTextures.get(textureName);
                float size = explosion.getSize();
                renderer.render(explosionTexture, explosion.getX() - size/2 - cameraX, explosion.getY() - size/2 - cameraY, size, size);
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
        renderer.renderTextureWithColor(vignetteTexture, 0, 0, gameLoop.getScreenWidth(), gameLoop.getScreenHeight(), red, green, blue, 0.15f);
    }
    
    private void renderGameObjects(Renderer renderer, GameLoop gameLoop, float cameraX, float cameraY) {
        Player player = gameLoop.getPlayer();
        
        // Create list of all objects for depth sorting (including player)
        List<Object> allObjects = new ArrayList<>();
        allObjects.add(player);
        allObjects.addAll(gameLoop.getTurrets());
        allObjects.addAll(gameLoop.getFoliages());
        
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
                if (gameObj instanceof Trackable && gameObj instanceof Damageable) {
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
                    renderer.render(foliageTexture, gameObj.getX() - 25 - cameraX, gameObj.getY() - 25 - cameraY, 50, 50);
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
        
        float ammoPercent = player.getAmmo() / 10.0f;
        
        renderer.renderRectOutline(ammoBarX, ammoBarY, ammoBarWidth, ammoBarHeight, 1.0f, 1.0f, 1.0f, 1.0f);
        
        float fillWidth = ammoBarWidth * ammoPercent;
        if (fillWidth > 0) {
            renderer.renderRect(ammoBarX, ammoBarY, fillWidth, ammoBarHeight, 0.0f, 0.5f, 1.0f, 1.0f);
        }
    }
    
    private void renderTiledBackground(Renderer renderer, float cameraX, float cameraY, float mapWidth, float mapHeight) {
        int tileSize = 64;
        
        int startTileX = Math.max(0, (int)(cameraX / tileSize) - 1);
        int startTileY = Math.max(0, (int)(cameraY / tileSize) - 1);
        int endTileX = Math.min((int)(mapWidth / tileSize), startTileX + (704 / tileSize) + 3);
        int endTileY = Math.min((int)(mapHeight / tileSize), startTileY + (396 / tileSize) + 3);
        
        for (int x = startTileX; x < endTileX; x++) {
            for (int y = startTileY; y < endTileY; y++) {
                float worldX = x * tileSize;
                float worldY = y * tileSize;
                renderer.render(grassTexture, worldX - cameraX, worldY - cameraY, tileSize, tileSize);
            }
        }
    }
}
