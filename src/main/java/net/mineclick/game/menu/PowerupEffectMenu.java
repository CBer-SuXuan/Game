package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.PowerupProgress;
import net.mineclick.game.service.PowerupService;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.powerup.PowerupCategory;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.Skins;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class PowerupEffectMenu extends InventoryUI {
    private static final String RANDOM_ON_SKULL = Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGRhZDA3MzA1MTA2YzFhNzU1YjI1NDY4ZGEwOGIzMDk2MTY4YjU1ZDEyZGEwZmRhNjQ0OTU5NTlhYmU5OTIxNCJ9fX0=");
    private static final String RANDOM_OFF_SKULL = Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzEyNjExNjU2M2U5MDRjZGU3ZjUyYWUwZmI1ZTA3NjZlNjBhYmY0NzU3OTU3ZGU5ZGQzYjA2ZWRmMWY4YmQ4ZSJ9fX0=");

    public PowerupEffectMenu(GamePlayer player, PowerupCategory category) {
        super(category.getMenuTitle(), 45);

        PowerupProgress progress = PowerupService.i().getProgress(player, category);
        PowerupType selectedType = progress.getSelectedType();
        int position = 11;
        for (PowerupType type : PowerupType.values()) {
            if (!type.getCategory().equals(category)) continue;

            boolean selected = type.equals(selectedType) && !progress.getSelection().equals(PowerupProgress.SelectionType.RANDOM);
            boolean unlocked = player.getUnlockedPowerups().contains(type);
            ItemUI itemUI = new ItemUI(MenuUtil.LOCKED_SKIN);
            itemUI.setClickConsumer(inventoryClickPack -> {
                if (unlocked && !selected) {
                    PowerupService.i().setSelectedPowerup(player, category, type);
                    player.clickSound();
                    PowerupService.i().openEffectMenu(player, category);
                }
            });
            if (unlocked) {
                if (type.getSkull() != null) {
                    itemUI.setSkin(type.getSkull());
                } else {
                    itemUI.setMaterial(type.getMaterial());
                }

                itemUI.setTitle((selected ? ChatColor.GREEN : ChatColor.GOLD) + type.getName());
                itemUI.addLore(ChatColor.GRAY + "Rarity: " + type.getRarity().getName());

                if (type.getPerk() != null) {
                    itemUI.addLore(" ");
                    boolean applies = type.getPerk().getPerk(player) > 0;
                    String description = type.getPerk().getDescription(player);
                    for (String s : description.split("\n")) {
                        itemUI.addLore((applies ? ChatColor.GREEN : ChatColor.GRAY) + s);
                    }
                }

                itemUI.addLore(" ");
                itemUI.addLore(selected ? ChatColor.GREEN + "Selected" : ChatColor.YELLOW + "Click to select");
            } else {
                boolean special = type.getRarity().equals(Rarity.SPECIAL);
                itemUI.setTitle((special ? ChatColor.DARK_RED : ChatColor.DARK_GRAY) + "???");
                itemUI.addLore(" ");
                if (special) {
                    String holiday = type.getHoliday() != null ? type.getHoliday().getName() + " " : "";
                    itemUI.addLore(ChatColor.GRAY + holiday + "Special Powerup Effect");
                    if (type.getHoliday() != null && type.getHoliday().isNow()) {
                        itemUI.addLore(ChatColor.GREEN + "Can be found in geodes for a limited time!");
                    } else {
                        itemUI.addLore(ChatColor.GRAY + "Cannot be found in geodes");
                    }
                } else {
                    itemUI.addLore(ChatColor.GRAY + "Find this Powerup Effect");
                    itemUI.addLore(ChatColor.GRAY + "by opening geodes");
                }
            }

            setItem(position, itemUI);
            if ((position + 3) % 9 == 0) {
                position += 4;
            }
            position++;
        }

        // Manual/most effective/random powerup effect item
        PowerupProgress.SelectionType selectionType = progress.getSelection();
        ItemUI selectionItem = new ItemUI(Material.COMMAND_BLOCK);
        selectionItem.setClickConsumer(inventoryClickPack -> {
            switch (selectionType) {
                case MANUAL:
                    progress.setSelection(PowerupProgress.SelectionType.RANDOM);
                    break;
                case RANDOM:
                    if (player.getRank().isAtLeast(Rank.PAID)) {
                        progress.setSelection(PowerupProgress.SelectionType.BEST);
                    } else {
                        progress.setSelection(PowerupProgress.SelectionType.MANUAL);
                    }
                    break;
                case BEST:
                    progress.setSelection(PowerupProgress.SelectionType.MANUAL);
                    break;
            }
            player.clickSound();

            PowerupService.i().openEffectMenu(player, category);
        });
        selectionItem.setTitle(ChatColor.GREEN + "Powerup effect selection");
        selectionItem.addLore(" ");
        selectionItem.addLore((selectionType.equals(PowerupProgress.SelectionType.MANUAL) ? ChatColor.GREEN : ChatColor.GRAY) + "- Manually select the powerup effect");
        selectionItem.addLore((selectionType.equals(PowerupProgress.SelectionType.RANDOM) ? ChatColor.GREEN : ChatColor.GRAY) + "- Use a random powerup effect");
        selectionItem.addLore((selectionType.equals(PowerupProgress.SelectionType.BEST) ? ChatColor.GREEN : ChatColor.GRAY) + "- Automatically use the best");
        selectionItem.addLore((selectionType.equals(PowerupProgress.SelectionType.BEST) ? ChatColor.GREEN : ChatColor.GRAY) + "   powerup effect " + ChatColor.LIGHT_PURPLE + "(Premium Membership)");
        selectionItem.addLore(" ");
        selectionItem.addLore(ChatColor.GOLD + "Click to switch");
        setItem(44, selectionItem);

        setItem(0, MenuUtil.getCloseMenu(p -> PowerupService.i().openMenu(p)));
        open(player.getPlayer());
    }
}
