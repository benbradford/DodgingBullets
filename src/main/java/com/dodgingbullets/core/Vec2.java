package com.dodgingbullets.core;

public class Vec2 {
    private final float x;
    private final float y;
    
    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public float x() { return x; }
    public float y() { return y; }
    
    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }
    
    public Vec2 subtract(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }
    
    public Vec2 multiply(float scalar) {
        return new Vec2(x * scalar, y * scalar);
    }
    
    public float distance(Vec2 other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    public double angle() {
        return Math.atan2(y, x);
    }
    
    public Vec2 clamp(Vec2 min, Vec2 max) {
        return new Vec2(
            Math.max(min.x, Math.min(max.x, x)),
            Math.max(min.y, Math.min(max.y, y))
        );
    }
    
    public static Vec2 fromAngle(double angle, float magnitude) {
        return new Vec2(
            (float) (Math.cos(angle) * magnitude),
            (float) (Math.sin(angle) * magnitude)
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vec2 vec2 = (Vec2) obj;
        return Float.compare(vec2.x, x) == 0 && Float.compare(vec2.y, y) == 0;
    }
    
    @Override
    public int hashCode() {
        return Float.hashCode(x) * 31 + Float.hashCode(y);
    }
}
