package net.mineclick.game.type.powerup.orb;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.Sound;

public class Ball8Orb extends OrbPowerup {
    public Ball8Orb(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);

        getPlayer().playSound(Sound.BLOCK_ENCHANTMENT_TABLE_USE, player.getPlayer().getLocation(), 0.2, 1);
    }

    @Override
    public void onOrbSpawn(Orb orb) {
        orb.setParticleTick(() -> {
            Location location = orb.getHeadLocation();
            ParticlesUtil.send(ParticleTypes.ENCHANT, location, Triple.of(0.2F, 0.2F, 0.2F), 1, getPlayers());
        });
    }

    @Override
    public boolean onShoot(Orb orb) {
        Location location = orb.getHeadLocation();
        getPlayer().playSound(Sound.ITEM_BOOK_PUT, location, 0.2, 1);
        ParticlesUtil.send(ParticleTypes.ENCHANTED_HIT, location, Triple.of(0.5F, 0.5F, 0.5F), 15, getPlayers());
        orb.discard();

        return true;
    }
}
