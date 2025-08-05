package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.game.service.GeodesService;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

public class GeodesMenu extends InventoryUI {
    public GeodesMenu(GamePlayer player, GeodeCrusher crusher) {
        super("           MineClick Geodes", 36);

        addAllowedClickType(ClickType.RIGHT);

        // Furnace
        ItemUI furnace = new ItemUI(ItemBuilder.builder()
                .material(Material.FURNACE)
                .title(ChatColor.GREEN + "Geodes furnace")
                .lore(ChatColor.GRAY + "Merge same geodes together")
                .lore("")
                .lore(ChatColor.GOLD + "Click to open"));
        furnace.setClickConsumer(inventoryClickPack -> {
            new GeodesFurnaceMenu(player, crusher);
            player.clickSound();
        });
        setItem(4, furnace);

        // Geodes
        int position = 20;
        for (Rarity rarity : Rarity.values()) {
            if (rarity.equals(Rarity.SPECIAL)) continue;

            int count = GeodesService.i().getCount(player, rarity);
            ItemUI itemUI = new ItemUI(count > 0 ? rarity.getSkin() : MenuUtil.LOCKED_SKIN);
            itemUI.setTitle(rarity.getGeodeName() + ChatColor.YELLOW + " geode");
            itemUI.addLore(ChatColor.GRAY + "Count: " + ChatColor.GREEN + count);
            itemUI.addLore(" ");
            if (count > 0) {
                itemUI.addLore(ChatColor.YELLOW + "Left-click to open 1");
                if (count > 1) {
                    itemUI.addLore(ChatColor.YELLOW + "Right-click to open all");
                }
            } else {
                itemUI.addLore(ChatColor.GRAY + "Get geodes from ascending");
                itemUI.addLore(ChatColor.GRAY + "or at " + ChatColor.DARK_AQUA + "store.mineclick.net");
            }
            itemUI.setClickConsumer(inventoryClickPack -> {
                if (!crusher.open(player, rarity, inventoryClickPack.event().isRightClick())) {
                    player.noSound();
                } else {
                    player.getPlayer().getOpenInventory().close();
                }
            });

            setItem(position, itemUI);
            position++;
        }

        setItem(0, MenuUtil.getCloseMenu());
        open(player.getPlayer());
    }
}
