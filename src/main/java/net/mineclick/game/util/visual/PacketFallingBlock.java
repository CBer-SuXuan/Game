package net.mineclick.game.util.visual;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.nms.NMS;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.global.util.Runner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R1.block.data.CraftBlockData;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Getter
@Setter
public class PacketFallingBlock {
    private final int id = NMS.createId();
    private final UUID uuid = UUID.randomUUID();
    private final Set<UUID> spawnedFor = new HashSet<>();

    private final Location location;
    private final BlockData blockData;
    private final Set<GamePlayer> players;

    private double minY = 0;
    private Vector velocity = new Vector(0, 0, 0);
    private Consumer<Location> onHitGround = l -> {
    };

    public PacketFallingBlock(Location location, BlockData blockData, Set<GamePlayer> players) {
        this(location, new Vector(0, 0, 0), blockData, players);
    }

    public PacketFallingBlock(Location location, Vector velocity, BlockData blockData, Set<GamePlayer> players) {
        this.location = location;
        this.blockData = blockData;
        this.players = players;
        this.velocity = velocity;

        EntityPolice.getGloballyExcluded().add(id);

        BlockState blockState = ((CraftBlockData) blockData).getState();
        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(id, uuid, location.getX(), location.getY(), location.getZ(), 0, 0, EntityType.FALLING_BLOCK, Block.getId(blockState), new Vec3(velocity.getX(), velocity.getY(), velocity.getZ()), 0);

        List<WrappedDataValue> wrappedDataValueList = Lists.newArrayList();
        wrappedDataValueList.add(new WrappedDataValue(8, NMS.BLOCK_POS, new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ())));

        PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, id);
        metadataPacket.getDataValueCollectionModifier().write(0, wrappedDataValueList);

        players.forEach(p -> {
            p.sendPacket(addEntityPacket);
            ProtocolLibrary.getProtocolManager().sendServerPacket(p.getPlayer(), metadataPacket);
        });

        Runner.sync(0, 1, (state) -> {
            if (state.getTicks() > 200 || location.getY() < minY) {
                onHitGround.accept(location);

                ClientboundRemoveEntitiesPacket removeEntitiesPacket = new ClientboundRemoveEntitiesPacket(id);
                players.forEach(p -> p.sendPacket(removeEntitiesPacket));

                EntityPolice.getGloballyExcluded().remove(id);

                state.cancel();
                return;
            }

            velocity.setY(velocity.getY() - 0.04);
            velocity.multiply(0.98);
            location.add(velocity);

            // teleport packet
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
            packet.getIntegers().write(0, id);

            packet.getDoubles().write(0, location.getX());
            packet.getDoubles().write(1, location.getY());
            packet.getDoubles().write(2, location.getZ());

            players.forEach(p -> ProtocolLibrary.getProtocolManager().sendServerPacket(p.getPlayer(), packet));

            // velocity packet
            ClientboundSetEntityMotionPacket motionPacket = new ClientboundSetEntityMotionPacket(id, new Vec3(velocity.getX(), velocity.getY(), velocity.getZ()));
            players.forEach(p -> p.sendPacket(motionPacket));
        });
    }
}
