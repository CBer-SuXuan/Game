package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.game.service.QuestsService;
import net.mineclick.global.config.BuildingConfig;

import java.util.Set;

@Data
public class BuildingModel {
    private final int id;
    private int level = 0;

    private transient BuildingConfig config;
    private transient IslandModel island;

    /**
     * Build this building
     */
    public void build() {
        Set<GamePlayer> players = island.getAllPlayers();
        config.getSchematics().get(level).sendFakeOrLoad(config.getRegion().getMin(), players);
    }

    /**
     * @return True if this building can be upgraded further
     */
    public boolean canLevelUp() {
        return config.getNames().size() > level;
    }

    public void upgrade() {
        if (canLevelUp()) {
            level++;
            build();

            island.getPlayer().recalculateGoldRate();

            QuestsService.i().incrementProgress(island.getPlayer(), "dailyBuildings", 0, 1);
        }
    }
}
