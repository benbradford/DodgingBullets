package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.enemies.Bear;
import com.dodgingbullets.gameobjects.effects.Explosion;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CollisionSystem {
    
    public void checkBulletCollisions(List<Bullet> bullets, Player player, List<GameObject> gameObjects, 
                                    List<GameObject> foliages, List<Explosion> explosions) {
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            
            // Check foliage collision
            if (checkFoliageCollision(bullet, foliages)) {
                bulletIter.remove();
                continue;
            }
            
            // Check game object collision (player bullets only)
            if (bullet.isPlayerBullet() && checkGameObjectCollision(bullet, gameObjects, explosions)) {
                bulletIter.remove();
                continue;
            }
            
            // Check player collision (enemy bullets only)
            if (!bullet.isPlayerBullet() && checkPlayerCollision(bullet, player)) {
                player.takeDamage(GameConfig.ENEMY_DAMAGE);
                bulletIter.remove();
                continue;
            }
            
            if (bullet.isExpired()) {
                bulletIter.remove();
            }
        }
    }
    
    public void checkExplosionCollisions(List<Explosion> explosions, Player player) {
        for (Explosion explosion : explosions) {
            if (explosion.checkSpriteCollision(player.getX() - 6, player.getY() - 32, 12, 64)) {
                player.takeDamage(GameConfig.EXPLOSION_DAMAGE);
            }
        }
    }
    
    private boolean checkFoliageCollision(Bullet bullet, List<GameObject> foliages) {
        for (GameObject foliage : foliages) {
            if (foliage instanceof Collidable && 
                ((Collidable) foliage).checkSpriteCollision(bullet.getX(), bullet.getY(), 1, 1)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean checkGameObjectCollision(Bullet bullet, List<GameObject> gameObjects, List<Explosion> explosions) {
        for (GameObject gameObject : gameObjects) {
            if (gameObject instanceof Positionable && gameObject instanceof Damageable &&
                !((Damageable) gameObject).isDestroyed() &&
                ((Positionable) gameObject).isInSpriteHitbox(bullet.getX(), bullet.getY())) {
                
                Damageable damageable = (Damageable) gameObject;
                boolean wasDestroyed = damageable.isDestroyed();
                damageable.takeDamage(GameConfig.PLAYER_DAMAGE);
                
                // Only create explosions for non-Bear objects
                if (!wasDestroyed && damageable.isDestroyed() && !(gameObject instanceof Bear)) {
                    explosions.add(new Explosion(gameObject.getX(), gameObject.getY()));
                }
                return true;
            }
        }
        return false;
    }
    
    private boolean checkPlayerCollision(Bullet bullet, Player player) {
        float bulletX = bullet.getX();
        float bulletY = bullet.getY();
        return bulletX >= player.getX() - 6 && bulletX <= player.getX() + 6 && 
               bulletY >= player.getY() - 32 && bulletY <= player.getY() + 32;
    }
}
