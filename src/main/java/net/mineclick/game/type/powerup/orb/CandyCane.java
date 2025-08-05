package net.mineclick.game.type.powerup.orb;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.DustParticleOptions;

public class CandyCane extends OrbPowerup {
    public CandyCane(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public boolean onShoot(Orb orb) {
        ParticlesUtil.send(DustParticleOptions.REDSTONE, orb.getHeadLocation(), Triple.of(0.5F, 0.5F, 0.5F), 25, getPlayers());
        orb.discard();

        return true;
    }
}
