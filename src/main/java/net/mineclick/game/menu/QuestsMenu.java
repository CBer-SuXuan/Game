package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.service.LevelsService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.type.quest.Quest;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.game.type.quest.daily.DailyQuest;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

public class QuestsMenu extends InventoryUI {
    public QuestsMenu(GamePlayer player) {
        super("      MineClick Quests Menu", 54);

        // 500, 8000, 40500, 128000, 312500
        Supplier<Long> getMoreQuestsCost = () -> ((long) Math.pow(player.getUnlockedDailyQuests() - 2, 4)) * 500;

        // daily quests
        int pos = 10;
        if (LevelsService.i().getLevel(player.getExp()) < 3) {
            for (int i = 0; i < 7; i++) {
                ItemUI item = new ItemUI(ItemBuilder.builder().material(Material.GRAY_STAINED_GLASS_PANE).title(ChatColor.RED + "Daily quests").lore(" ").lore(ChatColor.GRAY + "Unlocked at level 5"));
                setItem(pos++, item);
            }
        } else {
            for (int i = 0; i < 7; i++) {
                ItemUI questItem = new ItemUI(ItemBuilder.builder().material(Material.AIR));
                int index = i;
                questItem.setUpdateConsumer(item -> {
                    if (index == player.getUnlockedDailyQuests()) {
                        long cost = getMoreQuestsCost.get();
                        boolean canAfford = player.getSchmepls() >= cost;

                        item.setMaterial(Material.PLAYER_HEAD);
                        item.setSkin(MenuUtil.LOCKED_SKIN);
                        item.setTitle((canAfford ? ChatColor.GREEN : ChatColor.RED) + "More daily quests");
                        item.setLore(" ");
                        item.addLore(MenuUtil.prerequisite(canAfford, Formatter.format(cost) + " schmepls", ""));
                        if (canAfford) {
                            item.addLore(" ");
                            item.addLore(ChatColor.YELLOW + "Click to unlock");
                        }

                        return;
                    } else if (index > player.getUnlockedDailyQuests()) {
                        return;
                    }

                    List<String> dailyQuests = player.getDailyQuests();
                    if (dailyQuests.size() <= index) {
                        return;
                    }
                    DailyQuest quest = (DailyQuest) QuestsService.i().getQuest(dailyQuests.get(index));
                    if (quest == null) {
                        return;
                    }

                    QuestProgress progress = quest.getQuestProgress(player);
                    if (progress == null) {
                        return;
                    }

                    item.setMaterial(progress.isComplete() ? Material.BOOK : Material.ENCHANTED_BOOK);
                    item.setTitle(ChatColor.GOLD + quest.getName(player));
                    item.setLore(" ");

                    DailyQuest.Stage stage = quest.getCurrentStage(player);
                    String task;
                    if (progress.isComplete()) {
                        task = ChatColor.DARK_GREEN + "[✔] " + ChatColor.GRAY + ChatColor.STRIKETHROUGH;
                    } else {
                        task = ChatColor.GREEN + "- " + ChatColor.YELLOW;
                    }
                    task += quest.getObjective(0).getName(player) + ChatColor.GOLD + " (" + progress.getTaskProgress() + "/" + stage.getValue() + ")";
                    item.addLore(task);

                    if (progress.isComplete()) {
                        item.addLore(" ");
                        item.addLore(ChatColor.GREEN + "Quest complete");

                        if (progress.getCompletedAt() != null) {
                            long now = System.currentTimeMillis();
                            long newQuestAt = progress.getCompletedAt().toEpochMilli() + Duration.of(23, ChronoUnit.HOURS).toMillis();
                            if (now < newQuestAt) {
                                item.addLore(ChatColor.DARK_GREEN + "New quest in: " + ChatColor.YELLOW + Formatter.duration(newQuestAt - now));
                            } else {
                                item.addLore(ChatColor.DARK_GREEN + "New quest in: " + ChatColor.YELLOW + "loading...");
                            }
                        }
                    }
                });
                questItem.setClickConsumer(inventoryClickPack -> {
                    if (index == player.getUnlockedDailyQuests()) {
                        long cost = getMoreQuestsCost.get();
                        if (player.chargeSchmepls(cost)) {
                            player.setUnlockedDailyQuests(player.getUnlockedDailyQuests() + 1);
                            player.expSound();
                        } else {
                            player.noSound();
                        }
                    }
                });

                setItem(pos++, questItem);
            }
        }

        // normal quests
        pos = 28;
        for (Quest quest : QuestsService.i().getQuests().values()) {
            if (quest instanceof DailyQuest) {
                continue;
            }

            ItemUI questItem = new ItemUI(ItemBuilder.builder().material(Material.AIR));
            questItem.setUpdateConsumer(item -> {
                QuestProgress progress = quest.getQuestProgress(player);
                boolean unlocked = progress != null;

                item.setMaterial(unlocked ? (progress.isComplete() ? Material.BOOK : Material.ENCHANTED_BOOK) : Material.GRAY_STAINED_GLASS_PANE);
                item.setTitle(unlocked ? ChatColor.GOLD + quest.getName(player) : ChatColor.DARK_GRAY + "???");
                item.setLore(" ");

                if (unlocked) {
                    if (!progress.isComplete()) {
                        for (int i = 0; i < progress.getObjective() + 1 && i < quest.getObjectives().size(); i++) {
                            QuestObjective objective = quest.getObjectives().get(i);
                            if (objective.hideAfterNextObjective() && progress.getObjective() > i) {
                                continue;
                            }

                            if (i != progress.getObjective()) {
                                item.addLore(ChatColor.DARK_GREEN + "[✔] " + ChatColor.GRAY + ChatColor.STRIKETHROUGH + objective.getName(player));
                            } else {
                                String task = ChatColor.GREEN + "[  ] " + ChatColor.YELLOW + objective.getName(player);
                                if (objective.getValue(player) > 1) {
                                    task += ChatColor.GOLD + " (" + progress.getTaskProgress() + "/" + objective.getValue(player) + ")";
                                }
                                item.addLore(task);
                            }
                        }
                    } else {
                        item.addLore(ChatColor.GREEN + "Quest complete");
                    }
                } else {
                    item.addLore(ChatColor.GRAY + "Yet to be discovered...");
                }
            });

            setItem(pos++, questItem);
            if ((pos + 1) % 9 == 0) {
                pos += 2;
            }
        }

        if (pos < 43) {
            for (; pos < 44; pos++) {
                ItemUI itemUI = new ItemUI(ItemBuilder.builder().material(Material.GRAY_STAINED_GLASS_PANE).title(ChatColor.DARK_GRAY + "???").lore(" ").lore(ChatColor.GRAY + "Coming in a future update!"));

                setItem(pos, itemUI);
                if ((pos + 2) % 9 == 0) {
                    pos += 2;
                }
            }
        }

        setItem(0, MenuUtil.getCloseMenu(p -> p.getMainMenu().open(p.getPlayer())));
        open(player.getPlayer());
    }
}
