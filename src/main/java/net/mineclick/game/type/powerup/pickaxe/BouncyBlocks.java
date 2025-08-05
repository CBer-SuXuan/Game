package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.model.DynamicMineBlock;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.Set;

public class BouncyBlocks extends Powerup {
    public BouncyBlocks(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 10 == 0) {
            Block original = getRandomBlock();
            if (original == null) return;

            Location spawn = original.getLocation();
            BlockData blockData = original.getBlockData();
            if (blockData.getMaterial().equals(Material.PLAYER_HEAD)) {
                blockData = Bukkit.createBlockData(Material.SLIME_BLOCK);
            }

            if (getDynamicMineBlockType() != null) {
                DynamicMineBlock dynamicMineBlock = getPlayer().getDynamicMineBlocks().get(original);
                if (dynamicMineBlock != null) {
                    blockData = Bukkit.createBlockData(dynamicMineBlock.getMaterial());
                }
            }

            PacketFallingBlock fallingBlock = new PacketFallingBlock(spawn.add(0.5, 0.2, 0.5), new Vector(0, 0.5, 0), blockData, getPlayers());
            fallingBlock.setMinY(spawn.getY() - 0.25);
            fallingBlock.setOnHitGround(location -> {
                getPlayer().playSound(Sound.BLOCK_SAND_FALL, location, 1, 1);

                dropItem(location, 5);
                ParticlesUtil.send(ParticleTypes.CRIT, location.add(0.5, 0.5, 0.5), Triple.of(0.5F, 0.5F, 0.5F), 10, getPlayers());

                // fix client side disappearing blocks
                Set<GamePlayer> players = getPlayers();
                Block spawnBlock = spawn.getBlock();
                players.forEach(player -> {
                    DynamicMineBlock dynamicMineBlock = player.getDynamicMineBlocks().get(spawnBlock);
                    if (dynamicMineBlock != null) {
                        dynamicMineBlock.update();
                    } else {
                        player.sendBlockChange(spawn, spawnBlock.getBlockData());
                    }
                });
            });
        }
    }

    @Override
    public boolean ignorePlayerHeadBlocks() {
        return true;
    }
}
