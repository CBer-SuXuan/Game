package net.mineclick.game.type.powerup.orb;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Sound;

public class Snowball extends OrbPowerup {
    public Snowball(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public boolean onShoot(Orb orb) {
        ParticlesUtil.send(ParticleTypes.ITEM_SNOWBALL, orb.getHeadLocation(), Triple.of(0.5F, 0.5F, 0.5F), 15, getPlayers());

        getPlayer().playSound(Sound.BLOCK_SNOW_BREAK, orb.getHeadLocation(), 0.2, 1);

        orb.discard();
        return true;
    }
}
