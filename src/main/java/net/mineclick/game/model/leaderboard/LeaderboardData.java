package net.mineclick.game.model.leaderboard;

import lombok.Data;
import net.mineclick.game.model.Statistic;

import java.util.Map;

@Data
public class LeaderboardData {
    private final String statistic;
    private final int count;
    private Map<String, Statistic> statistics;
}
