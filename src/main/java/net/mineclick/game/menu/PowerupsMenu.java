package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.PowerupProgress;
import net.mineclick.game.service.GeodesService;
import net.mineclick.game.service.PowerupService;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.powerup.PowerupCategory;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.Skins;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.function.Supplier;

public class PowerupsMenu extends InventoryUI {
    private static final String ORB_SKIN = Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzdlOGNiNTdmZTc5MGU5NjVlM2NmYTZjNGZiYzE2ZTMyMjYyMTBkNjVmNTYxNGU4ODUzZmE5ZmI4NDA3NDQ0MSJ9fX0=");

    public PowerupsMenu(GamePlayer player) {
        super("   Powerups and Geodes Menu", 54);

        // Available powerup parts
        ItemUI availableParts = new ItemUI(Material.FILLED_MAP);
        availableParts.setUpdateConsumer(itemUI -> {
            double totalUsedParts = PowerupService.i().getTotalParts(player);

            itemUI.setTitle(ChatColor.GOLD + "Available Powerup parts: " + (player.getPowerupsProgress().size() > 0 ? ChatColor.GREEN : ChatColor.RED) + Formatter.format(player.getPowerupParts()));
            if (totalUsedParts != player.getPowerupParts()) {
                itemUI.addLore(ChatColor.GRAY + "Used Powerup parts: " + ChatColor.GOLD + Formatter.format(totalUsedParts));
                itemUI.setLore(" ");
                itemUI.addLore(ChatColor.RED + "Click to reset all Powerup parts");
            } else {
                itemUI.setLore(" ");
                itemUI.addLore(ChatColor.GRAY + "Use these parts to upgrade Powerups");
            }
        });
        availableParts.setClickConsumer(inventoryClickPack -> {
            double totalParts = PowerupService.i().getTotalParts(player);
            if (totalParts != player.getPowerupParts()) {
                MenuUtil.openConfirmationMenu(player, aBoolean -> {
                    if (aBoolean) {
                        PowerupService.i().resetParts(player);
                    }
                    PowerupService.i().openMenu(player);
                }, ChatColor.RED + "Are you sure you want to", ChatColor.RED + "reset all " + Formatter.format(totalParts) + " powerup parts?", "You will not get any schmepls back!");

                player.clickSound();
            }
        });
        setItem(13, availableParts);

        // Pickaxe powerup
        if (PowerupService.i().isUnlocked(player, PowerupCategory.PICKAXE)) {
            buildPowerupItem(player, PowerupCategory.PICKAXE, new ItemUI(Material.WOODEN_PICKAXE), 20, "Charge this powerup by mining blocks.\nOnce fully charged, right-click\nto get a gold boost", () -> (int) (1 / player.getPickaxePowerup().getChargeRate(PowerupCategory.PICKAXE, false)) + " clicks");
            buildEffectItem(player, PowerupCategory.PICKAXE, 29);
        } else {
            ItemUI locked = MenuUtil.setLockedSkull(new ItemUI(Material.PLAYER_HEAD), "Locked - find this Powerup in a geode");
            setItem(20, locked);
        }

        // Orb powerup
        if (PowerupService.i().isUnlocked(player, PowerupCategory.ORB)) {
            buildPowerupItem(player, PowerupCategory.ORB, new ItemUI(ORB_SKIN), 24, "Powerful orbs help you gain more gold\nas you mine the blocks", () -> (int) (1 / player.getPickaxePowerup().getChargeRate(PowerupCategory.ORB, false)) + " seconds when clicking");
            buildEffectItem(player, PowerupCategory.ORB, 33);
        } else {
            ItemUI locked = MenuUtil.setLockedSkull(new ItemUI(Material.PLAYER_HEAD), "Locked - find this Powerup in a geode");
            setItem(24, locked);
        }

        // Geodes
        int position = 47;
        for (Rarity rarity : Rarity.values()) {
            if (rarity.equals(Rarity.SPECIAL)) continue;

            int count = GeodesService.i().getCount(player, rarity);
            ItemUI itemUI = new ItemUI(count > 0 ? rarity.getSkin() : MenuUtil.LOCKED_SKIN);
            itemUI.setTitle(rarity.getGeodeName() + ChatColor.YELLOW + " geode");
            itemUI.addLore(" ");
            itemUI.addLore(ChatColor.GRAY + "Count: " + ChatColor.GREEN + count);
            itemUI.addLore(ChatColor.GRAY + "Rarity: " + rarity.getName());
            itemUI.addLore(" ");
            if (count > 0) {
                itemUI.addLore(ChatColor.GRAY + "Can be opened in the lobby");
            } else {
                itemUI.addLore(ChatColor.GRAY + "Get geodes from ascending");
                itemUI.addLore(ChatColor.GRAY + "or at " + ChatColor.DARK_AQUA + "store.mineclick.net");
            }

            setItem(position, itemUI);
            position++;
        }

        // close menu
        setItem(0, MenuUtil.getCloseMenu(p -> p.getMainMenu().open(p.getPlayer())));
        open(player.getPlayer());
    }

