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
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.HologramsService;
import net.mineclick.game.util.nms.NMS;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Setter
@Getter
public class PacketNPC {
    private final int id = NMS.createId();
    private final UUID uuid = UUID.randomUUID();
    private final List<PacketHologram> holograms = new ArrayList<>();
    private final Location location;
    private final VillagerType villagerType;
    private final VillagerProfession villagerProfession;
    private final Set<UUID> lookingAt = new HashSet<>();
    private final Set<UUID> spawnedFor = new HashSet<>();
    private final Function<GamePlayer, Boolean> visibility;
    private final LoadingCache<GamePlayer, Boolean> visibilityCache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Boolean load(@Nonnull GamePlayer player) {
                    return visibility != null && visibility.apply(player) && player.getPlayer().getLocation().distanceSquared(location) < 250000;
                }
            });
    private Consumer<GamePlayer> clickConsumer;
    private Function<GamePlayer, Boolean> hasParticles;

    public PacketNPC(Location location, VillagerType villagerType, VillagerProfession villagerProfession, Function<GamePlayer, Boolean> visibility) {
        this.location = location;
        this.villagerType = villagerType;
        this.villagerProfession = villagerProfession;
        this.visibility = visibility;

        EntityPolice.getGloballyExcluded().add(id);
    }

    public void addHologram(Function<GamePlayer, String> text, boolean floating) {
        double dY = holograms.size() * 0.25;
        PacketHologram hologram = HologramsService.i().spawn(location.clone().add(0, 2 + dY, 0), player -> {
            if (visibilityCache.getUnchecked(player)) {
                return text.apply(player);
            }

            return null;
        }, floating);

        holograms.add(hologram);
    }

    public void tick(GamePlayer player) {
        if (player.getActivityData().wasMoving(5)) {
            if (visibilityCache.getUnchecked(player)) {
                if (spawnedFor.add(player.getUuid())) {
                    spawnFor(player);
                }

                // look towards player
                Location pLoc = player.getPlayer().getLocation();
                if (location.distanceSquared(pLoc) <= 25) {
                    lookingAt.add(player.getUuid());

                    Vector dir = pLoc.toVector().subtract(location.toVector()).normalize();
                    double theta = Math.atan2(-dir.getX(), dir.getZ());
                    float yaw = (float) Math.toDegrees((theta + 6.283185307179586D) % 6.283185307179586D);

                    sendHeadRotation(player, yaw);
                } else if (lookingAt.remove(player.getUuid())) {
                    sendHeadRotation(player, location.getYaw());
                }
            } else if (spawnedFor.remove(player.getUuid())) {
                despawnFor(player);
            }
        }

        // tick particles
        if (Game.getRandom().nextInt(5) == 0 && visibilityCache.getUnchecked(player) && hasParticles != null && hasParticles.apply(player)) {
            ParticlesUtil.send(ParticleTypes.HAPPY_VILLAGER, location.clone().add(0, 0.1, 0), Triple.of(0.25F, 0.05F, 0.25F), 3, player);
        }
    }

    public void handleClick(GamePlayer player) {
        if (clickConsumer != null) {
            clickConsumer.accept(player);
        }
    }

    public void spawnFor(GamePlayer player) {
        sendSpawn(player);
        sendMetadata(player);
        sendHeadRotation(player, location.getYaw());
    }

    public void despawnFor(GamePlayer player) {
        player.sendPacket(new ClientboundRemoveEntitiesPacket(id));

        holograms.forEach(h -> {
            h.getSpawnedFor().remove(player.getUuid());
            h.despawnFor(player);
        });
    }

    private void sendSpawn(GamePlayer player) {
        ClientboundAddEntityPacket packet = new ClientboundAddEntityPacket(id, uuid, location.getX(), location.getY(), location.getZ(), 0, 0, EntityType.VILLAGER, 0, Vec3.ZERO, 0);
        player.sendPacket(packet);
    }

    private void sendMetadata(GamePlayer player) {
        List<WrappedDataValue> wrappedDataValueList = Lists.newArrayList();
        wrappedDataValueList.add(new WrappedDataValue(4, NMS.BOOLEAN_TYPE, true)); // silent
        wrappedDataValueList.add(new WrappedDataValue(5, NMS.BOOLEAN_TYPE, true)); // no gravity
        wrappedDataValueList.add(new WrappedDataValue(18, NMS.VILLAGER_DATA_TYPE, new VillagerData(villagerType, villagerProfession, 1))); // silent

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, id);
        packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);

        ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), packet);
    }

    private void sendHeadRotation(GamePlayer player, float rotation) {
        int i = Mth.floor(rotation * 256.0F / 360.0F);

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        packet.getIntegers().write(0, id);
        packet.getBytes().write(0, (byte) i);

        ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), packet);
    }

    public void refresh(GamePlayer player) {
        visibilityCache.refresh(player);
        holograms.forEach(h -> h.refresh(player));

        if (!visibilityCache.getUnchecked(player)) {
            despawnFor(player);
        }
    }
}
