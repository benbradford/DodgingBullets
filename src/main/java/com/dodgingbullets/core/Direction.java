package com.dodgingbullets.core;

public enum Direction {
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    RIGHT("right"),
    UP_LEFT("upleft"),
    UP_RIGHT("upright"),
    DOWN_LEFT("downleft"),
    DOWN_RIGHT("downright");
    
    private final String prefix;
    
    Direction(String prefix) {
        this.prefix = prefix;
    }
    
    public String getPrefix() {
        return prefix;
    }
}
