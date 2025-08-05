package net.mineclick.game.type.powerup.perks;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.BuildingModel;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.IslandModel;
import net.mineclick.global.util.Formatter;

@RequiredArgsConstructor
public class BuildingsPerk extends PowerupPerk {
    private final double perk;

    public double getPerk(GamePlayer player) {
        IslandModel island = player.getCurrentIsland(false);
        long count = island.getBuildings().stream().mapToInt(BuildingModel::getLevel).sum();
        long total = island.getBuildings().stream().mapToInt(buildingModel -> buildingModel.getConfig().getNames().size()).sum();

        return count != total ? 0 : perk;
    }

    @Override
    public String getDescription(GamePlayer player) {
        return "+" + Formatter.format(perk * 100) + "% power when used on an island\nwith all buildings unlocked";
    }
}
