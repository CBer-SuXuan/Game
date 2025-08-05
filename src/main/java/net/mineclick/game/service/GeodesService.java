package net.mineclick.game.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.mineclick.game.Game;
import net.mineclick.game.menu.GeodesMenu;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.game.model.GeodeItem;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.util.GeodesCollection;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.SingletonInit;
import net.mineclick.global.util.Triple;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SingletonInit
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeodesService {
    private static GeodesService i;
    private final GeodesCollection items = new GeodesCollection();

    public static GeodesService i() {
        return i == null ? i = new GeodesService() : i;
    }

    /**
     * Add a geode item to the list of all the possible items a geode can drop.
     * <br>
     * <b>The list does not check for duplicates.</b>
     *
     * @param item The geode item
     */
    public void addToListOfGeodes(GeodeItem item) {
        items.add(item);
    }

    /**
     * Add a random geode to the player
     *
     * @param player The player
     * @return The rarity of the geode that was added
     */
    public Rarity addGeode(GamePlayer player) {
        Rarity rarity = Rarity.random();
        addGeode(player, rarity, 1);

        return rarity;
    }

    /**
     * Add an amount of geodes to the player of a specific rarity
     *
     * @param player The player
     * @param rarity The rarity
     * @param amount The amount to add
     */
    public void addGeode(GamePlayer player, Rarity rarity, int amount) {
        player.getGeodes().compute(rarity, (r, count) -> count == null ? amount : count + amount);
    }

    /**
     * @param player The player
     * @return Total amount of geodes
     */
    public int getTotal(GamePlayer player) {
        return player.getGeodes().values().stream().mapToInt(i -> i).sum();
    }

    /**
     * Open the geode menu
     *
     * @param player       The player
     * @param geodeCrusher The geode crusher
     */
    public void openMenu(GamePlayer player, GeodeCrusher geodeCrusher) {
        new GeodesMenu(player, geodeCrusher);
    }

    /**
     * @param player The player
     * @param rarity The rarity type
     * @return Number of geodes of the given type
     */
    public int getCount(GamePlayer player, Rarity rarity) {
        return player.getGeodes().getOrDefault(rarity, 0);
    }

    /**
     * Open a geode of the given rarity
     *
     * @param player  The player
     * @param rarity  The geode rarity
     * @param openAll Whether to open all geodes of the same rarity
     * @return The geode items that were found
     */
    public List<GeodeItem> openGeode(GamePlayer player, Rarity rarity, boolean openAll) {
        Integer count = player.getGeodes().get(rarity);
        if (count == null || count <= 0) return null;

        player.getGeodes().put(rarity, openAll ? 0 : Math.max(0, count - 1));

        int parts = 0;
        int schmepls = 0;
        int exp = 0;
        Map<GeodeItem, Pair<Integer, Boolean>> geodeItems = new HashMap<>();
        for (int i = 0; i < (openAll ? count : 1); i++) {
            GeodeItem item = items.next(rarity);
            if (item == null) continue;

            boolean duplicate = item.getDuplicateFunction().apply(player);
            if (!duplicate) {
                item.getConsumer().accept(player);
            }
            geodeItems.compute(item, (geodeItem, pair) -> pair == null ? Pair.of(1, duplicate) : Pair.of(pair.key() + 1, duplicate || pair.value()));

            // parts - only when a duplicate
            if (duplicate) {
                parts += getRandomIntInRange(item.getRarity().getParts());
            }

            // schmepls
            schmepls += getRandomIntInRange(rarity.getSchmepls());

            // exp
            exp += getRandomIntInRange(rarity.getExp());
        }

        List<Triple<Integer, String, Boolean>> messageList = new ArrayList<>();
        for (Map.Entry<GeodeItem, Pair<Integer, Boolean>> entry : geodeItems.entrySet()) {
            GeodeItem item = entry.getKey();
            int duplicates = entry.getValue().key();
            boolean duplicate = entry.getValue().value();

            messageList.add(Triple.of(0, duplicates + "x " + item.getName() + " " + item.getRarity().getName() + (duplicate ? ChatColor.DARK_GRAY + " [duplicate]" : ""), false));
        }

        if (parts > 0) {
            PowerupService.i().addParts(player, parts);
            messageList.add(Triple.of(parts, "Powerup part", true));
        }

        player.addSchmepls(schmepls);
        messageList.add(Triple.of(schmepls, "Schmepl", true));

        player.addExp(exp);
        messageList.add(Triple.of(exp, "EXP", false));

        AchievementsService.i().incrementProgress(player, "opengeodes", openAll ? count : 1);

        // inform about duplicates
        if (parts > 0) {
            messageList.add(Triple.of(0, " ", false));
            messageList.add(Triple.of(0, ChatColor.GRAY.toString() + ChatColor.ITALIC + "Duplicates get converted to Powerup parts", false));
        }

        player.sendListMessage(rarity.getGeodeName() + ChatColor.YELLOW + " geode(s) " + "contents:", messageList, true);
        return new ArrayList<>(geodeItems.keySet());
    }

    private int getRandomIntInRange(Pair<Integer, Integer> pair) {
        int min = pair.key();
        int max = pair.value();

        return Game.getRandom().nextInt((max - min) + 1) + min;
    }
}
