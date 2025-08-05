package net.mineclick.game.menu;

import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.GadgetsService;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Comparator;
import java.util.stream.Collectors;

public class GadgetsMenu extends InventoryUI {
    public GadgetsMenu(GamePlayer player) {
        super("       MineClick Gadgets", 54);

        int position = 10;
        for (Gadget gadget : GadgetsService.i().getGadgets().stream().sorted(Comparator.comparing(Gadget::isSecret).reversed()).collect(Collectors.toList())) {
            if (gadget.isSecret() && !player.getLobbyData().getUnlockedGadgets().contains(gadget.getImmutableName())) {
                continue;
            }

            ItemUI itemUI = new ItemUI(GadgetsService.i().buildItem(gadget, true), clickPack -> {
                if (gadget.getImmutableName().equals(player.getLobbyData().getCurrentGadget())) {
                    return;
                }

                if (!gadget.isSecret() && !player.getRank().isAtLeast(Rank.PAID)) {
                    player.sendMessage("This gadget requires a Premium Membership!", MessageType.ERROR);
                    player.noSound();
                    return;
                }

                player.getLobbyData().setCurrentGadget(gadget.getImmutableName());
                player.updateInventory();
                player.clickSound();
            });
            itemUI.setUpdateConsumer(item -> {
                item.setLore();
                for (String s : gadget.getDescription().split("\n")) {
                    item.addLore(ChatColor.GRAY + s);
                }
                item.addLore("");

                if (player.getRank().isAtLeast(Rank.PAID) || gadget.isSecret()) {
                    boolean current = gadget.getImmutableName().equals(player.getLobbyData().getCurrentGadget());
                    item.addLore(current ? ChatColor.DARK_GRAY + "Selected" : ChatColor.YELLOW + "Click to select");
                } else {
                    item.addLore(ChatColor.RED + "Requires Premium Membership");
                    item.addLore(ChatColor.GRAY + "Get yours at " + ChatColor.AQUA + "store.mineclick.net");
                }
            });

            setItem(position, itemUI);
            if ((position + 2) % 9 == 0) {
                position += 2;
            }
            position++;

            //TODO add pages
            if (position > 53)
                return;
        }

        ItemUI removeItem = new ItemUI(ItemBuilder.builder().material(Material.TNT).title(ChatColor.RED + "Remove gadget").lore(ChatColor.GRAY + "Click to remove the gadget").build().toItem(), clickPack -> {
            if (player.getLobbyData().getCurrentGadget() != null) {
                player.clickSound();
                player.getLobbyData().setCurrentGadget(null);
                player.updateInventory();
            }
        });
        removeItem.setUpdateConsumer(item -> {
            if (player.getLobbyData().getCurrentGadget() == null) {
                item.setMaterial(Material.AIR);
            } else {
                item.setMaterial(Material.TNT);
                item.setTitle(ChatColor.RED + "Remove gadget");
                item.setLore(ChatColor.GRAY + "Click to remove the gadget");
            }
        });
        setItem(53, removeItem);

        setItem(0, MenuUtil.getCloseMenu());
        open(player.getPlayer());
    }
}
