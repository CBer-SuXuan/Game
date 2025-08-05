package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.game.type.powerup.PowerupCategory;
import net.mineclick.game.type.powerup.PowerupType;

@Data
public class PowerupProgress {
    private PowerupType selectedType;
    private int parts;
    private int level;
    private SelectionType selection = SelectionType.MANUAL;

    private transient PowerupCategory category;
    private transient long lastCalculatedBest = 0;

    public int getPartsToNextLevel() {
        // parts=3*multiplier^level
        return (int) (3 * Math.pow(category.getCostMultiplier(), level)) - parts;
    }

    public double getPower() {
        // goldTime=powerPerLevel*level
        return category.getPowerPerLevel() * level;
    }

    public int getSchmeplsCost() {
        return (int) (category.getSchmeplsBaseCost() * Math.pow(level + 1, category.getCostMultiplier()));
    }

    public double getNextLevelPower() {
        return category.getPowerPerLevel() * (level + 1);
    }

    public enum SelectionType {
        MANUAL,
        RANDOM,
        BEST
    }
}
