package net.mineclick.game.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.mineclick.game.Game;
import net.mineclick.game.type.DynamicMineBlockType;
import net.mineclick.global.model.PlayerModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DynamicMineBlock {
    private final DynamicMineBlockType type;
    private final Material material;
    private final int maxClicks;
    @EqualsAndHashCode.Include
    private final transient Block block;
    private int clicks;

    private transient Set<GamePlayer> players = new HashSet<>();
    private transient BlockData blockData;
    private transient Runnable onBreak;
    private transient BlockData temporaryBlockData;
    private transient long lastUpdated = 0;
    private transient BiConsumer<GamePlayer, DynamicMineBlock> breakConsumer;

    public void addPlayers(Collection<GamePlayer> collection) {
        collection.forEach(player -> player.getDynamicMineBlocks().put(block, this));
        players.addAll(collection);

        if (blockData == null) {
            blockData = Bukkit.createBlockData(material);
        }
        update();
    }

    public void addPlayer(GamePlayer player) {
        player.getDynamicMineBlocks().put(block, this);
        players.add(player);

        if (blockData == null) {
            blockData = Bukkit.createBlockData(material);
        }
        update();
    }

    public void destroy(Block block) {
        for (GamePlayer player : players) {
            if (player.isOffline()) continue;

            player.getDynamicMineBlocks().remove(block);
            player.sendBlockChange(block);
        }

        players.clear();
    }

    public void onClick(GamePlayer player, int numOfClicks) {
        clicks += numOfClicks;
        update();

        if (clicks >= maxClicks) {
            if (onBreak != null) {
                onBreak.run();
            }

            destroy(block);
            if (breakConsumer != null) {
                breakConsumer.accept(player, this);
            }
        }
    }

    public void update() {
        players.removeIf(PlayerModel::isOffline);
        if (blockData == null) return;

        lastUpdated = System.currentTimeMillis();

        BlockData bData = blockData;
        if (temporaryBlockData != null) {
            bData = temporaryBlockData;
            temporaryBlockData = null;
        }

        BlockPos position = new BlockPos(block.getX(), block.getY(), block.getZ());
        int packetClicks = (int) (Math.min(10, (clicks / (double) maxClicks) * 10));
        ClientboundBlockDestructionPacket packet = null;
        if (packetClicks > 0) {
            packet = new ClientboundBlockDestructionPacket(Game.getRandom().nextInt(), position, packetClicks);
        }

        for (GamePlayer player : players) {
            player.sendBlockChange(block.getLocation(), bData);
            if (packet != null) {
                player.sendPacket(packet);
            }
        }
    }
}
