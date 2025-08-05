package net.mineclick.game.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.ResponsiveScoreboard;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class ParkourData {
    private Set<Integer> completedIslands = new HashSet<>();
    private Set<Integer> completedDimensions = new HashSet<>(); // never resets

    private boolean started;
    private long startedOn;
    private Map<String, Long> highScores = new HashMap<>();
    private int checkpointsUsed;
    private Location checkpoint;
    private int checkpoints = 2;
    private boolean shoesUnlocked;
    private boolean elytraUnlocked;

    private transient Location lastLocation;
    private transient boolean shoesRemoved;
    private transient boolean elytraRemoved;
    private transient GamePlayer player;

    public long getHighScore() {
        int islandId = player.getCurrentIsland().getId();
        int dimensionId = player.getDimensionsData().getCurrentDimensionId();

        return highScores.getOrDefault(dimensionId + ":" + islandId, 0L);
    }

    public void setHighScore(GamePlayer player, long score) {
        int islandId = player.getCurrentIsland().getId();
        int dimensionId = player.getDimensionsData().getCurrentDimensionId();

        highScores.put(dimensionId + ":" + islandId, score);
    }

    public void reset() {
        started = false;
        startedOn = 0;
        checkpoint = null;
        checkpointsUsed = 0;
        lastLocation = null;
        resetScoreboard();
    }

    /// Removes lines that are set during tick
    /// @see #tick()
    public void resetScoreboard() {
        // Remove these scores first since we don't know the current state
        // and what values gets updated later on
        if (player.getScoreboard() == null) return;
        player.getScoreboard().removeScore(11);
        player.getScoreboard().removeScore(12);
        player.getScoreboard().removeScore(13);
        player.updateScoreboard();
    }

    public void tick() {
        ResponsiveScoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) return;
        if (!started) return;

        long score = System.currentTimeMillis() - startedOn;
        long highScore = getHighScore();
        ChatColor color = highScore == 0 || highScore > score ? ChatColor.YELLOW : ChatColor.RED;
        scoreboard.setScore(11, ChatColor.GRAY + "Parkour Time");
        scoreboard.setScore(12, color + " " + Formatter.durationWithMilli(score));
        scoreboard.setScore(13, "     ");
    }
}
