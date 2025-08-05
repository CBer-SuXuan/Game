package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.achievement.Achievement;
import net.mineclick.game.model.achievement.AchievementNode;
import net.mineclick.game.model.achievement.AchievementProgress;
import net.mineclick.game.service.AchievementsService;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AchievementsMenu extends InventoryUI {
    public AchievementsMenu(GamePlayer player) {
        super("      MineClick Achievements", 54);

        int index = 9;
        for (AchievementNode node : AchievementsService.i().getAchievements()) {
            ItemUI itemUI = new ItemUI(ItemBuilder.builder().material(Material.GLASS_BOTTLE).title(ChatColor.GRAY + "Loading...").build().toItem(), clickPack -> {
                if (AchievementsService.i().awardAchievement(player, node)) {
                    player.expSound();
                    update();
                }
            });
            itemUI.setUpdateConsumer(item -> {
                AchievementProgress progress = player.getAchievements().get(node.getId());
                Achievement nextLevel = AchievementsService.i().getNextLevel(player, node);
                Achievement toBeAwarded = AchievementsService.i().getToBeAwardedAchievement(player, node);
                boolean complete = progress != null && progress.getAwardedLevel() >= node.getAchievements().size();
                boolean pending = toBeAwarded != null;

                item.setAmount(nextLevel.getLevel());
                item.setTitle((pending ? ChatColor.GOLD : (complete ? ChatColor.GREEN : ChatColor.RED)) + nextLevel.getName());
                if (pending) {
                    item.setLore(ChatColor.GREEN + "Click to claim reward!");
                    item.addLore(" ");
                    item.setMaterial(Material.EXPERIENCE_BOTTLE);
                } else if (complete) {
                    long score = AchievementsService.i().getProgress(player, node.getId());
                    item.setLore(ChatColor.DARK_GREEN + "Complete ");
                    item.addLore(" ");
                    item.setMaterial(Material.COBWEB);
                } else {
                    item.setLore(" ");
                    item.setMaterial(Material.GLASS_BOTTLE);
                }
                for (String s : nextLevel.getDescription().split(";")) {
                    item.addLore(ChatColor.GRAY + s);
                }

                if (!pending) {
                    long score = AchievementsService.i().getProgress(player, node.getId());
                    item.addLore(ChatColor.GRAY.toString() + ChatColor.ITALIC + "(" + score + "/" + (long) nextLevel.getScore() + ")");
                }

                if (!complete) {
                    item.addLore(" ");
                    item.addLore(ChatColor.GRAY + "Reward: ");
                    item.addLore(ChatColor.GREEN + " +" + ChatColor.AQUA + nextLevel.getSchmepls() + ChatColor.YELLOW + " schmepls");
                    item.addLore(ChatColor.GREEN + " +" + ChatColor.AQUA + nextLevel.getExp() + ChatColor.YELLOW + " EXP");
                }
            });
            setItem(index++, itemUI);

            // if ((index + 1) % 9 == 0) {
            //     index += 2;
            // }
        }

        setItem(0, MenuUtil.getCloseMenu(p -> p.getMainMenu().open(p.getPlayer())));
        setDestroyOnClose(false);
    }

    public void update() {
        setUpdatePeriod(1);
        schedule(2, () -> setUpdatePeriod(200));
    }

    @Override
    public void open(Player player) {
        schedule(2, this::update);
        super.open(player);
    }
}
