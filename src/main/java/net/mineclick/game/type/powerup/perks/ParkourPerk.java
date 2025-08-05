package net.mineclick.game.type.powerup.perks;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.util.Formatter;

@RequiredArgsConstructor
public class ParkourPerk extends PowerupPerk {
    private final double perkPerIsland;

    public double getPerk(GamePlayer player) {
        return player.getParkour().getCompletedIslands().size() * perkPerIsland;
    }

    @Override
    public String getDescription(GamePlayer player) {
        double perk = getPerk(player);
        return "+" + Formatter.format(perkPerIsland * 100) + "% power for every completed\n" +
                "parkour course in current dimension (+" + Formatter.format(perk * 100) + "%)";
    }
}
