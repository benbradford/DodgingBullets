package com.dodgingbullets.core;

import com.dodgingbullets.gameobjects.*;
import com.dodgingbullets.gameobjects.effects.Explosion;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CollisionSystem {
    
    public void checkBulletCollisions(List<Bullet> bullets, Player player, List<GameObject> turrets, 
                                    List<GameObject> foliages, List<Explosion> explosions) {
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            
            // Check foliage collision
            if (checkFoliageCollision(bullet, foliages)) {
                bulletIter.remove();
                continue;
            }
            
            // Check turret collision (player bullets only)
            if (bullet.isPlayerBullet() && checkTurretCollision(bullet, turrets, explosions)) {
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
    
    private boolean checkTurretCollision(Bullet bullet, List<GameObject> turrets, List<Explosion> explosions) {
        for (GameObject turret : turrets) {
            if (turret instanceof Positionable && 
                ((Positionable) turret).isInSpriteHitbox(bullet.getX(), bullet.getY())) {
                if (turret instanceof Damageable) {
                    Damageable damageable = (Damageable) turret;
                    boolean wasDestroyed = damageable.isDestroyed();
                    damageable.takeDamage(GameConfig.PLAYER_DAMAGE);
                    
                    if (!wasDestroyed && damageable.isDestroyed()) {
                        explosions.add(new Explosion(turret.getX(), turret.getY()));
                    }
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
