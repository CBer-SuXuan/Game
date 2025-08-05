package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.game.service.GeodesService;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;

public class GeodesFurnaceMenu extends InventoryUI {
    private final GamePlayer player;
    ItemUI plusSign = new ItemUI(ItemBuilder.builder().skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjBiNTVmNzQ2ODFjNjgyODNhMWMxY2U1MWYxYzgzYjUyZTI5NzFjOTFlZTM0ZWZjYjU5OGRmMzk5MGE3ZTcifX19").title(ChatColor.GREEN.toString() + ChatColor.BOLD + "+"));
    ItemUI equalSign = new ItemUI(ItemBuilder.builder().skull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDc3MzE1NTMwNmM5ZDJkNThiMTQ5NjczOTUxY2JjNjY2NmFlZjg3YjhmODczNTM4ZmM4NTc0NWYwMWI1MSJ9fX0=").title(ChatColor.GREEN.toString() + ChatColor.BOLD + "="));

    public GeodesFurnaceMenu(GamePlayer player, GeodeCrusher crusher) {
        super("     MineClick Geodes Furnace", 54);
        this.player = player;

        // Geodes
        int row = 1;
        for (Rarity rarity : Rarity.values()) {
            if (rarity.equals(Rarity.SPECIAL) || rarity.equals(Rarity.COMMON)) continue;
            Rarity previousRarity = Rarity.values()[rarity.ordinal() - 1];

            // Plus and equal sign
            setItem(row * 9 + 3, plusSign);
            setItem(row * 9 + 5, equalSign);

            // First geode
            ItemUI firstGeode = new ItemUI(MenuUtil.LOCKED_SKIN);
            firstGeode.setUpdateConsumer(itemUI -> {
                int count = getCount(previousRarity);
                itemUI.setSkin(count > 0 ? previousRarity.getSkin() : MenuUtil.LOCKED_SKIN);
                itemUI.setTitle(previousRarity.getGeodeName() + (count > 0 ? ChatColor.YELLOW : ChatColor.RED) + " geode");

                itemUI.setLore();
                itemUI.addLore("");
                itemUI.addLore(ChatColor.GRAY + "Count: " + (count > 1 ? ChatColor.GREEN : ChatColor.RED) + count);
            });
            setItem(row * 9 + 2, firstGeode);

            // Second geode
            ItemUI secondGeode = new ItemUI(MenuUtil.LOCKED_SKIN);
            secondGeode.setUpdateConsumer(itemUI -> {
                int count = getCount(previousRarity);
                itemUI.setSkin(count > 1 ? previousRarity.getSkin() : MenuUtil.LOCKED_SKIN);
                itemUI.setTitle(previousRarity.getGeodeName() + (count > 1 ? ChatColor.YELLOW : ChatColor.RED) + " geode");

                itemUI.setLore();
                itemUI.addLore("");
                itemUI.addLore(ChatColor.GRAY + "Count: " + (count > 1 ? ChatColor.GREEN : ChatColor.RED) + count);
            });
            setItem(row * 9 + 4, secondGeode);

            // Result
            ItemUI resultGeode = new ItemUI(MenuUtil.LOCKED_SKIN);
            resultGeode.setUpdateConsumer(itemUI -> {
                int count = getCount(previousRarity);
                int resultCount = getCount(rarity);
                itemUI.setSkin(count > 1 ? rarity.getSkin() : MenuUtil.LOCKED_SKIN);
                itemUI.setTitle(rarity.getGeodeName() + (count > 1 ? ChatColor.YELLOW : ChatColor.RED) + " geode");

                itemUI.setLore();
                itemUI.addLore(ChatColor.GRAY + "Count: " + (resultCount > 0 ? ChatColor.GREEN : ChatColor.RED) + resultCount);
                itemUI.addLore(" ");
                itemUI.addLore(MenuUtil.prerequisite(count > 1, count + "/2 " + previousRarity.getGeodeName() + " geodes", " "));
                itemUI.addLore(MenuUtil.prerequisite(rarity.getFurnaceCost() <= player.getSchmepls(), Formatter.format(rarity.getFurnaceCost()) + " schmepls", " "));
                if (count > 1 && rarity.getFurnaceCost() <= player.getSchmepls()) {
                    itemUI.addLore(" ");
                    itemUI.addLore(ChatColor.YELLOW + "Click to merge");
                }
            });
            resultGeode.setClickConsumer(inventoryClickPack -> {
                int count = getCount(previousRarity);
                if (count > 1 && rarity.getFurnaceCost() <= player.getSchmepls()) {
                    player.chargeSchmepls(rarity.getFurnaceCost());

                    player.getGeodes().put(previousRarity, Math.max(0, count - 2));
                    GeodesService.i().addGeode(player, rarity, 1);

                    player.popSound();
                } else {
                    player.noSound();
                }
            });
            setItem(row * 9 + 6, resultGeode);

            row++;
        }

        setItem(0, MenuUtil.getCloseMenu(p -> GeodesService.i().openMenu(player, crusher)));
        open(player.getPlayer());
    }

    private int getCount(Rarity rarity) {
        return GeodesService.i().getCount(player, rarity);
    }
}
