package net.mineclick.game.model;

import lombok.Data;
import org.bukkit.Location;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Data
public class PlayerActivityData {
    private static final int SAMPLE_SIZE_SECONDS = 20;
    private static final double AUTO_CLICKER_THRESHOLD = 0.92; // auto-clicking if the CPD is over 0.92

    private Instant lastOnlineAt = Instant.now();
    private Instant lastCreatedReport;
    private long afkTime = 0; // in seconds
    private long playTime = 0; // in seconds
    private boolean afk;
    private Location lastLocation;
    private long sameLocationSeconds = 0;
    private boolean everClicked; // if the player has ever clicked in this dimension
    private int autoClickerKicks = 0; // number of times the player was kicked by auto-clicker detection

    private transient boolean[] clicksSample = new boolean[SAMPLE_SIZE_SECONDS * 20];
    private transient int clicksSampleIndex = 0;
    private transient boolean clickedThisTick;

    /**
     * Call this method on every tick
     *
     * @param player The player
     * @param ticks  Ticks count
     */
    public void tick(GamePlayer player, long ticks) {
        if (ticks % 20 == 0) {
            Location currentLocation = player.getPlayer().getLocation();
            if (lastLocation != null
                    && lastLocation.getBlockX() == currentLocation.getBlockX()
                    && lastLocation.getBlockZ() == currentLocation.getBlockZ()) {
                if (sameLocationSeconds++ >= 300) { // 5 minutes
                    if (!afk) {
                        player.getPlayer().closeInventory();
                    }

                    afk = true;
                }
            } else {
                lastLocation = currentLocation;
                sameLocationSeconds = 0;
                afk = false;
                afkTime = 0;
            }

            if (afk) {
                afkTime++;
            } else {
                playTime++;
            }
        }


        if (clicksSampleIndex >= SAMPLE_SIZE_SECONDS * 20) {
            clicksSampleIndex = 0;
        }
        System.arraycopy(clicksSample, 0, clicksSample, 1, clicksSample.length - 1);
        clicksSample[0] = clickedThisTick;
        clickedThisTick = false;
    }

    /**
     * @param seconds The number of max seconds the player can be stationary for
     * @return True if the player has moved in the last X seconds
     */
    public boolean wasMoving(int seconds) {
        return sameLocationSeconds < seconds;
    }

    /**
     * Call this method to record a player click
     */
    public void click() {
        clickedThisTick = true;
    }

    /**
     * @return Average clicks per second
     */
    public int calculateAvgCPS() {
        int count = 0;
        for (int i = 0; i < 20; i++) {
            boolean sample = clicksSample[i];
            count += sample ? 1 : 0;
        }

        return count;
    }

    /**
     * Calculates clicks period dominance.
     * High CPD indicates a dominant period frequency
     * which is likely to be an auto-clicker
     *
     * @return Clicks period dominance (0.0 to 1.0)
     */
    public double calculateCPD() {
        Map<Integer, Integer> map = new HashMap<>();
        int period = 0;
        for (boolean clicked : clicksSample) {
            period++;
            if (clicked) {
                map.compute(period, (p, count) -> count == null ? 1 : count + 1);
                period = 0;
            }
        }

        if (map.size() <= 1) return 0;
        double sum = map.values().stream().mapToInt(i -> i).sum();
        if (sum <= SAMPLE_SIZE_SECONDS * 2) return 0;

        int mostFrequent = map.values().stream()
                .max(Comparator.comparingInt(i -> i))
                .orElse(0);

        return mostFrequent / sum;
    }

    /**
     * Calculate CPD and determine if the player is auto-clicking
     *
     * @return True if the player is auto-clicking
     */
    public boolean isAutoClicking() {
        return calculateCPD() >= AUTO_CLICKER_THRESHOLD;
    }
}
