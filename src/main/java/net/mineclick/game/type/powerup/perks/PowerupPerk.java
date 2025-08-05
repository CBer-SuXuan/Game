package net.mineclick.game.type.powerup.perks;

import net.mineclick.game.model.GamePlayer;

public abstract class PowerupPerk {
    public abstract double getPerk(GamePlayer player);

    public abstract String getDescription(GamePlayer player);
}
