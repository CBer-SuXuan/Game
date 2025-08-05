package net.mineclick.game.service;

import net.mineclick.core.messenger.Action;
import net.mineclick.game.messenger.LeaderboardsHandler;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.leaderboard.Leaderboard;
import net.mineclick.game.model.leaderboard.LeaderboardData;
import net.mineclick.game.model.leaderboard.PersonalStatsBoard;
import net.mineclick.game.type.LeaderboardType;
import net.mineclick.global.service.ConfigurationsService;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.SingletonInit;
import net.mineclick.global.util.location.LocationParser;
import net.mineclick.global.util.location.Region;
import net.minecraft.core.Direction;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SingletonInit
public class LeaderboardsService {
    private static LeaderboardsService i;

    private Set<Leaderboard> leaderboards = new HashSet<>();
    private PersonalStatsBoard personalStatsBoard;
    private Region region;

    private LeaderboardsService() {
        ConfigurationsService.i().onUpdate("lobby", this::load);

        // Query db every 30 seconds
        Runner.async(20, 600, state -> {
            if (!leaderboards.isEmpty()) {
                Set<LeaderboardData> set = leaderboards.stream()
                        .map(Leaderboard::getType)
                        .map(type -> new LeaderboardData(type.getStatisticType().getKey(), type.getSize().getLines()))
                        .collect(Collectors.toSet());

                LeaderboardsHandler handler = new LeaderboardsHandler();
                handler.setLeaderboards(set);
                handler.setResponseConsumer(message -> {
                    for (LeaderboardData data : ((LeaderboardsHandler) message).getLeaderboards()) {
                        leaderboards.stream()
                                .filter(leaderboard -> leaderboard.getType().getStatisticType().getKey().equals(data.getStatistic()))
                                .forEach(leaderboard -> leaderboard.setStatistics(data.getStatistics()));
                    }
                });

                handler.send(Action.GET);
            }
        });
    }

    public static LeaderboardsService i() {
        return i == null ? i = new LeaderboardsService() : i;
    }

    private void load() {
        ConfigurationSection section = ConfigurationsService.i().get("lobby");
        if (section == null) {
            return;
        }

        leaderboards = new HashSet<>();
        for (Map<?, ?> map : section.getMapList("leaderboards")) {
            LeaderboardType type = LeaderboardType.valueOf(String.valueOf(map.get("type")));
            Location loc = LocationParser.parse(String.valueOf(map.get("loc")));
            Direction face = Direction.valueOf(String.valueOf(map.get("face")));

            leaderboards.add(new Leaderboard(type, loc, face));
        }
        ConfigurationSection personalStatsSection = section.getConfigurationSection("personalStatsBoard");
        if (personalStatsSection != null) {
            Location loc = LocationParser.parse(personalStatsSection.getString("loc"));
            Direction face = Direction.valueOf(personalStatsSection.getString("face"));
            personalStatsBoard = new PersonalStatsBoard(LeaderboardType.PERSONAL, loc, face);
        }

        region = new Region(Objects.requireNonNull(section.getConfigurationSection("leaderboardsRegion")));
    }

    /**
     * Check if the player is in the leaderboard display region
     *
     * @param player The player
     * @return True if in the region
     */
    public boolean isInRegion(GamePlayer player) {
        if (player.isOffline())
            return false;

        return region.isIn(player.getPlayer().getLocation(), true);
    }

    /**
     * Update the leaderboards for a specific player
     *
     * @param player The player
     */
    public void update(GamePlayer player) {
        leaderboards.forEach(l -> l.update(player));

        if (personalStatsBoard != null) {
            personalStatsBoard.update(player);
        }
    }
}
