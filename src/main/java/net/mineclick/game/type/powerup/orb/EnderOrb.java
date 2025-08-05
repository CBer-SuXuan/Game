package net.mineclick.game.type.powerup.orb;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.Sound;

public class EnderOrb extends OrbPowerup {
    public EnderOrb(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public void onOrbSpawn(Orb orb) {
        orb.setParticleTick(() -> ParticlesUtil.send(ParticleTypes.FALLING_OBSIDIAN_TEAR, orb.getHeadLocation(), Triple.of(0.1F, 0.1F, 0.1F), 1, getPlayers()));
    }

    @Override
    public boolean onShoot(Orb orb) {
        Location location = orb.getHeadLocation();
        getPlayer().playSound(Sound.ENTITY_ENDERMAN_TELEPORT, location, 0.2, 1.5);
        ParticlesUtil.send(ParticleTypes.PORTAL, location, Triple.of(0.5F, 0.5F, 0.5F), 15, getPlayers());
        orb.discard();

        return true;
    }
}
