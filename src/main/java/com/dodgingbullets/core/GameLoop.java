package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.effects.Explosion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameLoop {
    private Player player;
    private List<Bullet> bullets = new ArrayList<>();
    private List<ShellCasing> shells = new ArrayList<>();
    private List<GameObject> turrets = new ArrayList<>();
    private List<GameObject> foliages = new ArrayList<>();
    private List<Explosion> explosions = new ArrayList<>();
    private float cameraX = 0;
    private float cameraY = 0;
    
    private CollisionSystem collisionSystem = new CollisionSystem();
    private InputHandler inputHandler = new InputHandler();
    
    public void initialize(Renderer renderer) {
        // Initialize player
        player = GameObjectFactory.createPlayer();
        player.loadTextures(renderer);
        
        // Initialize game objects
        turrets = GameObjectFactory.createTurrets();
        foliages = GameObjectFactory.createFoliage();
        
        // Set up collision objects for player
        List<GameObject> allCollidables = new ArrayList<>();
        allCollidables.addAll(turrets);
        allCollidables.addAll(foliages);
        player.setCollidableObjects(allCollidables);
    }
    
    public void update(boolean[] keys, boolean jumpPressed, boolean jumpHeld, boolean mousePressed, double mouseX, double mouseY) {
        // Process input
        InputState input = inputHandler.processInput(keys, jumpPressed, jumpHeld, mousePressed, mouseX, mouseY);
        
        // Update player
        player.update(input.keys, input.jumpPressed, input.jumpHeld);
        
        // Update camera
        updateCamera();
        inputHandler.updateCamera(cameraX, cameraY);
        
        // Update game objects
        updateTurrets();
        
        // Handle shooting
        handleShooting(input);
        
        // Update and check collisions
        updateBullets();
        updateExplosions();
        updateShells();
    }
    
    private void updateCamera() {
        float desiredCameraX = player.getX() - GameConfig.SCREEN_WIDTH / 2;
        float desiredCameraY = player.getY() - GameConfig.SCREEN_HEIGHT / 2;
        
        cameraX = Math.max(0, Math.min(GameConfig.MAP_WIDTH - GameConfig.SCREEN_WIDTH, desiredCameraX));
        cameraY = Math.max(0, Math.min(GameConfig.MAP_HEIGHT - GameConfig.SCREEN_HEIGHT, desiredCameraY));
    }
    
    private void updateTurrets() {
        for (GameObject turret : turrets) {
            if (turret instanceof Trackable) {
                ((Trackable) turret).update(player.getX(), player.getY());
            }
        }
    }
    
    private void handleShooting(InputState input) {
        // Handle player shooting
        if (input.mousePressed && player.canShoot()) {
            double deltaX = input.worldMouseX - player.getX();
            double deltaY = input.worldMouseY - player.getY();
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
            if (turret instanceof Trackable && turret instanceof Shooter && 
                turret instanceof Positionable && turret instanceof Damageable) {
                
                Trackable trackable = (Trackable) turret;
                Shooter shooter = (Shooter) turret;
                Positionable positionable = (Positionable) turret;
                Damageable damageable = (Damageable) turret;
                
                if (!damageable.isDestroyed() && trackable.canSeePlayer(player.getX(), player.getY()) && 
                    trackable.canSeePlayerInCurrentDirection(player.getX(), player.getY()) && shooter.canShoot()) {
                    
                    float[] barrelPos = positionable.getBarrelPosition();
                    double deltaX = player.getX() - turret.getX();
                    double deltaY = player.getY() - turret.getY();
                    double angleToPlayer = Math.atan2(deltaY, deltaX);
                    bullets.add(new Bullet(barrelPos[0], barrelPos[1], angleToPlayer, false));
                    shooter.shoot(player.getX(), player.getY());
                }
            }
        }
    }
    
    private void updateBullets() {
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            bullet.update();
        }
        
        collisionSystem.checkBulletCollisions(bullets, player, turrets, foliages, explosions);
    }
    
    private void updateExplosions() {
        Iterator<Explosion> explosionIter = explosions.iterator();
        while (explosionIter.hasNext()) {
            Explosion explosion = explosionIter.next();
            explosion.update(GameConfig.DELTA_TIME);
            
            if (!explosion.isActive()) {
                explosionIter.remove();
            }
        }
        
        collisionSystem.checkExplosionCollisions(explosions, player);
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
    
    // Getters for rendering
    public Player getPlayer() { return player; }
    public List<Bullet> getBullets() { return bullets; }
    public List<ShellCasing> getShells() { return shells; }
    public List<GameObject> getTurrets() { return turrets; }
    public List<GameObject> getFoliages() { return foliages; }
    public List<Explosion> getExplosions() { return explosions; }
    public float getCameraX() { return cameraX; }
    public float getCameraY() { return cameraY; }
    public float getMapWidth() { return GameConfig.MAP_WIDTH; }
    public float getMapHeight() { return GameConfig.MAP_HEIGHT; }
    public float getScreenWidth() { return GameConfig.SCREEN_WIDTH; }
    public float getScreenHeight() { return GameConfig.SCREEN_HEIGHT; }
}
