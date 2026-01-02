package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.effects.Explosion;
import com.dodgingbullets.gameobjects.environment.AmmoPowerUp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameLoop {
    private Player player;
    private List<Bullet> bullets = new ArrayList<>();
    private List<ShellCasing> shells = new ArrayList<>();
    private List<Grenade> grenades = new ArrayList<>();
    private List<GameObject> gameObjects = new ArrayList<>();
    private List<GameObject> foliages = new ArrayList<>();
    private List<GameObject> ammoPowerUps = new ArrayList<>();
    private List<Explosion> explosions = new ArrayList<>();
    private Vec2 camera = new Vec2(0, 0);
    private long lastPlayerShootTime = 0;
    
    private CollisionSystem collisionSystem = new CollisionSystem();
    private InputHandler inputHandler = new InputHandler();
    
    public void initialize(Renderer renderer) {
        // Initialize player
        player = GameObjectFactory.createPlayer();
        player.loadTextures(renderer);
        
        // Initialize game objects
        gameObjects = GameObjectFactory.createTurrets();
        foliages = GameObjectFactory.createFoliage();
        ammoPowerUps = GameObjectFactory.createAmmoPowerUps();
        
        // Set up collision objects for player
        List<GameObject> allCollidables = new ArrayList<>();
        allCollidables.addAll(gameObjects);
        allCollidables.addAll(foliages);
        allCollidables.addAll(ammoPowerUps);
        player.setCollidableObjects(allCollidables);
    }
    
    public void update(boolean[] keys, boolean jumpPressed, boolean jumpHeld, boolean mousePressed, boolean mouseHeld, boolean grenadePressed, double mouseX, double mouseY) {
        // Process input
        InputState input = inputHandler.processInput(keys, jumpPressed, jumpHeld, mousePressed, mouseHeld, grenadePressed, mouseX, mouseY);
        
        // Update player
        player.update(input.keys, input.jumpPressed, input.jumpHeld);
        
        // Update camera
        updateCamera();
        inputHandler.updateCamera(camera.x(), camera.y());
        
        // Update game objects
        updateTurrets();
        
        // Handle shooting
        handleShooting(input);
        
        // Handle grenades
        handleGrenades(input);
        
        // Check ammo power-up collection
        checkAmmoPowerUpCollection();
        
        // Update and check collisions
        updateBullets();
        updateGrenades();
        updateExplosions();
        updateShells();
    }
    
    private void updateCamera() {
        Vec2 playerPos = player.getPosition();
        Vec2 screenCenter = new Vec2(GameConfig.SCREEN_WIDTH / 2, GameConfig.SCREEN_HEIGHT / 2);
        Vec2 desiredCamera = playerPos.subtract(screenCenter);
        
        Vec2 mapBounds = new Vec2(GameConfig.MAP_WIDTH - GameConfig.SCREEN_WIDTH, GameConfig.MAP_HEIGHT - GameConfig.SCREEN_HEIGHT);
        camera = desiredCamera.clamp(new Vec2(0, 0), mapBounds);
    }
    
    private void updateTurrets() {
        for (GameObject gameObject : gameObjects) {
            if (gameObject instanceof Trackable) {
                ((Trackable) gameObject).update(player.getX(), player.getY());
            }
        }
    }
    
    private void handleShooting(InputState input) {
        // Handle player shooting
        boolean canRapidFire = player.hasSpecialBullets();
        boolean shouldShoot = canRapidFire ? input.mouseHeld : input.mousePressed;
        
        if (shouldShoot && player.canShoot()) {
            // Check rapid fire timing for special bullets
            if (canRapidFire) {
                long now = System.currentTimeMillis();
                if (now - lastPlayerShootTime < 200) { // 5 shots per second = 200ms interval
                    return;
                }
                lastPlayerShootTime = now;
            }
            
            double deltaX = input.worldMouseX - player.getX();
            double deltaY = input.worldMouseY - player.getY();
            double angle = Math.atan2(deltaY, deltaX);
            
            Direction shootDirection = Player.calculateDirectionFromAngle(angle);
            player.setShootingDirection(shootDirection);
            
            player.shoot();
            float[] gunPos = player.getGunBarrelPosition();
            bullets.add(new Bullet(gunPos[0], gunPos[1], angle, true, player.hasSpecialBullets()));
            shells.add(new ShellCasing(player.getX(), player.getY()));
        }
        
        // Handle shooting from game objects
        for (GameObject gameObject : gameObjects) {
            if (gameObject instanceof Trackable && gameObject instanceof Shooter && 
                gameObject instanceof Positionable && gameObject instanceof Damageable) {
                
                Trackable trackable = (Trackable) gameObject;
                Shooter shooter = (Shooter) gameObject;
                Positionable positionable = (Positionable) gameObject;
                Damageable damageable = (Damageable) gameObject;
                
                if (!damageable.isDestroyed() && trackable.canSeePlayer(player.getX(), player.getY()) && 
                    trackable.canSeePlayerInCurrentDirection(player.getX(), player.getY()) && shooter.canShoot()) {
                    
                    float[] barrelPos = positionable.getBarrelPosition();
                    double deltaX = player.getX() - gameObject.getX();
                    double deltaY = player.getY() - gameObject.getY();
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
        
        collisionSystem.checkBulletCollisions(bullets, player, gameObjects, foliages, explosions);
    }
    
    private void checkAmmoPowerUpCollection() {
        for (GameObject powerUp : ammoPowerUps) {
            if (powerUp instanceof AmmoPowerUp) {
                AmmoPowerUp ammo = (AmmoPowerUp) powerUp;
                if (!ammo.isCollected() && ammo.checkSpriteCollision(player.getX(), player.getY(), 12, 12)) {
                    ammo.collect();
                    player.collectAmmoPowerUp();
                }
            }
        }
    }
    
    private void handleGrenades(InputState input) {
        if (input.grenadePressed && player.canThrowGrenade()) {
            float[] gunPos = player.getGunBarrelPosition();
            
            // Create all collidable objects list for grenade collision
            List<GameObject> allCollidables = new ArrayList<>();
            allCollidables.addAll(gameObjects);
            allCollidables.addAll(foliages);
            allCollidables.addAll(ammoPowerUps);
            
            grenades.add(new Grenade(gunPos[0], gunPos[1], (float)input.worldMouseX, (float)input.worldMouseY, allCollidables));
            player.throwGrenade();
        }
    }
    
    private void updateGrenades() {
        Iterator<Grenade> grenadeIter = grenades.iterator();
        while (grenadeIter.hasNext()) {
            Grenade grenade = grenadeIter.next();
            grenade.update(GameConfig.DELTA_TIME);
            
            if (grenade.shouldExplode()) {
                // Create explosion at grenade position
                explosions.add(new Explosion(grenade.getX(), grenade.getY()));
                
                // Damage enemies in explosion radius
                for (GameObject gameObject : gameObjects) {
                    if (gameObject instanceof Damageable) {
                        Damageable damageable = (Damageable) gameObject;
                        float distance = new Vec2(gameObject.getX(), gameObject.getY()).distance(new Vec2(grenade.getX(), grenade.getY()));
                        if (distance <= 64) { // Explosion radius
                            damageable.takeDamage(GameConfig.GRENADE_EXPLOSION_DAMAGE);
                        }
                    }
                }
                
                grenadeIter.remove();
            } else if (!grenade.isActive()) {
                grenadeIter.remove();
            }
        }
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
    public List<Grenade> getGrenades() { return grenades; }
    public List<GameObject> getGameObjects() { return gameObjects; }
    public List<GameObject> getFoliages() { return foliages; }
    public List<GameObject> getAmmoPowerUps() { return ammoPowerUps; }
    public List<Explosion> getExplosions() { return explosions; }
    public float getCameraX() { return camera.x(); }
    public float getCameraY() { return camera.y(); }
    public Vec2 getCamera() { return camera; }
    public float getMapWidth() { return GameConfig.MAP_WIDTH; }
    public float getMapHeight() { return GameConfig.MAP_HEIGHT; }
    public float getScreenWidth() { return GameConfig.SCREEN_WIDTH; }
    public float getScreenHeight() { return GameConfig.SCREEN_HEIGHT; }
}
