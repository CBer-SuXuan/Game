package net.mineclick.game.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.game.type.geode.*;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.RandomCollection;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor
public enum Rarity {
    COMMON(new double[]{0.65, 0.56, 0.45, 0, 0}, "Stone", "MC-G001", 0, "I", ChatColor.GRAY, Common::new, Skulls.GEODE_COMMON, Pair.of(5, 15), Pair.of(1, 3), Pair.of(5, 10)),
    UNCOMMON(new double[]{0.35, 0.35, 0.35, 0.45, 0}, "Opal", "MC-G002", 20, "II", ChatColor.BLUE, Uncommon::new, Skulls.GEODE_UNCOMMON, Pair.of(15, 45), Pair.of(3, 6), Pair.of(10, 20)),
    RARE(new double[]{0, 0.09, 0.15, 0.35, 0.50}, "Quartz", "MC-G003", 80, "III", ChatColor.DARK_GREEN, Rare::new, Skulls.GEODE_RARE, Pair.of(45, 135), Pair.of(6, 12), Pair.of(20, 40)),
    VERY_RARE(new double[]{0, 0, 0.05, 0.16, 0.35}, "Magma", "MC-G004", 250, "IV", ChatColor.GOLD, VeryRare::new, Skulls.GEODE_VERY_RARE, Pair.of(135, 405), Pair.of(12, 24), Pair.of(40, 80)),
    LEGENDARY(new double[]{0, 0, 0, 0.04, 0.15}, "Ender", "MC-G005", 600, "V", ChatColor.DARK_PURPLE, Legendary::new, Skulls.GEODE_LEGENDARY, Pair.of(405, 1215), Pair.of(24, 48), Pair.of(80, 160)),
    SPECIAL(new double[]{0, 0.09, 0.15, 0.35, 0.50}, "Special", "MC-G006", 0, "", ChatColor.DARK_RED, Legendary::new, Skulls.GEODE_SPECIAL, Pair.of(45, 135), Pair.of(6, 12), Pair.of(20, 40)); // same stats as RARE

    private static RandomCollection<Rarity> collection;

    private final double[] chance;
    private final String geodeName;
    private final String storeId;
    private final int furnaceCost;
    private final String number;
    private final ChatColor color;
    private final BiFunction<GeodeCrusher, GamePlayer, GeodeAnimation> animation;
    private final String skin;
    private final Pair<Integer, Integer> schmepls;
    private final Pair<Integer, Integer> parts;
    private final Pair<Integer, Integer> exp;

    public static Rarity random() {
        if (collection == null) {
            collection = new RandomCollection<>();
            for (Rarity rarity : values()) {
                if (rarity.equals(SPECIAL)) continue;
                collection.add(rarity.getChance(), rarity);
            }
        }

        return collection.next();
    }

    public double getChance() {
        return Arrays.stream(chance).filter(chance -> chance > 0).findFirst().orElse(0);
    }

    public double getChance(Rarity rarity) {
        if (rarity.equals(SPECIAL)) {
            rarity = RARE; // Same stats as rare
        }
        return chance[rarity.ordinal()];
    }

    public String getName() {
        return color + (number.isEmpty() ? "" : number + " ") + StringUtils.capitalize(name().toLowerCase().replace("_", " "));
    }

    public String getGeodeName() {
        return color + geodeName;
    }
}
