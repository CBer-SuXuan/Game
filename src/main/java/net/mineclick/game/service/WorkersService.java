package net.mineclick.game.service;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.model.worker.WorkerConfiguration;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.global.service.ConfigurationsService;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

@SingletonInit
public class WorkersService {
    private static WorkersService i;

    @Getter
    private final Map<WorkerType, WorkerConfiguration> configurations = new LinkedHashMap<>();

    private WorkersService() {
        ConfigurationsService.i().onUpdate("workers", this::load);
    }

    public static WorkersService i() {
        return i == null ? i = new WorkersService() : i;
    }

    private void load() {
        ConfigurationSection workersSection = ConfigurationsService.i().get("workers");
        if (workersSection != null) {
            for (String id : workersSection.getKeys(false)) {
                ConfigurationSection section = workersSection.getConfigurationSection(id);
                WorkerType type = WorkerType.valueOf(id.toUpperCase());
                WorkerConfiguration config = configurations.computeIfAbsent(type, s -> new WorkerConfiguration());

                config.setName(section.getString("name", ""));
                config.setBaseCost(new BigNumber(section.getString("baseCost", "0")));
                config.setCostMultiplier(section.getDouble("costMultiplier", 0));
                config.setBaseIncome(new BigNumber(section.getString("baseIncome", "0")));
                config.setSkin(section.getString("skin", "2"));
            }
        }
    }

    /**
     * Load workers and their configs
     *
     * @param player The player
     */
    public void loadPlayerWorkers(GamePlayer player) {
        player.getWorkers().forEach((type, worker) -> loadWorkerConfig(worker, type, player));
    }

    /**
     * Unlock a worker type for this player if wasn't unlocked already
     *
     * @param player     The player
     * @param workerType The worker type
     */
    public void unlockWorker(GamePlayer player, WorkerType workerType) {
        if (player.getWorkers().containsKey(workerType)) return;

        Worker worker = new Worker();
        worker.setType(workerType);
        player.getWorkers().put(workerType, worker);
        loadWorkerConfig(worker, workerType, player);

        if (player.isOnOwnIsland()) {
            worker.spawn(player.getCurrentIsland());
        }
    }

    private void loadWorkerConfig(Worker worker, WorkerType type, GamePlayer player) {
        worker.setPlayer(player);
        worker.setType(type);

        WorkerConfiguration config = configurations.get(type);
        if (config != null) {
            worker.setConfiguration(config);
            return;
        }

        Game.i().getLogger().warning("Could not load worker config ("
                + type
                + ") for "
                + player.getName()
                + " "
                + player.getUuid());
    }
}
