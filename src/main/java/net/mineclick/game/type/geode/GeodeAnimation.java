package net.mineclick.game.type.geode;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.global.util.*;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.InventoryType;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
public abstract class GeodeAnimation {
    public static double X = 0.5;
    public static double Y = 0.5;
    private final GeodeCrusher geodeCrusher;
    private final GamePlayer player;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Set<GamePlayer> players;
    private ArmorStandUtil geodeStand;
    @Setter
    private Runnable onComplete;

    public GeodeAnimation(GeodeCrusher geodeCrusher, GamePlayer player) {
        this.geodeCrusher = geodeCrusher;
        this.player = player;
        players = geodeCrusher.getPlayers(player);

        Runner.sync(1, 1, state -> {
            if (state.getTicks() > getPeriod() || !running.get() || player.isOffline()) {
                if (onComplete != null) {
                    onComplete.run();
                }
                if (geodeStand != null) {
                    geodeStand.removeAll();
                }

                onReset();
                state.cancel();
                return;
            }

            // chek for players to remove
            players.removeIf(p -> {
                if (p.equals(player)) return false;

                boolean remove = p.isOffline() || p.isOpeningGeode() || p.getPlayer().getOpenInventory().getTopInventory().getType().equals(InventoryType.CHEST);
                if (remove) {
                    onPlayerRemove(p);
                }
                return remove;
            });

            // tick
            tick(state.getTicks());
        });
    }

    public void spawnGeodeStand(Rarity rarity) {
        geodeStand = ArmorStandUtil.builder()
                .location(geodeCrusher.getBlockLocation().clone().add(0.5, 0.5, 0.5))
                .viewers(Collections.singleton(getPlayer()))
                .marker(true)
                .visible(false)
                .small(true)
                .head(ItemBuilder.builder().skull(rarity.getSkin()).build().toItem())
                .tickConsumer(new Consumer<>() {
                    float dir = 0;

                    @Override
                    public void accept(ArmorStand stand) {
                        if (Game.getRandom().nextInt(5) == 0 && stand.headPose.getZ() == 0) {
                            dir = 4;
                            ParticlesUtil.send(ParticleTypes.CRIT, stand.getBukkitEntity().getLocation().add(0, 1, 0), Triple.of(0.25F, 0.25F, 0.25F), 1, GeodeAnimation.this.getPlayers());
                        }

                        float z = stand.headPose.getZ();
                        if (z == 16) {
                            dir = -4;
                        } else if (z == -16) {
                            dir = 4;
                        }
                        stand.setHeadPose(new Rotations(0, stand.headPose.getY() + 9, stand.headPose.getZ() + dir));
                        if (stand.headPose.getZ() == 0 && dir == 4) {
                            dir = 0;
                        }
                    }
                })
                .build();
        geodeStand.spawn();
    }

    public RandomCollection<Block> getBlocksAround(int radius) {
        int radiusSq = radius * radius;
        int bx = getGeodeCrusher().getBlockLocation().getBlockX();
        int bz = getGeodeCrusher().getBlockLocation().getBlockZ();
        int by = getGeodeCrusher().getBlockLocation().getBlockY() - 1;

        RandomCollection<Block> blocks = new RandomCollection<>();
        for (int x = bx - radius; x <= bx + radius; x++) {
            for (int z = bz - radius; z <= bz + radius; z++) {
                double distance = (bx - x) * (bx - x) + (bz - z) * (bz - z);
                if (distance < radiusSq) {
                    Block block = getGeodeCrusher().getBlockLocation().getWorld().getBlockAt(x, by, z);
                    double change = 1 / (distance == 0 ? 1 : distance);
                    blocks.add(change, block);
                }
            }
        }

        return blocks;
    }

    public abstract int getPeriod();

    public abstract void tick(long ticks);

    protected void onReset() {
    }

    protected void onPlayerRemove(GamePlayer player) {
    }

    public void cancel() {
        running.set(false);
    }

    public Set<GamePlayer> getPlayers() {
        return players;
    }
}
