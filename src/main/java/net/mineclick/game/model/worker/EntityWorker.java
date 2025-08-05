package net.mineclick.game.model.worker;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.model.worker.goal.MineGoal;
import net.mineclick.game.model.worker.goal.WorkerGoal;
import net.mineclick.game.type.Holiday;
import net.mineclick.global.util.RandomCollection;
import net.mineclick.global.util.Runner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.event.entity.CreatureSpawnEvent;

@Getter
public abstract class EntityWorker extends Mob {
    private final RandomCollection<WorkerGoal> goals = new RandomCollection<>();
    private WorkerGoal currentGoal;
    private IslandModel island;
    private Worker worker;
    private Location spawnLocation;

    @Setter
    private boolean followingItemEnabled;

    public EntityWorker(EntityType<? extends Mob> type, Level world) {
        super(type, world);

        persist = true;
        setSilent(true);

        Runner.sync(100, () -> {
            if (!valid) {
                this.discard();
            }
        });
    }

    public EntityWorker spawn(IslandModel island, Worker worker) {
        AttributeInstance attribute = getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(0.23);
        }

        this.island = island;
        this.worker = worker;

        if (worker.getPlayer().getCurrentIsland(false) != island) {
            Game.i().getLogger().warning(
                    "Tried to spawn an entity worker on an invalid island for player "
                            + worker.getPlayer().getName() + " (" + worker.getPlayer().getUuid() + ")"
            );
            discard();
            return this;
        }

        updateWorker();

        // find a drop location
        spawnLocation = worker.getPlayer().getCurrentIsland().getRandomNpcSpawn();
        moveTo(spawnLocation.getX(), spawnLocation.getY() + 50, spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch());

        goals.clear();
        currentGoal = null;
        loadDefaultGoals();
        loadGoals();
        if (!valid) {
            Runner.sync(() -> level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM));
        }
        island.getAllPlayers().forEach(p -> p.getAllowedEntities().add(getId()));

        return this;
    }

    public void updateWorker() {
        if (Holiday.APRIL_FOOLS.isNow()) {
            setCustomNameVisible(false);
            setCustomName(CraftChatMessage.fromStringOrNull("Dinnerbone"));
        } else {
            setCustomNameVisible(true);
            setCustomName(CraftChatMessage.fromStringOrNull((worker.isExcited() ? ChatColor.GOLD : ChatColor.GREEN) + worker.getConfiguration().getName() + ChatColor.YELLOW + " lvl " + worker.getLevel()));
        }
    }

    @Override
    public void remove(RemovalReason removalReason) {
        super.remove(removalReason);

        if (island != null && island.getPlayer() != null) {
            island.getAllPlayers().forEach(p -> p.getAllowedEntities().remove(getId()));
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public abstract void loadGoals();

    private void loadDefaultGoals() {
        goals.add(0.8, createMineGoal());
    }

    protected MineGoal createMineGoal() {
        return new MineGoal(this);
    }

    @Override
    public void baseTick() {
        if (spawnLocation != null) {
            moveTo(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch());
            spawnLocation = null;
        }

        super.activatedTick = MinecraftServer.currentTick + 20;

        if (!worker.getPlayer().isOnOwnIsland() || worker.getPlayer().getActivityData().isAfk()) {
            return;
        }

        super.baseTick();

        if (!goals.values().isEmpty() && (currentGoal == null || currentGoal.hasEnded())) {
            currentGoal = goals.next();
            currentGoal.start();
        }

        if (currentGoal != null) {
            currentGoal.tick();
        }
    }

    @Override
    public boolean hurt(DamageSource damagesource, float damage) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player entityhuman, InteractionHand enumhand) {
        GamePlayer player = worker.getPlayer();

        if (worker.getExcitedTicks() <= 0
                && player.getCookies() > 0
                && entityhuman.getUUID().equals(player.getUuid())
                && player.getPlayer().getInventory().getItemInMainHand().getType().equals(org.bukkit.Material.COOKIE)
        ) {
            player.setCookies(player.getCookies() - 1);
            player.updateCookiesItem();

            worker.giveCookie();
        }

        return InteractionResult.PASS;
    }


    @Override
    public void absMoveTo(double d0, double d1, double d2) {
        //No collision
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public Location getLocation() {
        return new Location(level().getWorld(), getX(), getY(), getZ(), getYRot(), getXRot());
    }

    @Override
    protected void playStepSound(BlockPos blockposition, BlockState iblockdata) {
        SoundType soundeffecttype = iblockdata.getSoundType();

        ClientboundSoundPacket packet = new ClientboundSoundPacket(Holder.direct(soundeffecttype.getStepSound()), getSoundSource(), getX(), getY(), getZ(), soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch(), 0);
        island.getAllPlayers().forEach(p -> p.sendPacket(packet));
    }
}
