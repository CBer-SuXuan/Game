package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.LevelsService;
import net.mineclick.game.service.SkillsService;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

import java.util.function.Supplier;

public class SkillsMenu extends InventoryUI {
    private final GamePlayer player;
    private int scroll = 0;

    public SkillsMenu(GamePlayer player) {
        super("       MineClick Skills Menu", 54);
        this.player = player;

        for (int i = 1; i < 53; i++) {
            if (i == 8) continue;

            ItemUI itemUI = new ItemUI(Material.AIR);
            setItem(i, itemUI);
        }

        // arrow up
        ItemUI up = new ItemUI(Material.AIR);
        up.setUpdateConsumer(itemUI -> {
            if (scroll == 0) {
                itemUI.setMaterial(Material.AIR);
            } else {
                itemUI.setTitle(ChatColor.GOLD + "Scroll up");
                itemUI.setMaterial(Material.PLAYER_HEAD);
                itemUI.setSkin(MenuUtil.ARROW_UP);
            }
        });
        up.setClickConsumer(inventoryClickPack -> {
            if (scroll > 0) {
                scroll--;

                player.clickSound();
                redraw();
            }
        });
        setItem(8, up);

        // arrow down
        ItemUI down = new ItemUI(Material.AIR);
        down.setUpdateConsumer(itemUI -> {
            if (scroll >= SkillType.Category.values().length - 4) {
                itemUI.setMaterial(Material.AIR);
            } else {
                itemUI.setTitle(ChatColor.GOLD + "Scroll down");
                itemUI.setMaterial(Material.PLAYER_HEAD);
                itemUI.setSkin(MenuUtil.ARROW_DOWN);
            }
        });
        down.setClickConsumer(inventoryClickPack -> {
            if (scroll < SkillType.Category.values().length - 4) {
                scroll++;

                player.clickSound();
                redraw();
            }
        });
        setItem(53, down);

        setItem(0, MenuUtil.getCloseMenu(p -> p.getMainMenu().open(p.getPlayer())));
        setDestroyOnClose(false);
        addAllowedClickType(ClickType.RIGHT);

        redraw();
    }

    private void redraw() {
        for (int i = 1; i < 53; i++) {
            if (i == 8) continue;

            ItemUI itemUI = getItem(i);
            itemUI.setUpdateConsumer(item -> item.setMaterial(Material.AIR));
            itemUI.setClickConsumer(null);
        }

        int row = scroll == 0 ? 1 : 0;
        SkillType.Category[] categories = SkillType.Category.values();
        for (int i = Math.max(0, scroll - 1), length = categories.length; i < length && row < 6; i++) {
            SkillType.Category category = categories[i];
            int pos = (row * 9) + 1;

            ItemUI categoryItem = getItem(pos);
            categoryItem.setMaterial(category.getMaterial());
            categoryItem.setTitle(ChatColor.GOLD + category.getName());
            categoryItem.setUpdateConsumer(itemUI -> {
                int unlockedSkills = SkillsService.i().getUnlocked(player, category);
                int totalSkills = category.getCount();

                itemUI.setLore();
                itemUI.addLore(ChatColor.DARK_AQUA.toString() + unlockedSkills + "/" + totalSkills + ChatColor.GRAY + " skills unlocked");

                if (unlockedSkills == totalSkills)
                    itemUI.setGlowing();
            });

            for (SkillType skillType : SkillType.values()) {
                if (!skillType.getCategory().equals(category)) continue;
                pos++;

                Supplier<Boolean> canUnlock = () -> SkillsService.i().canUnlock(player, skillType);
                ItemUI item = getItem(pos);
                Material pane = (scroll % 2 == 0 ? row : row + 1) % 2 == 0 ? Material.BLUE_STAINED_GLASS_PANE : Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                Material paneLocked = (scroll % 2 == 0 ? row : row + 1) % 2 == 0 ? Material.BLACK_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                item.setUpdateConsumer(itemUI -> {
                    boolean unlocked = SkillsService.i().has(player, skillType);
                    boolean unlockable = canUnlock.get();

                    itemUI.setMaterial(unlocked ? pane : unlockable ? Material.LIME_STAINED_GLASS_PANE : paneLocked);
                    itemUI.setTitle((unlocked ? ChatColor.GOLD : unlockable ? ChatColor.GREEN : ChatColor.RED) + skillType.getCategory().getName() + " " + skillType.getNumber());

                    item.setLore();
                    for (String line : skillType.getDescription().get().split("\n")) {
                        item.addLore(ChatColor.GRAY + line);
                    }

                    item.addLore(" ");
                    if (unlocked) {
                        item.addLore(ChatColor.GOLD + "Unlocked");

                        if (skillType.getOnRightClick() != null) {
                            item.addLore(ChatColor.GREEN + "Right-click to configure");
                        }
                    } else {
                        item.addLore(MenuUtil.prerequisite(player.getSchmepls() >= skillType.getCost(), Formatter.format(skillType.getCost()) + ChatColor.DARK_AQUA + " schmepls", ChatColor.GOLD + "Cost:  "));

                        int level = LevelsService.i().getLevel(player.getExp());
                        item.addLore(MenuUtil.prerequisite(level >= skillType.getMinLevel(), level + "/" + skillType.getMinLevel(), ChatColor.GOLD + "Level: "));

                        SkillType previous = skillType.getPrevious();
                        if (previous != null && !SkillsService.i().has(player, previous)) {
                            item.addLore(" ");
                            item.addLore(ChatColor.RED + "Requires " + category.getName() + " " + previous.getNumber());
                        }

                        if (unlockable) {
                            item.addLore(" ");
                            item.addLore(ChatColor.GOLD + "Click to buy for " + ChatColor.DARK_AQUA + Formatter.format(skillType.getCost()) + " schmepls");
                        }
                    }
                });
                item.setClickConsumer(inventoryClickPack -> {
                    if (inventoryClickPack.event().isRightClick()) {
                        if (skillType.getOnRightClick() != null && SkillsService.i().has(player, skillType)) {
                            skillType.getOnRightClick().accept(player);
                        }

                        return;
                    }

                    if (player.isRankAtLeast(Rank.DEV) && SkillsService.i().has(player, skillType)) {
                        player.addSchmepls(skillType.getCost());
                        player.getSkills().remove(skillType);

                        player.recalculateGoldRate();
                        player.clickSound();
                        return;
                    }

                    if (canUnlock.get() && player.chargeSchmepls(skillType.getCost())) {
                        player.levelUpSound();

                        SkillsService.i().unlock(player, skillType);
                    } else {
                        player.noSound();
                    }
                });
            }

            row++;
        }
    }

    public void open() {
        open(player.getPlayer());
    }
}
