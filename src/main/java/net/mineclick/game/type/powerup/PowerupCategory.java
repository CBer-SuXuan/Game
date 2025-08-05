package net.mineclick.game.type.powerup;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PowerupCategory {
    PICKAXE("    MineClick Pickaxe Powerup", 3, 15, 100, 0.005),
    ORB("      MineClick Orb Powerup", 2, 10, 10, 0.05);

    private final String menuTitle;
    private final double costMultiplier;
    private final int schmeplsBaseCost;
    private final double powerPerLevel;
    private final double chargeRate;
}
