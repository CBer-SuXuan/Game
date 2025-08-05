package net.mineclick.game.service;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.mineclick.game.Game;
import net.mineclick.game.model.DynamicMineBlock;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.DynamicMineBlockType;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;
import java.util.function.BiConsumer;

@SingletonInit
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DynamicMineBlocksService {
    private static DynamicMineBlocksService i;

    private final List<BlockFace> blockFaces = Arrays.asList(BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.UP);
    private final List<BlockFace> blockFacesInverted = Arrays.asList(BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.DOWN);

    public static DynamicMineBlocksService i() {
        return i == null ? i = new DynamicMineBlocksService() : i;
    }

    /**
     * Check if the following player can see and interact
     * with a dynamic mine block at this location
     *
     * @param player The player
     * @param block  The block
     * @return True if the player can see and interact with this block
     */
    public boolean contains(GamePlayer player, Block block) {
        return player.getDynamicMineBlocks().containsKey(block);
    }

    /**
     * Get the dynamic mine block at the given location,
     * if one exists
     *
     * @param player The player that can see and interact with this block
     * @param block  The block
     * @return The block (if exists)
     */
    public DynamicMineBlock get(GamePlayer player, Block block) {
        return player.getDynamicMineBlocks().get(block);
    }

    /**
     * Get the material of the dynamic block at the given location,
     * if one exists
     *
     * @param player The player that can see and interact with this block
     * @param block  The block
     * @return The material (if exists)
     */
    public Material getMaterial(GamePlayer player, Block block) {
        DynamicMineBlock mineBlock = get(player, block);
        if (mineBlock != null) {
            return mineBlock.getMaterial();
        }

        return null;
    }

    /**
     * Create a block at a location for the given players
     *
     * @param type     Type of the block
     * @param block    The block
     * @param material The material of the block
     * @param maxClick The amount of max clicks it takes to break the block
     * @param players  The players
     * @return DynamicMineBlock
     */
    public DynamicMineBlock create(DynamicMineBlockType type, Block block, Material material, int maxClick, Collection<GamePlayer> players) {
        DynamicMineBlock mineBlock = new DynamicMineBlock(type, material, maxClick, block);
        mineBlock.addPlayers(players);
        return mineBlock;
    }

    /**
     * Clear all dynamic mine block from this player
     *
     * @param player      The player
     * @param updateBlock Whether to send the block update packet
     */
    public void clear(GamePlayer player, boolean updateBlock) {
        clear(player, null, updateBlock);
    }

    /**
     * Clear all dynamic mine block from this player
     *
     * @param player      The player
     * @param type        Type of the block. Passing null will clear everything
     * @param updateBlock Whether to send the block update packet
     */
    public void clear(GamePlayer player, DynamicMineBlockType type, boolean updateBlock) {
        player.getDynamicMineBlocks().entrySet().removeIf(entry -> {
            if (type != null && !entry.getValue().getType().equals(type)) return false;

            entry.getValue().getPlayers().remove(player);
            if (updateBlock) {
                player.sendBlockChange(entry.getKey());
            }

            return true;
        });
    }

    /**
     * Handle clicking of the dynamic mine block
     *
     * @param player The player that clicked
     * @param block  The block
     * @param clicks Number of clicks
     * @return True if clicked
     */
    public boolean click(GamePlayer player, Block block, int clicks) {
        DynamicMineBlock mineBlock = get(player, block);
        if (mineBlock != null && mineBlock.getMaxClicks() > 0) {
            mineBlock.onClick(player, clicks);
            return true;
        }
        return false;
    }

    /**
     * Update close (50 blocks away) dynamic mine blocks of the given player. Packets will only be sent for the blocks
     * that weren't updated in the last 5 seconds
     *
     * @param player The player
     */
    public void update(GamePlayer player) {
        if (player.isOffline()) return;

        Location loc = player.getPlayer().getLocation();
        long time = System.currentTimeMillis();
        player.getDynamicMineBlocks().entrySet().stream()
                .filter(e -> time - e.getValue().getLastUpdated() >= 5000 && e.getKey().getLocation().distanceSquared(loc) <= 10000)
                .forEach(e -> e.getValue().update());
    }

    /**
     * Generates a blob of dynamic mine blocks at the given location
     *
     * @param type           The type of the dynamic mine block
     * @param origin         The origin/center of the blob
     * @param inverted       Whether the blob should try to up (true) instead of down (false)
     * @param maxRadius      Max radius the blob can extend to
     * @param blockMaterial  The blob's material
     * @param clicksPerBlock Clicks per block that it takes to break it
     * @param breakConsumer  Called then a block from this blob is broken
     * @param players        The players that can see and interact with this blob
     * @return The collection of mine blocks that were placed
     */
    public Set<DynamicMineBlock> generateBlob(DynamicMineBlockType type, Location origin, boolean inverted, int maxRadius, Material blockMaterial, int clicksPerBlock, BiConsumer<GamePlayer, DynamicMineBlock> breakConsumer, Collection<GamePlayer> players) {
        int maxDistanceSquared = maxRadius * maxRadius;

        Set<Block> blocks = new HashSet<>();
        Set<DynamicMineBlock> dynamicMineBlocks = new HashSet<>();
        recursiveBlob(origin, inverted, maxDistanceSquared, blocks, origin.getBlock());
        blocks.forEach(block -> {
            DynamicMineBlock mineBlock = create(type, block, blockMaterial, clicksPerBlock, players);
            mineBlock.setBreakConsumer(breakConsumer);
            dynamicMineBlocks.add(mineBlock);
        });

        return dynamicMineBlocks;
    }

    private void recursiveBlob(Location origin, boolean inverted, double maxDistanceSquared, Set<Block> blocks, Block block) {
        for (BlockFace face : (inverted ? blockFacesInverted : blockFaces)) {
            Block relative = block.getRelative(face);
            if (blocks.contains(relative)) return;
            if (relative.getType().equals(Material.AIR) || relative.getType().toString().endsWith("_CARPET") || relative.getType().toString().endsWith("_BUTTON")) {
                double distanceSquared = origin.distanceSquared(relative.getLocation());
                if (distanceSquared <= maxDistanceSquared) {
                    blocks.add(relative);
                    if (face.equals(BlockFace.DOWN) || distanceSquared <= 4 || Game.getRandom().nextBoolean()) {
                        recursiveBlob(origin, inverted, maxDistanceSquared, blocks, relative);
                    }
                }
            }
        }
    }

}
