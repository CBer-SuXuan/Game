package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.model.worker.WorkerConfiguration;
import net.mineclick.game.service.WorkersService;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.ListIterator;

public class WorkerCookiesMenu extends InventoryUI {
    public WorkerCookiesMenu(GamePlayer player) {
        super("   MineClick Worker Settings", 54);

        player.clickSound();

        ListIterator<WorkerType> iterator = Arrays.asList(WorkerType.values()).listIterator();
        for (int row = 2; row < 4; row++) {
            for (int col = 2; col < 7; col++) {
                WorkerType type = iterator.hasNext() ? iterator.next() : null;
                if (type == null || !WorkersService.i().getConfigurations().containsKey(type))
                    break;

                ItemUI itemUI = new ItemUI(Material.PLAYER_HEAD);
                itemUI.setClickConsumer(inventoryClickPack -> {
                    if (player.getWorkers().containsKey(type)) {
                        Worker worker = player.getWorkers().get(type);
                        worker.setNoAutoCookies(!worker.isNoAutoCookies());

                        player.clickSound();
                    } else {
                        player.noSound();
                    }
                });
                itemUI.setUpdateConsumer(item -> {
                    item.setLore();
                    boolean unlocked = player.getWorkers().containsKey(type);
                    if (unlocked) {
                        boolean noAutoCookies = player.getWorkers().get(type).isNoAutoCookies();
                        WorkerConfiguration configuration = WorkersService.i().getConfigurations().get(type);
                        configuration.setHeadItem(item);
                        item.setTitle((noAutoCookies ? ChatColor.RED : ChatColor.GREEN) + configuration.getName());

                        item.addLore(" ");
                        item.addLore(ChatColor.DARK_GREEN + "Automatically give cookies: " + (noAutoCookies ? ChatColor.RED + "no" : ChatColor.GREEN + "yes"));
                        item.addLore(ChatColor.GOLD + "Click to change");
                    } else {
                        MenuUtil.setLockedSkull(item);
                    }
                });

                setItem(row * 9 + col, itemUI);
            }
        }

        setItem(0, MenuUtil.getCloseMenu(p -> {
            p.getSkillsMenu().open();
            p.clickSound();
        }));
        open(player.getPlayer());
    }
}
