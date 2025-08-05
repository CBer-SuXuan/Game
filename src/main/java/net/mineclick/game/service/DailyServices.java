package net.mineclick.game.service;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.ChatColor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SingletonInit
public class DailyServices {
    private static DailyServices i;

    public static DailyServices i() {
        return i == null ? i = new DailyServices() : i;
    }

    public long getBestDailyStreak(GamePlayer player) {
        return AchievementsService.i().getProgress(player, "dailyStreak");
    }

    public void updateDailyStreak(GamePlayer player) {
        // reset daily streak if missed a day even with the skill unlocked... yikes
        if (SkillsService.i().has(player, SkillType.MISC_3) && Instant.now().isAfter(player.getLastDailyStreakLogin().plus(3, ChronoUnit.DAYS))) {
            player.setDailyStreak(0);
            player.sendMessage(ChatColor.RED + "You missed two days! Your daily streak has been reset to 0!");
        }
        // uh oh, joined after missing a day
        else if (Instant.now().isAfter(player.getLastDailyStreakLogin().plus(2, ChronoUnit.DAYS))) {
            // pretend this day never happened if skill is unlocked
            if (SkillsService.i().has(player, SkillType.MISC_3)) {
                player.sendMessage(ChatColor.RED + "You missed a day! Your daily streak wasn't increased!");
            }
            // reset daily streak if skill issue
            else {
                player.setDailyStreak(0);
                player.sendMessage(ChatColor.RED + "You missed a day! Your daily streak has been reset to 0!");
            }
        }
        // increase daily streak if didn't skip next day
        else if (Instant.now().isAfter(player.getLastDailyStreakLogin().plus(1, ChronoUnit.DAYS))) {
            int newStreak = player.getDailyStreak() + 1;

            player.setDailyStreak(newStreak);
            player.sendMessage(
                    ChatColor.YELLOW + "Increased your daily streak from "
                            + ChatColor.AQUA + (newStreak - 1)
                            + ChatColor.YELLOW + " to "
                            + ChatColor.AQUA + newStreak
                            + ChatColor.YELLOW + "!");

            long highDailyStreak = getBestDailyStreak(player);
            if (newStreak > highDailyStreak) {
                AchievementsService.i().setProgress(player, "dailyStreak", newStreak);
            }
        }

        player.setLastDailyStreakLogin(Instant.now().truncatedTo(ChronoUnit.DAYS));
    }

    public double getDailyChestRewardMultiplier(GamePlayer player) {
        int dailyStreakCount = Math.min(player.getDailyStreak(), 28);
        double mulitplier = 1;

        // 10% per every day after 7th day
        if (dailyStreakCount > 7) {
            mulitplier += 0.1 * (dailyStreakCount - 7);
            dailyStreakCount = 7;
        }

        // 2.5% per every day after everything else
        mulitplier += 0.025 * dailyStreakCount;

        return mulitplier;
    }
}
