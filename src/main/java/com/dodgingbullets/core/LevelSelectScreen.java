package com.dodgingbullets.core;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class LevelSelectScreen {
    private List<String> availableLevels = new ArrayList<>();
    private boolean levelSelected = false;
    private String chosenLevel = null;
    
    public LevelSelectScreen() {
        loadAvailableLevels();
    }
    
    private void loadAvailableLevels() {
        try {
            URL resource = getClass().getClassLoader().getResource("maps");
            if (resource != null) {
                File mapsDir = new File(resource.toURI());
                if (mapsDir.exists() && mapsDir.isDirectory()) {
                    File[] files = mapsDir.listFiles((dir, name) -> name.endsWith(".json") && !name.equals("default.json"));
                    if (files != null) {
                        for (File file : files) {
                            availableLevels.add(file.getName());
                        }
                        Collections.sort(availableLevels);
                    }
                }
            }
        } catch (Exception e) {
            availableLevels.add("level1.json");
            availableLevels.add("level2.json");
            availableLevels.add("level3.json");
        }
        
        if (availableLevels.isEmpty()) {
            availableLevels.add("level1.json");
        }
    }
    
    public void handleClick(double mouseX, double mouseY) {
        // Each level button is 200x40 pixels, centered horizontally
        float screenWidth = GameConfig.SCREEN_WIDTH;
        float screenHeight = GameConfig.SCREEN_HEIGHT;
        float buttonWidth = 200;
        float buttonHeight = 40;
        float startX = (screenWidth - buttonWidth) / 2;
        float startY = 270;
        
        for (int i = 0; i < availableLevels.size(); i++) {
            float buttonY = startY - i * 60;
            if (mouseX >= startX && mouseX <= startX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                chosenLevel = "maps/" + availableLevels.get(i);
                levelSelected = true;
                break;
            }
        }
    }
    
    public boolean isLevelSelected() {
        return levelSelected;
    }
    
    public String getChosenLevel() {
        return chosenLevel;
    }
    
    public List<String> getAvailableLevels() {
        return availableLevels;
    }
}
