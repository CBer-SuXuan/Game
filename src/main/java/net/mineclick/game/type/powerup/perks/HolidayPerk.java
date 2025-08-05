package net.mineclick.game.type.powerup.perks;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.Holiday;
import net.mineclick.global.util.Formatter;

@RequiredArgsConstructor
public class HolidayPerk extends PowerupPerk {
    private final Holiday holiday;
    private final double perk;

    @Override
    public double getPerk(GamePlayer player) {
        return holiday.isNow() ? perk : 0;
    }

    @Override
    public String getDescription(GamePlayer player) {
        return "+" + Formatter.format(perk * 100) + "% power when used during\nthe " + holiday.getName() + " holiday";
    }
}
