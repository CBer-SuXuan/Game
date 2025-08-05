package net.mineclick.game.util.visual;

import com.google.common.base.Preconditions;
import lombok.Builder;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.util.Runner;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Builder
public class SwingAnimation {

    private static final double[] NORTH = {-0.188, -0.875, 0.438};
    private static final double[] SOUTH = {0.188, -0.875, -0.438};
    private static final double[] EAST = {-0.438, -0.875, -0.188};
    private static final double[] WEST = {0.438, -0.875, 0.188};

    private final ItemStack item;
    private final Location location;
    private final BlockFace face;
    private final double degreeStep;
    @Builder.Default
    private final int swingsToLive = 1;
    private final Consumer<Integer> onSwing;
    private Location spawnLocation;

    public void spawn(Collection<GamePlayer> players) {
        Preconditions.checkNotNull(item);
        Preconditions.checkNotNull(location);
        Preconditions.checkNotNull(location.getWorld());
        Preconditions.checkNotNull(face);

        if (spawnLocation == null) {
            spawnLocation = location;
        }

        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, ((CraftWorld) location.getWorld()).getHandle()) {
            @Override
            public void remove(RemovalReason removalReason) {
                super.remove(removalReason);

                for (GamePlayer player : players) {
                    player.getAllowedEntities().remove(getId());
                }
            }
        };

        stand.setSmall(true);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setNoGravity(true);
        stand.setSilent(true);
        stand.setRightArmPose(new Rotations(270, 0, 0));

        double[] offset;
        double addX = 0;
        double addZ = 0;
        double addY;
        float yaw;
        switch (face) {
            case SOUTH -> {
                offset = SOUTH;
                yaw = 0;
                addX = 0.25 + Game.getRandom().nextDouble() * 0.5;
                addY = 0.25 + Game.getRandom().nextDouble() * 0.5;
                addZ = 1;
            }
            case NORTH -> {
                offset = NORTH;
                yaw = 180;
                addX = 0.25 + Game.getRandom().nextDouble() * 0.5;
                addY = 0.25 + Game.getRandom().nextDouble() * 0.5;
            }
            case EAST -> {
                offset = EAST;
                yaw = -90;
                addZ = 0.25 + Game.getRandom().nextDouble() * 0.5;
                addY = 0.25 + Game.getRandom().nextDouble() * 0.5;
                addX = 1;
            }
            case WEST -> {
                offset = WEST;
                yaw = 90;
                addZ = 0.25 + Game.getRandom().nextDouble() * 0.5;
                addY = 0.25 + Game.getRandom().nextDouble() * 0.5;
            }
            default -> {
                switch (Game.getRandom().nextInt(4)) {
                    case 0 -> {
                        offset = NORTH;
                        yaw = 180;
                    }
                    case 1 -> {
                        offset = SOUTH;
                        yaw = 0;
                    }
                    case 2 -> {
                        offset = EAST;
                        yaw = -90;
                    }
                    default -> {
                        offset = WEST;
                        yaw = 90;
                    }
                }
                addX = 0.25 + Game.getRandom().nextDouble() * 0.5;
                addZ = 0.25 + Game.getRandom().nextDouble() * 0.5;
                addY = 0.3;
            }
        }

        for (GamePlayer player : players) {
            player.getAllowedEntities().add(stand.getId());
        }

        stand.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(item));
        stand.moveTo(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), yaw, 0);
        stand.level().addFreshEntity(stand);

        Location standLoc = location.clone().add(offset[0] + addX, offset[1] + addY, offset[2] + addZ);
        Runner.sync(2, () -> stand.moveTo(standLoc.getX(), standLoc.getY(), standLoc.getZ(), yaw, 0));

        AtomicInteger dir = new AtomicInteger(-1);
        AtomicInteger swings = new AtomicInteger(0);
        Runner.sync(1, 1, (state) -> {
            if (!stand.isAlive()) {
                state.cancel();
                return;
            }

            float degree = stand.rightArmPose.getX();
            if (degree > 270) {
                if (onSwing != null)
                    onSwing.accept(swings.incrementAndGet());
                if (swings.get() >= swingsToLive) {
                    stand.discard();
                    state.cancel();
                    return;
                }

                dir.set(-1);
            } else if (degree < 180)
                dir.set(1);
            stand.setRightArmPose(new Rotations(degree + (float) (degreeStep * dir.get()), 0, 0));
        });
    }
}
