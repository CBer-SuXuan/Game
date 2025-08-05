package net.mineclick.game.service;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.type.quest.Quest;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.game.type.quest.daily.*;
import net.mineclick.game.type.quest.villager.*;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.ChatColor;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SingletonInit
public class QuestsService {
    private static QuestsService i;
    @Getter
    private final Map<String, Quest> quests = new LinkedHashMap<>();

    private QuestsService() {
        // daily quests
        addQuest(new DailyAscendQuest());
        addQuest(new DailyBatsQuest());
        addQuest(new DailyBuildingsQuest());
        addQuest(new DailyClicksQuest());
        addQuest(new DailyCookiesQuest());
        addQuest(new DailyCrimsonQuest());
        addQuest(new DailyParkourQuest());
        addQuest(new DailyPickaxeQuest());
        addQuest(new DailySpleefQuest());
        addQuest(new DailySuperBlockQuest());
        addQuest(new DailyTraderQuest());
        addQuest(new DailyWorkersQuest());
        addQuest(new DailySilverfishQuest());

        // normal quests
        addQuest(new CrimsonQuest());
        addQuest(new CollectorQuest());
        addQuest(new CampfireQuest());
        addQuest(new CookieThievesQuest());
        addQuest(new ParkourLobbyQuest());
        addQuest(new ElytraQuest());
        addQuest(new BookshelvesQuest());
    }

    public static QuestsService i() {
        return i == null ? i = new QuestsService() : i;
    }

    private void addQuest(Quest quest) {
        quests.put(quest.getId(), quest);

        if (quest instanceof VillagerQuest) {
            ((VillagerQuest) quest).spawnVillager();
        }
    }

    /**
     * Get a quest by its id
     *
     * @param id The quest id
     * @return The quest if found, null otherwise
     */
    @Nullable
    public Quest getQuest(String id) {
        return quests.get(id);
    }

    /**
     * @param player  The player
     * @param questId The quest
     * @return True if player has unlocked this quest
     */
    public boolean unlockedQuest(GamePlayer player, String questId) {
        Quest quest = getQuest(questId);
        return quest != null && quest.isUnlocked(player);
    }

    /**
     * @param player  The player
     * @param questId The quest id
     * @return True is player has completed this quest
     */
    public boolean completedQuest(GamePlayer player, String questId) {
        Quest quest = getQuest(questId);
        return quest != null && quest.isComplete(player);
    }

    /**
     * Load player's quest progress data
     *
     * @param player The player
     */
    public void loadPlayerQuests(GamePlayer player) {
        player.getQuests().forEach((key, questProgress) -> {
            questProgress.setPlayer(player);
            questProgress.setQuest(getQuest(key));
        });
    }

    /**
     * Handle villager click
     *
     * @param player The player that clicked
     * @param quest  The quest the villager belongs to
     */
    public void handleVillagerClick(GamePlayer player, VillagerQuest quest) {
        if (player.isOffline() || !quest.isVisible(player) || quest.isOnCooldown(player)) {
            return;
        }

        QuestProgress questProgress = quest.getQuestProgress(player);
        if (questProgress == null) {
            if (quest.hasPrerequisite(player)) {
                questProgress = new QuestProgress();
                questProgress.setQuest(quest);
                player.getQuests().put(quest.getId(), questProgress);
            } else {
                return;
            }
        }

        if (questProgress.isComplete()) {
            quest.onVillagerClickAfterComplete(player);
            return;
        }

        QuestObjective objective = quest.getObjectives().get(Math.min(questProgress.getObjective(), quest.getObjectives().size() - 1));
        objective.onVillagerClick(player, questProgress.isTalkedToVillager());
        questProgress.setTalkedToVillager(true);
    }

    /**
     * Periodically checks if any quest objectives are complete.
     * This does not check daily quests.
     *
     * @param player The player
     */
    public void checkQuestObjectives(GamePlayer player) {
        for (QuestProgress questProgress : player.getQuests().values()) {
            Quest quest = questProgress.getQuest();
            if (questProgress.isComplete() || quest == null || quest instanceof DailyQuest) {
                continue;
            }

            QuestObjective objective = quest.getObjectives().get(Math.min(questProgress.getObjective(), quest.getObjectives().size() - 1));
            boolean lastObjective = questProgress.getObjective() >= quest.getObjectives().size() - 1;

            if (questProgress.getTaskProgress() >= objective.getValue(player)) {
                if (!lastObjective) {
                    questProgress.setObjective(questProgress.getObjective() + 1);

                    String firstLine = null;
                    if (objective.sendCompleteObjective()) {
                        firstLine = ChatColor.STRIKETHROUGH.toString() + ChatColor.DARK_GREEN + "Complete: " + ChatColor.GRAY + ChatColor.STRIKETHROUGH + objective.getName(player);
                    }

                    quest.sendQuestUpdateMessage(player, firstLine, ChatColor.GREEN + "New objective: " + ChatColor.YELLOW + quest.getObjectives().get(questProgress.getObjective()).getName(player));
                    player.expSound();

                    questProgress.setTaskProgress(0);
                    questProgress.setTalkedToVillager(false);
                }

                if (lastObjective) {
                    awardQuestCompletion(player, quest);
                    questProgress.setComplete(true);
                }
            }
        }
    }

