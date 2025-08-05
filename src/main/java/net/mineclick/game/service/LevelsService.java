package net.mineclick.game.service;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.util.SingletonInit;

@SingletonInit
public class LevelsService {
    private static LevelsService i;

    private LevelsService() {
    }

    public static LevelsService i() {
        return i == null ? i = new LevelsService() : i;
    }

    /**
     * Get the level for a given exp amount
     *
     * @param exp The exp amount
     * @return The level for this exp amount
     */
    public int getLevel(long exp) {
        if (exp <= 0) {
            return 0;
        }

        int level = 0;

        // Use binary search to find the level
        int low = 0;
        // this is 1,339,566,080 of xp
        // if you need 2 minutes to ascend
        // you need around 8 years of constant ascending
        int high = 512;

        // this while will not be run more than 9 times a row
        while (low <= high) {
            int mid = (low + high) / 2;
            long expForMid = getLevelExp(mid);
            long expForNext = getLevelExp(mid + 1);

            if (exp >= expForMid && exp < expForNext) {
                level = mid;
                break;
            } else if (exp < expForMid) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return level;
    }

    /**
     * Get exp needed for a given level
     *
     * @param level The level
     * @return The exp amount
     */
    public long getLevelExp(int level) {
        if (level <= 0) {
            return 0;
        }

        // Formula: 10x³ - 10x² + 20x
        return 10 * (long)Math.pow(level, 3) -
                10 * (long)Math.pow(level, 2) +
                20L * level;
    }

    /**
     * Get the delta needed to achieve the next level
     *
     * @param level The current level
     * @return The exp delta
     */
    public long getLevelDelta(int level) {
        return getLevelExp(level + 1) - getLevelExp(level);
    }

    public void updateExpBar(GamePlayer player) {
        if (player.isOffline())
            return;

        int level = getLevel(player.getExp());
        float percent = (player.getExp() - getLevelExp(level)) / (float) getLevelDelta(level);
        player.getPlayer().setExp(percent);
        player.getPlayer().setLevel(level);
    }
}
