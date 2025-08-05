package net.mineclick.game.type.powerup.perks;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.config.DimensionConfig;
import net.mineclick.global.util.Formatter;

@RequiredArgsConstructor
public class DimensionPerk extends PowerupPerk {
    private final int dimensionId;
    private final double perk;

    @Override
    public double getPerk(GamePlayer player) {
        return player.getDimensionsData().getCurrentDimensionId() == dimensionId ? perk : 0;
    }

    @Override
    public String getDescription(GamePlayer player) {
        DimensionConfig dimensionConfig = DimensionConfig.getById(dimensionId);
        if (dimensionConfig == null) return "";

        return "+" + Formatter.format(perk * 100) + "% power when used in\nthe " + dimensionConfig.getName() + " dimension";
    }
}
