package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.game.Game;
import net.mineclick.game.service.*;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.location.RandomVector;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

@Data
public class DailyRewardChest {
    private static final int MAX_CLICKS = 15;
    private static final int EXP = 2;
    private static final int SCHMEPLS = 20;

    private Instant refreshAt = Instant.now();
    private int clicks = 0;

    public boolean isEmpty(GamePlayer player) {
        int max = MAX_CLICKS;

        if (SkillsService.i().has(player, SkillType.DAILY_CHEST_2)) {
            max = (int) (max * 1.5);
        }

        return clicks >= max;
    }

    public boolean isRefreshed() {
        return refreshAt.isBefore(Instant.now());
    }

    public void handleClick(GamePlayer player) {
        if (isEmpty(player)) {
            return;
        }

        clicks++;
        if (isEmpty(player)) {
            refreshAt = Instant.now().plus(23, ChronoUnit.HOURS);
        }

        // Spawn geodes if needed
        if (player.isRankAtLeast(Rank.PAID) && clicks <= 5) {
            Rarity rarity = GeodesService.i().addGeode(player);
            spawnHologram(1, rarity.getGeodeName() + " geode", player);
        }

        // MAX_CLICKS (15) * 50% is 22.5, meaning with 1/7 chance on average daily chests will give 3 geodes in total.
        if (SkillsService.i().has(player, SkillType.DAILY_CHEST_3) && Game.getRandom().nextInt(7) == 0) {
            Rarity rarity = GeodesService.i().addGeode(player);
            spawnHologram(1, rarity.getGeodeName() + " geode", player);
        }

        //Give rewards
        double multiplier = getRewardMultiplier(player);
        double levelMultiplier = getLevelMultiplier(player);

        int expAmount = (int) (EXP * multiplier * levelMultiplier);
        int schmeplsAmount = (int) (SCHMEPLS * multiplier * levelMultiplier);

        player.addExp(expAmount);
        player.addSchmepls(schmeplsAmount);

        spawnHologram(expAmount, "EXP", player);
        spawnHologram(schmeplsAmount, "schmepls", player);
    }

    private double getRewardMultiplier(GamePlayer player) {
        double multiplier = DailyServices.i().getDailyChestRewardMultiplier(player);

        if (SkillsService.i().has(player, SkillType.DAILY_CHEST_1))
            multiplier += 0.25;

        if (SkillsService.i().has(player, SkillType.DAILY_CHEST_4))
            multiplier *= 2;

        return multiplier;
    }

    private double getLevelMultiplier(GamePlayer player) {
        int level = LevelsService.i().getLevel(player.getExp());
        return 1 + (level - 1) * 0.05; // This will give a multiplier of 1 for level 1, 1.05 for level 2, and so on.
    }

    private void spawnHologram(int value, String text, GamePlayer player) {
        Location loc = LobbyService.i().getRewardsChest();
        HologramsService.i().spawnFloatingUp(loc.clone().add(new RandomVector()), p -> ChatColor.GREEN + "+" + ChatColor.YELLOW + value + ChatColor.AQUA + " " + text, Collections.singleton(player));
    }

    public String getTimeLeft() {
        long diff = refreshAt.toEpochMilli() - System.currentTimeMillis();

        if (diff > 0) {
            return Formatter.duration(diff);
        } else {
            return "00:00:00";
        }
    }
}
