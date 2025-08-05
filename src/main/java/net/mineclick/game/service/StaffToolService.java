package net.mineclick.game.service;

import net.mineclick.game.Game;
import net.mineclick.game.menu.StaffToolMenu;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.StaffData;
import net.mineclick.game.type.StaffTool;
import net.mineclick.game.type.StaffToolTrigger;
import net.mineclick.global.commands.Commands;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.SingletonInit;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@SingletonInit
public class StaffToolService implements Listener {
    private static StaffToolService i;

    private StaffToolService() {
        Commands.addCommand(Commands.Command.builder()
                .name("st")
                .description("Manage your staff tool")
                .minRank(Rank.STAFF)
                .callFunction((data, strings) -> {
                    if (strings.length == 0) {
                        new StaffToolMenu((GamePlayer) data);
                    } else if (strings[0].equalsIgnoreCase("custom")) {
                        String command = StringUtils.join(Arrays.copyOfRange(strings, 1, strings.length), " ");
                        ((GamePlayer) data).getStaffData().setCustomToolCommand(command);
                        return ChatColor.YELLOW + "Assigned custom command to staff tool: " + ChatColor.DARK_GRAY + command;
                    } else if (strings[0].equalsIgnoreCase("tool")) {
                        StaffData staffData = ((GamePlayer) data).getStaffData();
                        staffData.setHideTool(!staffData.isHideTool());
                        ((GamePlayer) data).updateInventory();

                        return ChatColor.YELLOW + "Staff tool is " + (staffData.isHideTool() ? "hidden" : "shown");
                    }

                    return "";
                })
                .build());

        Bukkit.getPluginManager().registerEvents(this, Game.i());
    }

    public static StaffToolService i() {
        return i == null ? i = new StaffToolService() : i;
    }

    @EventHandler
    public void on(PlayerInteractEvent e) {
        if (e.hasItem() && e.getItem().hasItemMeta() && e.getItem().getItemMeta().hasDisplayName() && e.getItem().getItemMeta().getDisplayName().contains("Staff Tool")) {
            PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
                if (!player.isRankAtLeast(Rank.STAFF))
                    return;

                for (StaffToolTrigger trigger : StaffToolTrigger.values()) {
                    if (trigger.isApplied.apply(e)) {
                        StaffTool staffTool = player.getStaffData().getStaffTools().get(trigger);
                        if (staffTool != null) {
                            staffTool.execute.accept(player);
                        }
                        return;
                    }
                }
            });
        }
    }

    @EventHandler
    public void on(PlayerDropItemEvent e) {
        ItemStack stack = e.getItemDrop().getItemStack();
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName() && stack.getItemMeta().getDisplayName().contains("Staff Tool")) {
            PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
                if (!player.isRankAtLeast(Rank.STAFF))
                    return;

                StaffTool staffTool = player.getStaffData().getStaffTools().get(StaffToolTrigger.DROP);
                if (staffTool != null) {
                    staffTool.execute.accept(player);
                }
            });
        }
    }
}
