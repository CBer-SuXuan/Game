package net.mineclick.game.service;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.achievement.Achievement;
import net.mineclick.game.model.achievement.AchievementNode;
import net.mineclick.game.model.achievement.AchievementProgress;
import net.mineclick.game.type.StatisticType;
import net.mineclick.global.service.ConfigurationsService;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@SingletonInit
public class AchievementsService {
    private static AchievementsService i;

    private Map<String, AchievementNode> achievements = new LinkedHashMap<>();

    private AchievementsService() {
        ConfigurationsService.i().onUpdate("achievements", this::update);
    }

    public static AchievementsService i() {
        return i == null ? i = new AchievementsService() : i;
    }

    private void update() {
        if (!ConfigurationsService.i().contains("achievements")) {
            Game.i().getLogger().warning("Could not load Achievements");
            return;
        }

        achievements = new LinkedHashMap<>();
        ConfigurationSection section = ConfigurationsService.i().get("achievements");
        for (String key : section.getKeys(false)) {
            AchievementNode node = new AchievementNode(key);

            int count = 1;
            for (Map<?, ?> level : section.getMapList(key)) {
                String name = null;
                try {
                    name = (String) level.get("name");
                    Achievement achievement = new Achievement(name);
                    achievement.setLevel(count++);
                    achievement.setDescription((String) level.get("description"));
                    achievement.setScore(Double.parseDouble(level.get("score") + ""));
                    achievement.setSchmepls(Long.parseLong(level.get("schmepls") + ""));
                    achievement.setExp(Long.parseLong(level.get("exp") + ""));

                    node.getAchievements().add(achievement);
                } catch (Exception e) {
                    Game.i().getLogger().log(Level.SEVERE, "Could not parse achievement " + name, e);
                }
            }
            achievements.put(key, node);
        }

        Game.i().getLogger().info("Loaded " + achievements.size() + " Achievement nodes");
    }

    public List<AchievementNode> getAchievements() {
        return new ArrayList<>(achievements.values());
    }

    /**
     * Increment an achievement progress by a given amount
     *
     * @param player        The player
     * @param achievementId Id of the achievement (node)
     * @param amount        The amount to increment by
     */
    public void incrementProgress(GamePlayer player, String achievementId, long amount) {
        setProgress(player, achievementId, amount, true);
    }

    /**
     * Set an achievement progress with the given amount.
     * Will not be set to less than what the current progress is
     *
     * @param player        The player
     * @param achievementId Id of the achievement (node)
     * @param amount        The amount to set the progress to
     */
    public void setProgress(GamePlayer player, String achievementId, long amount) {
        setProgress(player, achievementId, amount, false);
    }

    private void setProgress(GamePlayer player, String achievementId, long amount, boolean increment) {
        AchievementProgress progress = getProgressOrCreate(player, achievementId);

        if (progress != null && progress.getStatisticType() == null) {
            long oldProgress = progress.getProgress();
            progress.setProgress(increment ? oldProgress + amount : Math.max(oldProgress, amount));
        }
    }

    /**
     * Get the player's progress for a given achievement
     *
     * @param player        The player
     * @param achievementId Id of the achievement
     * @return The progress or 0 is no progress was recorded (or missing)
     */
    public long getProgress(GamePlayer player, String achievementId) {
        AchievementProgress progress = getProgressOrCreate(player, achievementId);
        if (progress == null) {
            return 0;
        }

        if (progress.getStatisticType() != null) {
            return (long) StatisticsService.i().get(player.getUuid(), progress.getStatisticType()).getScore();
        }

        return progress.getProgress();
    }

    private AchievementProgress getProgressOrCreate(GamePlayer player, String achievementId) {
        return player.getAchievements().computeIfAbsent(achievementId, id -> {
            AchievementNode node = achievements.get(achievementId);
            if (node == null) {
                Game.i().getLogger().severe("Cannot increment achievement id " + achievementId + ". No such achievement configuration");
                return null;
            }

            AchievementProgress newProgress = new AchievementProgress();
            StatisticType statisticType = StatisticType.getByKey(id);
            if (statisticType != null) {
                newProgress.setStatisticType(statisticType);
            }

            return newProgress;
        });
    }

    /**
     * Get the lowest level completed (not awarded yet) achievement
     * from the achievement node
     *
     * @param player          The player
     * @param achievementNode The achievement node
     * @return The lowest level achievement that has been fulfilled by the progression
     * or null if no such exist for this progression
     */
    public Achievement getToBeAwardedAchievement(GamePlayer player, AchievementNode achievementNode) {
        AchievementProgress progress = player.getAchievements().get(achievementNode.getId());
        if (progress == null) {
            return null;
        }

        // If already awarded the highest level no need to check further
        if (progress.getAwardedLevel() == achievementNode.getAchievements().size()) {
            return null;
        }

        return achievementNode.getByProgress(getProgress(player, achievementNode.getId()), progress.getAwardedLevel());
    }

    /**
     * Reward this achievement node (its lowest, eligible and yet to be awarded level)
     *
     * @param player          The player
     * @param achievementNode The achievement node
     * @return True if an achievement was awarded
     */
    public boolean awardAchievement(GamePlayer player, AchievementNode achievementNode) {
        Achievement achievement = getToBeAwardedAchievement(player, achievementNode);
        AchievementProgress progress = player.getAchievements().get(achievementNode.getId());
        if (achievement == null || progress == null) {
            return false;
        }

        progress.setAwardedLevel(achievement.getLevel());
        player.addExp(achievement.getExp());
        player.addSchmepls(achievement.getSchmepls());
        player.playSound(Sound.ENTITY_PLAYER_LEVELUP, 1, 2);

        // check the "achievements" achievement
        int sum = player.getAchievements().values().stream().mapToInt(AchievementProgress::getAwardedLevel).sum();
        setProgress(player, "achievements", sum);

        return true;
    }

    /**
     * Get the next level achievement for this player and achievement node (or the highest)
     *
     * @param player          The player
     * @param achievementNode The achievement node
     * @return The next level achievement or the highest
     */
    public Achievement getNextLevel(GamePlayer player, AchievementNode achievementNode) {
        AchievementProgress progress = player.getAchievements().get(achievementNode.getId());
        if (progress == null) return achievementNode.getByLevel(1);

        return achievementNode.getByLevel(progress.getAwardedLevel() + 1);
    }

    /**
     * @param player The player
     * @return True if has any uncollected achievements
     */
    public boolean hasUncollected(GamePlayer player) {
        return achievements.values().stream()
                .anyMatch(node -> getToBeAwardedAchievement(player, node) != null);
    }

    /**
     * Check for uncollected achievements
     * and notify the player if any exist
     *
     * @param player The player
     */
    public void checkAchievements(GamePlayer player) {
        boolean announce = false;

        for (AchievementNode node : achievements.values()) {
            AchievementProgress progress = player.getAchievements().get(node.getId());
            if (progress != null && !progress.isChecked() && getToBeAwardedAchievement(player, node) != null) {
                progress.setChecked(true);
                announce = true;
            }
        }

        if (announce) {
            player.levelUpSound();
            player.sendImportantMessage("Achievement Unlocked!", "Collect your achievements in the Main Menu");
        }
    }
}
