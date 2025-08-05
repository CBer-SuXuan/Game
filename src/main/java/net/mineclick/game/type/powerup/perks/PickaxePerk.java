package net.mineclick.game.type.powerup.perks;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.util.Formatter;

@RequiredArgsConstructor
public class PickaxePerk extends PowerupPerk {
    private final double perkPerLevel;

    public double getPerk(GamePlayer player) {
        if (player.getPickaxe().getLevel() < 50) return 0;

        return (int) (player.getPickaxe().getLevel() / 50) * perkPerLevel;
    }

    @Override
    public String getDescription(GamePlayer player) {
        double perk = getPerk(player);
        return "+" + Formatter.format(perkPerLevel * 100) + "% power for every 50 levels\nof Pickaxe" + " (" + Formatter.format(perk * 100) + "%)";
    }
}
