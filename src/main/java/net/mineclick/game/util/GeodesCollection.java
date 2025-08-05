package net.mineclick.game.util;

import net.mineclick.game.Game;
import net.mineclick.game.model.GeodeItem;
import net.mineclick.game.type.Rarity;

import java.util.*;

public class GeodesCollection {
    private final List<GeodeItem> items = new ArrayList<>();
    private final Random random = Game.getRandom();
    private final Map<Rarity, Double> totalsMap = new HashMap<>();

    public GeodesCollection add(GeodeItem geodeItem) {
        for (Rarity rarity : Rarity.values()) {
            double total = totalsMap.getOrDefault(rarity, 0D);
            total += geodeItem.getRarity().getChance(rarity) * geodeItem.getRarityOffset();
            totalsMap.put(rarity, total);
        }

        items.add(geodeItem);
        return this;
    }

    public GeodeItem next(Rarity geodeRarity) {
        if (items.isEmpty()) return null;

        double r = random.nextDouble() * totalsMap.get(geodeRarity);
        double countWeight = 0;
        for (GeodeItem item : items) {
            countWeight += item.getRarity().getChance(geodeRarity);
            if (countWeight >= r)
                return item;
        }

        return null;
    }
}
