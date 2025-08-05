package net.mineclick.game.type.geode;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Rare extends GeodeAnimation {
    public static double SPEED_INIT = 0.02;
    public static double SPEED = 0.01;

    private final List<Block> blocks = new ArrayList<>();
    private final List<ArmorStandUtil> stands = new ArrayList<>();
    private final Vector spawn = getGeodeCrusher().getBlockLocation().clone().add(0.5, 1, 0.5).toVector();

    public Rare(GeodeCrusher geodeCrusher, GamePlayer player) {
        super(geodeCrusher, player);
    }

    @Override
    public int getPeriod() {
        return 60;
    }

    @Override
    public void tick(long ticks) {
        if (ticks < 20 && ticks % 2 == 0) {
            Block block = getBlock();

            ArmorStandUtil standUtil = ArmorStandUtil.builder()
                    .viewers(getPlayers())
                    .location(block.getLocation().add(0.5, 0, 0.5))
                    .head(new ItemStack(block.getType()))
                    .small(true)
                    .marker(true)
                    .visible(false)
                    .tickConsumer(new Consumer<>() {
                        Vector vector;
                        double speed = SPEED_INIT;

                        @Override
                        public void accept(ArmorStand stand) {
                            if (vector == null) {
                                vector = new Vector(stand.getX(), stand.getY(), stand.getZ()).subtract(spawn.clone());
                            }

                            if (vector.lengthSquared() > 0.25) {
                                vector.multiply(0.95);
                            }

                            vector.rotateAroundY(speed += SPEED);
                            stand.moveTo(spawn.getX() + vector.getX(), spawn.getY() + vector.getY() - 1.5, spawn.getZ() + vector.getZ());
                        }
                    })
                    .build();
            standUtil.spawn();
            stands.add(standUtil);
        }

        if (ticks == 60) {
            for (ArmorStandUtil standUtil : stands) {
                for (ArmorStand stand : standUtil.getStands()) {
                    ParticlesUtil.sendBlock(stand.getBukkitEntity().getLocation().add(0, 1, 0), Material.DIRT, Triple.of(0.1F, 0.1F, 0.1F), 1, 10, getPlayers());
                }
            }
            Location location = getGeodeCrusher().getBlockLocation().clone().add(0.5, 1, 0.5);
            getPlayer().playSound(Sound.ENTITY_GENERIC_EXPLODE, location, 0.3, 1);
        }
    }

    private Block getBlock() {
        if (blocks.isEmpty()) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                        for (int y = 2; y <= 5; y++) {
                            Block block = getGeodeCrusher().getBlockLocation().getBlock().getRelative(x, y, z);
                            if (block.getType().isSolid()) {
                                blocks.add(block);
                            }
                        }
                    }
                }
            }
        }

        if (blocks.isEmpty()) return getGeodeCrusher().getBlockLocation().getBlock();
        return blocks.get(Game.getRandom().nextInt(blocks.size()));
    }

    @Override
    public void onReset() {
        stands.forEach(ArmorStandUtil::removeAll);
        stands.clear();
    }
}
