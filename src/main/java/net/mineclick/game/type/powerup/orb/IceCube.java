package net.mineclick.game.type.powerup.orb;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import org.bukkit.Material;
import org.bukkit.Sound;

public class IceCube extends OrbPowerup {
    public IceCube(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public boolean onShoot(Orb orb) {
        ParticlesUtil.sendBlock(orb.getHeadLocation(), Material.ICE, Triple.of(0.5F, 0.5F, 0.5F), 0.1F, 25, getPlayers());

        getPlayer().playSound(Sound.BLOCK_GLASS_BREAK, orb.getHeadLocation(), 0.2, 1);

        orb.discard();
        return true;
    }
}
