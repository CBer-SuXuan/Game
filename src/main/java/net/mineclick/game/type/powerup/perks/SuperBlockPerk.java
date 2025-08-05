package net.mineclick.game.type.powerup.perks;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.util.Formatter;

@RequiredArgsConstructor
public class SuperBlockPerk extends PowerupPerk {
    public double getPerk(GamePlayer player) {
        return player.getSuperBlockData().getChance();
    }

    @Override
    public String getDescription(GamePlayer player) {
        double perk = getPerk(player);
        return "Get same percent power increase as the\n" +
                "current super block chance (+" + Formatter.format(perk * 100) + "%).\n" +
                "Boosters do not apply";
    }
}