    /**
     * Checks and sets new daily quests for the given player
     *
     * @param player The player
     */
    public void checkDailyQuests(GamePlayer player) {
        if (LevelsService.i().getLevel(player.getExp()) < 3) {
            return;
        }

        List<String> dailyQuests = player.getDailyQuests();
        int max = player.getUnlockedDailyQuests();

        // not sure hen this can happen, but who knows
        while (dailyQuests.size() > max) {
            dailyQuests.remove(dailyQuests.size() - 1);
        }

        // add quests if unlocked more
        while (dailyQuests.size() < max) {
            DailyQuest quest = getNewDailyQuest(player);
            if (quest == null) {
                break;
            }
            dailyQuests.add(quest.getId());
        }

        // check for new quests
        for (int i = 0; i < max; i++) {
            DailyQuest quest = (DailyQuest) getQuest(dailyQuests.get(i));
            if (quest == null) {
                break; // we're just gonna ignore this one...
            }

            QuestProgress progress = quest.getQuestProgress(player);
            if (progress != null && progress.isComplete()
                    && progress.getCompletedAt() != null
                    && progress.getCompletedAt().plus(23, ChronoUnit.HOURS).isBefore(Instant.now())) {
                DailyQuest newQuest = getNewDailyQuest(player);
                if (newQuest == null) {
                    break;
                }

                dailyQuests.set(i, newQuest.getId());
                progress.setCompletedCount(progress.getCompletedCount() + 1);
                progress = newQuest.getQuestProgress(player);

                if (progress != null) {
                    progress.setObjective(newQuest.getRandomObjectiveIndex());
                    progress.setComplete(false);
                    progress.setCompletedAt(null);
                    progress.setTaskProgress(0);
                    progress.setTalkedToVillager(false);
                }
            }

            if (progress == null) {
                progress = new QuestProgress();
                progress.setPlayer(player);
                progress.setQuest(quest);
                progress.setObjective(quest.getRandomObjectiveIndex());

                player.getQuests().put(quest.getId(), progress);
            }

            if (!progress.isComplete()) {
                DailyQuest.Stage stage = quest.getCurrentStage(player);
                if (progress.getTaskProgress() >= stage.getValue()) {
                    progress.setComplete(true);
                    progress.setCompletedAt(Instant.now());

                    awardQuestCompletion(player, quest);
                }
            }
        }
    }

    private void awardQuestCompletion(GamePlayer player, Quest quest) {
        int schmeplsAmount = quest.getSchmeplsReward(player);
        int expAmount = quest.getExpReward(player);
        player.addSchmepls(schmeplsAmount);
        player.addExp(expAmount);
        String schmepls = ChatColor.GREEN + "+" + ChatColor.YELLOW + schmeplsAmount + ChatColor.AQUA + " schmepls";
        String exp = ChatColor.GREEN + "+" + ChatColor.YELLOW + expAmount + ChatColor.AQUA + " EXP";
        quest.sendQuestUpdateMessage(player, ChatColor.GREEN + "Quest complete!", schmepls + " and " + exp);
        player.levelUpSound();
    }

    private DailyQuest getNewDailyQuest(GamePlayer player) {
        List<DailyQuest> availableQuests = this.quests.values().stream()
                .filter(q -> q instanceof DailyQuest)
                .filter(q -> q.hasPrerequisite(player))
                .filter(q -> !player.getDailyQuests().contains(q.getId()))
                .map(q -> ((DailyQuest) q))
                .collect(Collectors.toList());
        Collections.shuffle(availableQuests);

        if (availableQuests.isEmpty()) {
            Game.i().getLogger().severe("Error: No new daily quests for " + player.getName() + " " + player.getUuid());
            return null;
        }
        return availableQuests.get(0);
    }

    /**
     * Increment player's progress for the given quest
     *
     * @param player      The player
     * @param questId     The quest id
     * @param objectiveId The objective id
     * @param amount      Amount to increment the progress by
     * @return True if successfully incremented, that is if the quest id is valid and progress is not complete/null
     */
    public boolean incrementProgress(GamePlayer player, String questId, int objectiveId, int amount) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            return false;
        }

        QuestProgress progress = quest.getQuestProgress(player);
        if (progress == null || progress.isComplete() || progress.getObjective() != objectiveId) {
            return false;
        }

        int max;
        if (quest instanceof DailyQuest) {
            max = ((DailyQuest) quest).getCurrentStage(player).getValue();
        } else {
            max = quest.getObjective(objectiveId).getValue(player);
        }

        progress.setTaskProgress(Math.min(progress.getTaskProgress() + amount, max));
        return true;
    }
}
