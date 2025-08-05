package net.mineclick.game.type.powerup.orb;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;

public class TntOrb extends OrbPowerup {
    public TntOrb(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);

        getPlayer().playSound(Sound.ENTITY_TNT_PRIMED, player.getPlayer().getLocation(), 0.2, 1);
    }

    @Override
    public void onOrbSpawn(Orb orb) {
        orb.setParticleTick(() -> {
            Location location = orb.getHeadLocation();
            if (Game.getRandom().nextInt(10) == 0) {
                DroppedItem.spawn(Material.TNT, location, 20, getPlayers());
            }
            ParticlesUtil.send(ParticleTypes.CRIT, location, Triple.of(0.1F, 0.1F, 0.1F), 1, getPlayers());
        });
    }

    @Override
    public boolean onShoot(Orb orb) {
        Location location = orb.getHeadLocation();
        getPlayer().playSound(Sound.ENTITY_GENERIC_EXPLODE, location, 0.2, 1);
        ParticlesUtil.send(ParticleTypes.FLAME, location, Triple.of(0.5F, 0.5F, 0.5F), 15, getPlayers());
        ParticlesUtil.send(ParticleTypes.LARGE_SMOKE, location, Triple.of(0.5F, 0.5F, 0.5F), 5, getPlayers());
        orb.discard();

        return true;
    }
}
