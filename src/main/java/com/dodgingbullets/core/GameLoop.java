package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.enemies.GunTurret;
import com.dodgingbullets.gameobjects.environment.Foliage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GameLoop {
    private Player player;
    private List<Bullet> bullets = new ArrayList<>();
    private List<ShellCasing> shells = new ArrayList<>();
    private List<GameObject> turrets = new ArrayList<>();
    private List<GameObject> foliages = new ArrayList<>();
    private float cameraX = 0;
    private float cameraY = 0;
    
    // Game constants
    private static final float MAP_WIDTH = 2560;
    private static final float MAP_HEIGHT = 1440;
    private static final float SCREEN_WIDTH = 704; // Updated for 10% zoom out
    private static final float SCREEN_HEIGHT = 396; // Updated for 10% zoom out
    
    public void initialize(Renderer renderer) {
        // Initialize player
        player = new Player(320, 180);
        player.loadTextures(renderer);
        
        // Initialize turrets at different locations
        turrets.add(new GunTurret(800, 150));
        turrets.add(new GunTurret(1200, 400));
        turrets.add(new GunTurret(600, 600));
        
        // Initialize foliage
        foliages.add(new Foliage(400, 300));
        foliages.add(new Foliage(1000, 200));
        foliages.add(new Foliage(1500, 800));
        foliages.add(new Foliage(200, 900));
        foliages.add(new Foliage(1800, 600));
        
        // Set up collision objects for player
        List<GameObject> allCollidables = new ArrayList<>();
        allCollidables.addAll(turrets);
        allCollidables.addAll(foliages);
        player.setCollidableObjects(allCollidables);
    }
    
    public void update(boolean[] keys, boolean jumpPressed, boolean jumpHeld, boolean mousePressed, double mouseX, double mouseY) {
        // Update player
        player.update(keys, jumpPressed, jumpHeld);
        
        // Update camera to follow player with map edge clamping
        float desiredCameraX = player.getX() - SCREEN_WIDTH / 2;
        float desiredCameraY = player.getY() - SCREEN_HEIGHT / 2;
        
        cameraX = Math.max(0, Math.min(MAP_WIDTH - SCREEN_WIDTH, desiredCameraX));
        cameraY = Math.max(0, Math.min(MAP_HEIGHT - SCREEN_HEIGHT, desiredCameraY));
        
        // Update turrets
        for (GameObject turret : turrets) {
            if (turret instanceof Trackable) {
                ((Trackable) turret).update(player.getX(), player.getY());
            }
        }
        
        // Handle player shooting
        if (mousePressed && player.canShoot()) {
            double worldMouseX = mouseX + cameraX;
            double worldMouseY = mouseY + cameraY;
            
            double deltaX = worldMouseX - player.getX();
            double deltaY = worldMouseY - player.getY();
            double angle = Math.atan2(deltaY, deltaX);
            
            Direction shootDirection = Player.calculateDirectionFromAngle(angle);
            player.setShootingDirection(shootDirection);
            
            player.shoot();
            float[] gunPos = player.getGunBarrelPosition();
            bullets.add(new Bullet(gunPos[0], gunPos[1], angle, true));
            shells.add(new ShellCasing(player.getX(), player.getY()));
        }
        
        // Handle turret shooting
        for (GameObject turret : turrets) {
            if (turret instanceof Trackable && turret instanceof Shooter && turret instanceof Positionable && turret instanceof Damageable) {
                Trackable trackableTurret = (Trackable) turret;
                Shooter shooterTurret = (Shooter) turret;
                Positionable positionableTurret = (Positionable) turret;
                Damageable damageableTurret = (Damageable) turret;
                
                if (!damageableTurret.isDestroyed() && trackableTurret.canSeePlayer(player.getX(), player.getY()) && 
                    trackableTurret.canSeePlayerInCurrentDirection(player.getX(), player.getY()) && shooterTurret.canShoot()) {
                    float[] barrelPos = positionableTurret.getBarrelPosition();
                    double deltaX = player.getX() - turret.getX();
                    double deltaY = player.getY() - turret.getY();
                    double angleToPlayer = Math.atan2(deltaY, deltaX);
                    bullets.add(new Bullet(barrelPos[0], barrelPos[1], angleToPlayer, false));
                    shooterTurret.shoot(player.getX(), player.getY());
                }
            }
        }
        
        // Update bullets and check collisions
        updateBullets();
        
        // Update shell casings
        updateShells();
    }
    
    private void updateBullets() {
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            bullet.update();
            
            // Check collision with foliage
            boolean hitFoliage = false;
            for (GameObject foliage : foliages) {
                if (foliage instanceof Collidable && ((Collidable) foliage).checkSpriteCollision(bullet.getX(), bullet.getY(), 1, 1)) {
                    bulletIter.remove();
                    hitFoliage = true;
                    break;
                }
            }
            if (hitFoliage) continue;
            
            // Check collision with turrets (only player bullets)
            if (bullet.isPlayerBullet()) {
                for (GameObject turret : turrets) {
                    if (turret instanceof Positionable && ((Positionable) turret).isInSpriteHitbox(bullet.getX(), bullet.getY())) {
                        if (turret instanceof Damageable) {
                            ((Damageable) turret).takeDamage(10);
                        }
                        bulletIter.remove();
                        break;
                    }
                }
                if (!bulletIter.hasNext()) continue;
            }
            
            // Check collision with player (only enemy bullets)
            if (!bullet.isPlayerBullet() && isPlayerHit(bullet.getX(), bullet.getY())) {
                player.takeDamage(5);
                bulletIter.remove();
                continue;
            }
            
            if (bullet.isExpired()) {
                bulletIter.remove();
            }
        }
    }
    
    private void updateShells() {
        Iterator<ShellCasing> shellIter = shells.iterator();
        while (shellIter.hasNext()) {
            ShellCasing shell = shellIter.next();
            shell.update();
            if (shell.isExpired()) {
                shellIter.remove();
            }
        }
    }
    
    private boolean isPlayerHit(float bulletX, float bulletY) {
        return bulletX >= player.getX() - 6 && bulletX <= player.getX() + 6 && 
               bulletY >= player.getY() - 32 && bulletY <= player.getY() + 32;
    }
    
    // Getters for rendering
    public Player getPlayer() { return player; }
    public List<Bullet> getBullets() { return bullets; }
    public List<ShellCasing> getShells() { return shells; }
    public List<GameObject> getTurrets() { return turrets; }
    public List<GameObject> getFoliages() { return foliages; }
    public float getCameraX() { return cameraX; }
    public float getCameraY() { return cameraY; }
    public float getMapWidth() { return MAP_WIDTH; }
    public float getMapHeight() { return MAP_HEIGHT; }
    public float getScreenWidth() { return SCREEN_WIDTH; }
    public float getScreenHeight() { return SCREEN_HEIGHT; }
}
