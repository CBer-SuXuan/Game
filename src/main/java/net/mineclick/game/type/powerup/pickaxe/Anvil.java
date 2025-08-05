package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.PacketFallingBlock;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class Anvil extends Powerup {
    public Anvil(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 10 == 0) {
            Block original = getRandomBlock();
            if (original == null) return;

            Location spawn = original.getLocation();
            for (int i = 0; i < 5; i++) {
                Block block = spawn.getBlock().getRelative(BlockFace.UP);
                spawn = block.getLocation();
                Material type = block.getType();
                if (!type.equals(Material.AIR) && !type.equals(Material.RAIL) && !type.equals(original.getType())) {
                    break;
                }
            }

            PacketFallingBlock fallingBlock = new PacketFallingBlock(spawn.add(0.5, 0, 0.5), Bukkit.createBlockData(Material.ANVIL), getPlayers());
            fallingBlock.setMinY(original.getY());
            fallingBlock.setOnHitGround((location) -> {
                location = location.getBlock().getLocation().add(0.5, 0.5, 0.5);

                getPlayer().playSound(Sound.BLOCK_ANVIL_LAND, location, 0.3, 1);

                dropItem(location, 5);
                if (!getPlayer().isOffline()) {
                    ParticlesUtil.send(ParticleTypes.CRIT, location, Triple.of(0.5F, 0.5F, 0.5F), 10, getPlayers());
                }
            });
        }
    }
}