    private void buildPowerupItem(GamePlayer player, PowerupCategory category, ItemUI item, int position, String description, Supplier<String> rechargeInfo) {
        item.setUpdateConsumer(itemUI -> {
            PowerupProgress progress = PowerupService.i().getProgress(player, category);
            boolean unlocked = progress.getLevel() > 0;

            itemUI.setTitle(ChatColor.GOLD + StringUtils.capitalize(category.name().toLowerCase()) + " Powerup" + (unlocked ? ChatColor.GREEN + " level " + progress.getLevel() : ChatColor.RED + " LOCKED"));
            itemUI.setLore(" ");
            for (String info : description.split("\n")) {
                itemUI.addLore(ChatColor.GRAY + info);
            }
            itemUI.addLore(" ");

            int parts = progress.getPartsToNextLevel();
            BigNumber nextReward = PowerupService.i().getGoldReward(player, category, true);
            if (!unlocked) {
                itemUI.addLore(ChatColor.GRAY + "Power: " + ChatColor.YELLOW + Formatter.format(progress.getNextLevelPower()) + " clicks " + ChatColor.GRAY + "(" + nextReward.print(player) + ChatColor.GRAY + " gold)");
                itemUI.addLore(ChatColor.GRAY + "Recharge: " + ChatColor.YELLOW + rechargeInfo.get());
                itemUI.addLore(" ");
                itemUI.addLore(ChatColor.GRAY + "Unlock cost:");
                itemUI.addLore(MenuUtil.prerequisite(parts <= player.getPowerupParts(), Formatter.format(parts) + " powerup parts", " "));
                int schmeplsCost = progress.getSchmeplsCost();
                itemUI.addLore(MenuUtil.prerequisite(schmeplsCost <= player.getSchmepls(), Formatter.format(schmeplsCost) + " schmepls", " "));
                if (parts <= player.getPowerupParts() && schmeplsCost <= player.getSchmepls()) {
                    itemUI.addLore(" ");
                    itemUI.addLore(ChatColor.YELLOW + "Click to unlock");
                }
            } else {
                BigNumber reward = PowerupService.i().getGoldReward(player, category, false);

                itemUI.addLore(ChatColor.GRAY + "Power: " + ChatColor.YELLOW + Formatter.format(progress.getPower()) + " clicks " + ChatColor.GRAY + "(" + reward.print(player) + ChatColor.GRAY + " gold)");
                itemUI.addLore(ChatColor.GRAY + "Recharge: " + ChatColor.YELLOW + rechargeInfo.get());
                itemUI.addLore(" ");
                itemUI.addLore(ChatColor.GRAY + "Next level: " + Formatter.format(progress.getNextLevelPower()) + " clicks (" + nextReward.print(player, false, true, ChatColor.GRAY, ChatColor.GRAY, ChatColor.GRAY, false) + " gold)");

                itemUI.addLore(ChatColor.GRAY + "Upgrade cost:");
                itemUI.addLore(MenuUtil.prerequisite(parts <= player.getPowerupParts(), Formatter.format(parts) + " powerup parts", " "));
                int schmeplsCost = progress.getSchmeplsCost();
                itemUI.addLore(MenuUtil.prerequisite(schmeplsCost <= player.getSchmepls(), Formatter.format(schmeplsCost) + " schmepls", " "));
                if (parts <= player.getPowerupParts()) {
                    itemUI.addLore(" ");
                    itemUI.addLore(ChatColor.YELLOW + "Click to purchase the upgrade");
                }
            }
        });
        item.setClickConsumer(inventoryClickPack -> {
            if (PowerupService.i().unlockNextLevel(player, category)) {
                player.popSound();
            } else {
                player.noSound();
            }
        });
        setItem(position, item);
    }

    private void buildEffectItem(GamePlayer player, PowerupCategory category, int position) {
        ItemUI effectItem = new ItemUI(Material.CHEST);
        effectItem.setUpdateConsumer(itemUI -> {
            itemUI.setTitle(ChatColor.GOLD + StringUtils.capitalize(category.name().toLowerCase()) + " Powerup Effect");

            PowerupType selectedPowerup = PowerupService.i().getSelectedPowerup(player, category);
            itemUI.setLore(ChatColor.GRAY + "Selected: " + (selectedPowerup == null ? "Random" : selectedPowerup.getRarity().getColor() + selectedPowerup.getName()));

            Pair<Long, Long> unlockedCount = PowerupService.i().getUnlockedCount(player, category);
            itemUI.addLore(ChatColor.GRAY + "Unlocked: " + (unlockedCount.key().equals(unlockedCount.value()) ? ChatColor.GREEN : ChatColor.YELLOW) + unlockedCount.key() + "/" + unlockedCount.value());
            itemUI.addLore(" ");
            itemUI.addLore(ChatColor.YELLOW + "Click to select a different effect");
        });
        effectItem.setClickConsumer(inventoryClickPack -> {
            PowerupService.i().openEffectMenu(player, category);
            player.clickSound();
        });
        setItem(position, effectItem);
    }
}
