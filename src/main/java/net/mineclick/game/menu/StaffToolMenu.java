package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.StaffTool;
import net.mineclick.game.type.StaffToolTrigger;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

public class StaffToolMenu extends InventoryUI {
    public StaffToolMenu(GamePlayer player) {
        super("Staff tool manager", 9);

        int index = 0;
        for (StaffTool tool : StaffTool.values()) {
            Optional<Map.Entry<StaffToolTrigger, StaffTool>> optional = player.getStaffData().getStaffTools().entrySet().stream().filter(s -> s.getValue().equals(tool)).findFirst();

            ItemStack stack = ItemBuilder.builder()
                    .material(optional.isPresent() ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                    .title(ChatColor.YELLOW + tool.getName())
                    .lore(ChatColor.GRAY + tool.getDescription())
                    .lore(" ")
                    .lore(ChatColor.GRAY + optional.map(e -> "Assigned to: " + ChatColor.GREEN + e.getKey().getName()).orElse("Not assigned"))
                    .lore(ChatColor.GRAY + "Click to edit")
                    .build().toItem();
            setItem(index++, new ItemUI(stack, pack -> openTriggerSelection(tool, optional.map(Map.Entry::getKey).orElse(null), player)));
        }

        open(player.getPlayer());
    }

    private void openTriggerSelection(StaffTool tool, StaffToolTrigger currentTrigger, GamePlayer player) {
        InventoryUI ui = new InventoryUI("Select the trigger...", 9);

        int index = 0;
        for (StaffToolTrigger trigger : StaffToolTrigger.values()) {
            boolean active = trigger.equals(currentTrigger);
            ItemStack stack = ItemBuilder.builder()
                    .material(active ? Material.LIME_STAINED_GLASS : Material.GRAY_STAINED_GLASS_PANE)
                    .title(ChatColor.YELLOW + trigger.getName())
                    .lore(active ? (ChatColor.GREEN + "Currently assigned") : (ChatColor.GRAY + "Click to assign"))
                    .build().toItem();
            ui.setItem(index++, new ItemUI(stack, pack -> {
                player.getStaffData().getStaffTools().put(trigger, tool);
                player.updateInventory();
                new StaffToolMenu(player);
            }));
        }

        ui.open(player.getPlayer());
    }
}
