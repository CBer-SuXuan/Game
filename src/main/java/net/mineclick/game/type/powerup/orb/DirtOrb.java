package net.mineclick.game.type.powerup.orb;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;

public class DirtOrb extends OrbPowerup {
    public DirtOrb(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public void onOrbSpawn(Orb orb) {
        orb.setParticleTick(() -> ParticlesUtil.sendBlock(orb.getHeadLocation(), Material.DIRT, Triple.of(0.1F, 0.1F, 0.1F), 0.1F, 1, getPlayers()));
    }

    @Override
    public boolean onShoot(Orb orb) {
        Location location = orb.getHeadLocation();
        getPlayer().playSound(Sound.BLOCK_GRASS_BREAK, location, 0.25, 1);
        ParticlesUtil.sendBlock(location, Material.DIRT, Triple.of(0.5F, 0.5F, 0.5F), 0.1F, 25, getPlayers());
        orb.discard();

        return true;
    }
}
