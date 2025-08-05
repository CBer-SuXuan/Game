package net.mineclick.game.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Color;

@Getter
@RequiredArgsConstructor
public enum BoosterType {
    GOLD_BOOSTER(
            "Gold Income Booster",
            "Increase everyone's gold income by " + ChatColor.GREEN + "x2",
            2,
            60,
            Color.ORANGE,
            "MC-B001"
    ),
    PICKAXE_BOOSTER(
            "Pickaxe Income Booster",
            "Increase everyone's pickaxe income by " + ChatColor.GREEN + "x10",
            10,
            60,
            Color.RED,
            "MC-B002"
    ),
    POWERUPS_BOOSTER(
            "Powerups Booster",
            "Increase everyone's powerup income by " + ChatColor.GREEN + "x2",
            2,
            60,
            Color.TEAL,
            "MC-B003"
    ),
    SUPER_BLOCK_BOOSTER(
            "Super Block Booster",
            "Increase everyone's chance to mine a Super Block by " + ChatColor.GREEN + "25%",
            1.25,
            60,
            Color.FUCHSIA,
            "MC-B004"
    ),
    CHEAP_WORKER_BOOSTER(
            "Cheap Workers Booster",
            "Everyone's workers are " + ChatColor.GREEN + "25%" + ChatColor.GRAY + " cheaper",
            0.75,
            60,
            Color.GREEN,
            "MC-B005"
    ),
    CHEAP_BUILDINGS_BOOSTER(
            "Cheap Buildings Booster",
            "Everyone's buildings are " + ChatColor.GREEN + "50%" + ChatColor.GRAY + " cheaper",
            0.50,
            60,
            Color.BLUE,
            "MC-B006"
    );

    private final String name;
    private final String description;
    private final double boost;
    private final int durationMin;
    private final Color color;
    private final String storeId;

    public static BoosterType get(String name) {
        for (BoosterType booster : values()) {
            if (booster.toString().equalsIgnoreCase(name))
                return booster;
        }

        return null;
    }
}
