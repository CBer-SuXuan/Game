package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.GlobalPlugin;
import net.mineclick.global.model.ServerModel;
import net.mineclick.global.service.ServersService;
import net.mineclick.global.service.TpService;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LobbyTpMenu extends InventoryUI {
    public static final ItemStack MENU_ITEM = ItemBuilder.builder()
            .material(Material.COMPASS)
            .title(ChatColor.YELLOW + "Lobby Selector" + ChatColor.GRAY + "" + ChatColor.ITALIC + " right-click")
            .build().toItem();

    public LobbyTpMenu(GamePlayer player) {
        super("    MineClick Lobby Selector", 9);

        List<ServerModel> servers = ServersService.i().getRunningServers(true);
        for (int i = 0; i < servers.size() && i < 9; i++) {
            ServerModel server = servers.get(i);
            boolean current = server.getId().equals(GlobalPlugin.i().getServerId());

            ItemStack stack = ItemBuilder.builder()
                    .material(current ? Material.YELLOW_CONCRETE : Material.LIME_CONCRETE)
                    .title((current ? ChatColor.RED : ChatColor.GREEN) + "Lobby " + (i + 1))
                    .lore(ChatColor.GRAY + (current ? "Current lobby" : "Click to connect"))
                    .build().toItem();
            ItemUI item = new ItemUI(stack, clickPack -> {
                if (!current) {
                    TpService.i().tpToServer(player, server.getId());

                    player.getPlayer().closeInventory();
                }
            });
            item.setUpdateConsumer(itemUI -> {
                if (!server.getStatus().isRunning()) {
                    player.getPlayer().closeInventory();
                    schedule(1, () -> new LobbyTpMenu(player));
                    return;
                }

                int players = server.getPlayers();

                item.setLore(ChatColor.GRAY + (current ? "Current lobby" : "Click to connect"));
                item.addLore(" ");
                item.addLore(ChatColor.GREEN.toString() + players + ChatColor.GRAY + " players");
                item.setAmount(players);
            });

            setItem(i, item);
        }

        open(player.getPlayer());
    }
}
