package net.mineclick.game.util.visual;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.model.PlayerModel;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.Runner;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @deprecated use {@link net.mineclick.game.util.visual.PacketNPC} instead
 */
@Getter
@Deprecated
public class NPC extends Villager {
    private final Location location;
    private final ClientboundLevelParticlesPacket particlesPacket;
    private final Set<GamePlayer> players;
    @Setter
    private Consumer<GamePlayer> clickConsumer;
    @Setter
    private Function<GamePlayer, Boolean> particlesFunction;
    @Setter
    private boolean looking = true;
    @Setter
    private boolean shouldMove = false;
    private boolean respawned;

    public NPC(Location location, Set<GamePlayer> players) {
        this(location, null, null, players);
    }

    public NPC(Location location, VillagerType type, VillagerProfession profession, Set<GamePlayer> players) {
        super(EntityType.VILLAGER, ((CraftWorld) location.getWorld()).getHandle());

        if (type != null && profession != null) {
            this.setVillagerData(this.getVillagerData().setType(type).setProfession(profession));
        }

        this.location = location;
        this.players = players;

        moveTo(location.getX(), location.getY(), location.getZ());
        for (GamePlayer player : players) {
            player.getAllowedEntities().add(getId());
        }

        particlesPacket = new ClientboundLevelParticlesPacket(ParticleTypes.HAPPY_VILLAGER, false, getX(), getY(), getZ(), 0.2F, 0.05F, 0.2F, 1, 3);

        persist = true;
        Runner.sync(() -> level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM));
    }

    @Override
    public void checkDespawn() {
        this.noActionTime = 0;
    }

    @Override
    public void remove(RemovalReason removalReason) {
        super.remove(removalReason);

        for (GamePlayer player : players) {
            player.getAllowedEntities().remove(getId());
        }
    }

    @Override
    protected Brain.Provider<Villager> brainProvider() {
        return Brain.provider(ImmutableList.of(), ImmutableList.of());
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

    @Override
    public void refreshBrain(ServerLevel worldserver) {
    }

    @Override
    public InteractionResult mobInteract(Player entityhuman, InteractionHand enumhand) {
        if (isAlive() && clickConsumer != null) {
            PlayersService.i().<GamePlayer>get(entityhuman.getUUID(), player -> {
                if (player != null) {
                    clickConsumer.accept(player);
                }
            });
        }

        return InteractionResult.PASS;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void baseTick() {
        if (shouldMove) {
            super.baseTick();
        }

        players.stream().filter(PlayerModel::isOffline).collect(Collectors.toSet()).forEach(this::despawnFor);
        if (players.isEmpty()) {
            discard();
            return;
        }

        for (GamePlayer player : players) {
            // refresh villager in case client is retarded
            if (!respawned && tickCount > 40 && player.getActivityData().wasMoving(1)) {
                respawned = true;

                player.sendPacket(new ClientboundAddEntityPacket(this));
                player.sendPacket(new ClientboundSetEntityDataPacket(getId(), getEntityData().getNonDefaultValues()));
            }

            if (tickCount % 20 == 0) {
                if (particlesFunction != null && particlesFunction.apply(player)) {
                    player.sendPacket(particlesPacket);
                }
            }

            if (looking) {
                Location pLoc = player.getPlayer().getLocation();
                if (this.distanceToSqr(pLoc.getX(), pLoc.getY(), pLoc.getZ()) <= 25) {
                    lookTowards(player);
                } else if (tickCount % 60 == 0) {
                    int i = Mth.floor(location.getYaw() * 256.0F / 360.0F);
                    player.sendPacket(new ClientboundRotateHeadPacket(this, (byte) i));
                }
            }
        }
    }

    public void lookTowards(GamePlayer player) {
        Location pLoc = player.getPlayer().getLocation();
        Vector dir = pLoc.toVector().subtract(new Vector(getX(), getY(), getZ())).normalize();

        double theta = Math.atan2(-dir.getX(), dir.getZ());
        float yaw = (float) Math.toDegrees((theta + 6.283185307179586D) % 6.283185307179586D);
        int i = Mth.floor(yaw * 256.0F / 360.0F);
        player.sendPacket(new ClientboundRotateHeadPacket(this, (byte) i));
    }

    @Override
    public boolean hurt(DamageSource damagesource, float damage) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public void despawnFor(GamePlayer player) {
        if (!players.contains(player))
            return;
        players.remove(player);

        if (!player.isOffline()) {
            player.sendPacket(new ClientboundRemoveEntitiesPacket(getId()));
            player.getAllowedEntities().remove(getId());
        }
    }
}
