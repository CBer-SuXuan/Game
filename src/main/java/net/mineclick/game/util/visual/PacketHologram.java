package net.mineclick.game.util.visual;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.nms.NMS;
import net.mineclick.game.util.packet.EntityPolice;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Getter
@Setter
public class PacketHologram {
    private final int id = NMS.createId();
    private final UUID uuid = UUID.randomUUID();
    private final Set<UUID> spawnedFor = new HashSet<>();

    private final Location location;
    private final Function<GamePlayer, String> textFunction;
    private final LoadingCache<GamePlayer, Boolean> visibilityCache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Boolean load(@Nonnull GamePlayer player) {
                    if (player.getPlayer().getLocation().distanceSquared(location) > 250000) {
                        return false;
                    }
                    String text = textFunction == null ? null : textFunction.apply(player);
                    return text != null && !text.isEmpty();
                }
            });
    public double floatY = 0;
    public double locationY;
    public boolean floating;
    public Function<GamePlayer, Integer> backgroundFunction;
    public int lifespan = 0;

    public PacketHologram(Location location, Function<GamePlayer, String> textFunction) {
        this.location = location;
        this.textFunction = textFunction;

        locationY = location.getY();

        EntityPolice.getGloballyExcluded().add(id);
    }

    // return true if the hologram should be removed
    public boolean tick(GamePlayer player) {
        if (lifespan > 0 && --lifespan <= 0) {
            return true;
        }

        if (visibilityCache.getUnchecked(player)) {
            if (spawnedFor.add(player.getUuid())) {
                spawnFor(player);
            }

            if (player.getTicks() % 40 == 0) {
                sendMetadata(player);
            }

            if (floating || player.getTicks() % 100 == 0) {
                sendTeleport(player);
            }
        } else if (spawnedFor.remove(player.getUuid())) {
            despawnFor(player);
        }

        return false;
    }

    public void refresh(GamePlayer player) {
        visibilityCache.refresh(player);
        spawnedFor.remove(player.getUuid());

        if (!visibilityCache.getUnchecked(player)) {
            despawnFor(player);
        }
    }

    public void floatTick() {
        if (floating) {
            locationY = location.getY() + 0.25 + Math.sin(floatY += 0.07854) * 0.25;
        } else if (locationY != location.getY()) {
            locationY = location.getY();
        }
    }

    private void sendTeleport(GamePlayer player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, id);

        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, locationY);
        packet.getDoubles().write(2, location.getZ());

        ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), packet);
    }

    private void spawnFor(GamePlayer player) {
        sendSpawn(player);
        sendMetadata(player);
    }

    public void despawnFor(GamePlayer player) {
        player.sendPacket(new ClientboundRemoveEntitiesPacket(id));
    }

    private void sendSpawn(GamePlayer player) {
        ClientboundAddEntityPacket packet = new ClientboundAddEntityPacket(id, uuid, location.getX(), location.getY(), location.getZ(), 0, 0, EntityType.TEXT_DISPLAY, 0, Vec3.ZERO, 0);
        player.sendPacket(packet);
    }

    private void sendMetadata(GamePlayer player) {
        String text = textFunction.apply(player);
        if (text == null) {
            text = "";
        }

        List<WrappedDataValue> wrappedDataValueList = Lists.newArrayList();
        wrappedDataValueList.add(new WrappedDataValue(14, NMS.BYTE_TYPE, (byte) 3)); // billboard center
        wrappedDataValueList.add(new WrappedDataValue(22, NMS.CHAT_COMPONENT_TYPE, CraftChatMessage.fromString(text)[0]));
        wrappedDataValueList.add(new WrappedDataValue(23, NMS.INT_TYPE, 300));
//        wrappedDataValueList.add(new WrappedDataValue(26, NMS.BYTE_TYPE, (byte) 0x02)); // see through walls

        if (backgroundFunction != null) {
            wrappedDataValueList.add(new WrappedDataValue(24, NMS.INT_TYPE, backgroundFunction.apply(player)));
        }

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, id);
        packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);

        ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), packet);
    }
}
