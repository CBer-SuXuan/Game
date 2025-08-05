package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BlazingWheel extends Powerup {
    private final Set<ArmorStandUtil> stands = new HashSet<>();

    public BlazingWheel(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public double getPeriod() {
        return 7;
    }

    @Override
    public void onReset(boolean premature) {
        stands.forEach(ArmorStandUtil::removeAll);
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 5 == 0 && stands.size() < Math.min(getLevel() * 2, 8)) {
            Pair<Block, BlockFace> pair = getRandomBlockRelative();
            if (pair == null) return;
            Block block = pair.key();
            Block target = block.getRelative(pair.value().getOppositeFace());

            BlockFace face = pair.value();
            if (face.equals(BlockFace.UP) || face.equals(BlockFace.SELF)) {
                if (face.equals(BlockFace.UP)) {
                    block = block.getRelative(BlockFace.DOWN);
                }
                face = BlockFace.NORTH;
            }
            Location location = block.getLocation().add(0.5, -1, 0.5).add(face.getDirection().multiply(-0.5));

            boolean forward = face.equals(BlockFace.NORTH) || face.equals(BlockFace.SOUTH);
            Vector dir = new Vector(0, 1, 0);
            Vector axis = VectorUtil.rotateOnVector(dir, face.getDirection(), Math.PI / 2);

            AtomicReference<ArmorStandUtil> build = new AtomicReference<>();
            build.set(ArmorStandUtil.builder()
                    .small(true)
                    .viewers(getPlayers())
                    .location(location)
                    .head(new ItemStack(Material.BLAZE_ROD))
                    .tickConsumer(new Consumer<>() {
                        Location origin = location.clone();
                        ArmorStand main = null;
                        int ticks = 0;

                        @Override
                        public void accept(ArmorStand stand) {
                            // need to make sure to only tick once as the tick consumer is called for every stand
                            if (main == null || main.equals(stand)) {
                                main = stand;
                                if (location.distanceSquared(origin) >= 1) {
                                    VectorUtil.rotateOnVector(axis, dir, -Math.PI / 2);
                                    origin = location.clone();
                                }
                                location.add(dir.clone().multiply(0.1));

                                if (ticks % 5 == 0) {
                                    // check if the block is still there
                                    if (!getBlocks().contains(target)) {
                                        build.get().removeAll();
                                        return;
                                    }

                                    ParticlesUtil.send(ParticleTypes.FLAME, location.clone().add(0, 1, 0), Triple.of(0.1F, 0.1F, 0.1F), 1, getPlayers());
                                }
                                if (ticks % 40 == 0) {
                                    dropItem(location.clone().add(0, 1, 0), 1);
                                }
                                ticks++;
                            }

                            float rotation = (forward ? stand.headPose.getY() : stand.headPose.getZ()) + 18;
                            stand.setHeadPose(new Rotations(forward ? 90 : 0, forward ? rotation : 0, forward ? 90 : rotation));
                            stand.moveTo(location.getX(), location.getY(), location.getZ());
                        }
                    })
                    .build()
            );

            for (int i = 0; i < 8; i++) {
                ArmorStand stand = build.get().spawn();
                int offset = 45 * i;
                stand.setHeadPose(new Rotations(forward ? 90 : 0, forward ? offset : 0, forward ? 90 : offset));
            }

            stands.add(build.get());
        }
    }
}
