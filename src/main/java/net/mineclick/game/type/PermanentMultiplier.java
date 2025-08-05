package net.mineclick.game.type;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;

@Getter
public enum PermanentMultiplier {
    NONE(Material.AIR, 0, 1),
    COAL(Material.COAL_ORE, 300, 1.5),
    IRON(Material.IRON_ORE, 700, 2),
    REDSTONE(Material.REDSTONE_ORE, 1500, 2.5),
    GOLD(Material.GOLD_ORE, 4000, 3.5),
    LAPIS(Material.LAPIS_ORE, 9500, 4.5),
    DIAMOND(Material.DIAMOND_ORE, 19000, 6),
    EMERALD(Material.EMERALD_ORE, 21000, 7.5),
    QUARTZ(Material.NETHER_QUARTZ_ORE, 50000, 10),
    STAR(Material.NETHER_STAR, 150000, 20),
    CRYSTAL(Material.END_CRYSTAL, 500000, 40),
    AMETHYST(Material.AMETHYST_SHARD, 1000000, 60),
    SCULK(Material.SCULK, 5000000, 80),
    END_GATEWAY(Material.END_STONE, 10000000, 100, "End Gateway");

    private final Material material;
    private final long cost;
    private final double multiplier;
    private final String name;

    PermanentMultiplier(Material material, long cost, double multiplier, String name) {
        this.material = material;
        this.cost = cost;
        this.multiplier = multiplier;
        this.name = name;
    }

    PermanentMultiplier(Material material, long cost, double multiplier) {
        this(material, cost, multiplier, null);
    }

    public static PermanentMultiplier next(PermanentMultiplier multiplier) {
        if (multiplier.ordinal() + 1 == values().length)
            return null;

        return values()[multiplier.ordinal() + 1];
    }

    public String getName() {
        if (name != null)
            return name;
        return StringUtils.capitalize(toString().toLowerCase());
    }
}
