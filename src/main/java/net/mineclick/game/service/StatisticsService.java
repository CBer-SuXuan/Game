package net.mineclick.game.service;

import net.mineclick.core.messenger.Action;
import net.mineclick.game.Game;
import net.mineclick.game.messenger.StatisticsHandler;
import net.mineclick.game.model.Statistic;
import net.mineclick.game.type.StatisticType;
import net.mineclick.global.model.PlayerId;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.service.ServersService;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.SingletonInit;

import java.util.*;
import java.util.stream.Collectors;

// TODO this might throw some concurrency exceptions,
//  but I think the collision in handler's response should be rare enough.
//  Still keep an eye on it.
@SingletonInit
public class StatisticsService {
    private static StatisticsService i;

    private final Map<UUID, Map<String, Statistic>> cache = new HashMap<>();
    private Set<UUID> loadBuffer; // Buffer used to collect all load requests and send all at once (3 sec delay)

    private StatisticsService() {
        // Flush statistics cache (save to backend) every 15 seconds
        Runner.sync(300, 300, state -> flush());

        ServersService.i().onShutdown(this::flush);
    }

    public static StatisticsService i() {
        return i == null ? i = new StatisticsService() : i;
    }

    /**
     * Increment a statistic by 1
     *
     * @param uuid      UUID of the player
     * @param statistic The statistic
     */
    public void increment(UUID uuid, StatisticType statistic) {
        increment(uuid, statistic, 1);
    }

    /**
     * Increment a statistic by the specific amount
     *
     * @param uuid   UUID of the player
     * @param type   The statistic type
     * @param amount The amount to increment by. Must be bigger than 0
     */
    public void increment(UUID uuid, StatisticType type, double amount) {
        if (amount > 0) {
            if (cache.containsKey(uuid)) {
                Map<String, Statistic> map = cache.get(uuid);
                Statistic existingStatistic = map.computeIfAbsent(type.getKey(), t -> new Statistic());
                existingStatistic.setScore(existingStatistic.getScore() + amount);
            } else {
                Game.i().getLogger().warning("Statistics not loaded. " + uuid + " " + type.getKey() + " " + amount);
            }
        } else {
            Game.i().getLogger().warning("Invalid statistic amount. " + uuid + " " + type.getKey() + " " + amount);
        }
    }

    /**
     * Check if the player's statistics were loaded
     *
     * @param uuid The player's uuid
     * @return True if the statistics for this player were loaded
     */
    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /**
     * Get the statistic by type for a specific player
     *
     * @param uuid Player's uuid
     * @param type Statistic type
     * @return The statistic. <b>May return null if the player wasn't loaded yet</b>
     */
    public Statistic get(UUID uuid, StatisticType type) {
        if (isLoaded(uuid)) {
            Map<String, Statistic> map = cache.get(uuid);
            return map.computeIfAbsent(type.getKey(), t -> new Statistic());
        }

        return new Statistic();
    }

    /**
     * Load statistics for a given player<br>
     * <b>Should only be called once on player join.</b>
     * Otherwise the existing statistic changes will be lost.
     *
     * @param uuid The player's uuid
     */
    public void load(UUID uuid) {
        if (loadBuffer == null) {
            loadBuffer = new HashSet<>();

            Runner.sync(60, () -> {
                if (loadBuffer == null) {
                    return;
                }

                Set<String> keys = Arrays.stream(StatisticType.values())
                        .map(StatisticType::getKey)
                        .collect(Collectors.toSet());

                StatisticsHandler handler = new StatisticsHandler();
                handler.setKeys(keys);
                handler.setUuids(loadBuffer);
                handler.setResponseConsumer(message -> {
                    Map<UUID, Map<String, Statistic>> statistics = ((StatisticsHandler) message).getStatistics();
                    if (statistics == null) {
                        statistics = new HashMap<>();
                    }

                    cache.putAll(statistics);
                });

                handler.send(Action.GET);
                loadBuffer = null;
            });
        }

        loadBuffer.add(uuid);
    }

    /**
     * Manually flush (save) all cached statistics to the backend.
     * Should only be called on server shutdown as it's already flushed periodically.
     */
    public void flush() {
        flush(null);
    }

    /**
     * Flush (save) statistics for a specific player
     *
     * @param uuid UUID of the player. <b>Will flush all statistics for all players if null</b>
     */
    public void flush(UUID uuid) {
        Map<UUID, Map<String, Statistic>> statistics = cache;
        if (uuid != null) {
            Map<String, Statistic> stats = cache.get(uuid);
            if (stats == null) return;

            statistics = Collections.singletonMap(uuid, stats);
        }

        StatisticsHandler handler = new StatisticsHandler();
        handler.setStatistics(statistics);
        handler.setResponseConsumer(message -> {
            if (message == null) return;

            // Update the ranks
            Map<UUID, Map<String, Statistic>> updatedStats = ((StatisticsHandler) message).getStatistics();
            if (updatedStats != null && !updatedStats.isEmpty()) {
                updatedStats.forEach((id, map) -> {
                    Map<String, Statistic> cachedStats = cache.get(id);
                    if (cachedStats != null) {
                        cachedStats.forEach((statisticKey, statistic) -> {
                            Statistic updatedStat = map.get(statisticKey);
                            if (updatedStat != null) {
                                statistic.setRank(updatedStat.getRank());
                            }
                        });
                    }
                });
            }
        });
        handler.send(Action.POST);

        // Clear cache off of offline players after it's been flushed
        Set<UUID> toRemove = new HashSet<>(cache.keySet());
        toRemove.removeAll(PlayersService.i().getAll().stream()
                .filter(playerModel -> !playerModel.isOffline())
                .map(PlayerId::getUuid)
                .collect(Collectors.toSet()));
        cache.keySet().removeAll(toRemove);
    }

    /**
     * Fully reset this player's statistics
     *
     * @param uuid The player's uuid
     */
    public void reset(UUID uuid) {
        Map<String, Statistic> map = cache.computeIfAbsent(uuid, u -> new HashMap<>());
        for (StatisticType type : StatisticType.values()) {
            map.put(type.getKey(), new Statistic());
        }
    }
}
