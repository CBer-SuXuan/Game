package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.model.DynamicMineBlock;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Ice extends Powerup {
    private final Set<Block> modifiedBlocks = new HashSet<>();
    private final List<Block> blocks;
    private int ageToSet = 0;

    public Ice(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);

        blocks = getBlocks();
    }

    @Override
    public double getPeriod() {
        return 4;
    }

    @Override
    public void onReset(boolean premature) {
        resetBlocks();
    }

    private void resetBlocks() {
        for (Block block : blocks) {
            for (GamePlayer player : getPlayers()) {
                DynamicMineBlock dynamicMineBlock = player.getDynamicMineBlocks().get(block);
                if (dynamicMineBlock != null) {
                    dynamicMineBlock.setTemporaryBlockData(null);
                    dynamicMineBlock.update();
                } else {
                    player.sendBlockChange(block.getLocation(), block.getBlockData());
                }
            }
        }
    }

    @Override
    public void tick(long ticks) {
        if (ticks == 80) {
            resetBlocks();

            Set<GamePlayer> players = getPlayers();
            for (int i = 0; i < 10 && i < blocks.size(); i++) {
                Location location = blocks.get(i).getLocation().add(0.5, 0.5, 0.5);
                dropItem(location, 1);
                ParticlesUtil.send(ParticleTypes.ITEM_SNOWBALL, location, Triple.of(1F, 1F, 1F), 10, players);
            }

            getPlayer().playSound(Sound.BLOCK_GLASS_BREAK);
            return;
        }
        if (ticks > 78) return;

        if (ticks % 20 == 0 && ticks > 0) {
            int newAge = Math.min((int) (ticks / 20) + 1, 3);
            if (newAge != ageToSet) {
                ageToSet = newAge;
                modifiedBlocks.clear();
            }
        }

        if (modifiedBlocks.size() == blocks.size()) return;
        Collections.shuffle(blocks);

        Block block = null;
        for (Block b : blocks) {
            if (!modifiedBlocks.contains(b)) {
                block = b;
                break;
            }
        }
        if (block == null) return;

        modifiedBlocks.add(block);
        for (GamePlayer player : getPlayers()) {
            BlockData blockData = Bukkit.createBlockData(Material.FROSTED_ICE, "[age=" + ageToSet + "]");
            if (getDynamicMineBlockType() != null) {
                DynamicMineBlock dynamicMineBlock = player.getDynamicMineBlocks().get(block);
                if (dynamicMineBlock != null) {
                    dynamicMineBlock.setTemporaryBlockData(blockData);
                }
            } else {
                player.sendBlockChange(block.getLocation(), blockData);
            }
        }
    }

    @Override
    public boolean ignorePlayerHeadBlocks() {
        return true;
    }
}
