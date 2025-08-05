package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class BlackHole extends Powerup {
    private final Set<ArmorStandUtil> stands = new HashSet<>();

    public BlackHole(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public double getPeriod() {
        return 7;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 5 == 0 && stands.size() < Math.min(getLevel() * 5, 20)) {
            Block block = getRandomBlock();
            if (block == null) return;

            Location location = block.getLocation().add(0.25 - Game.getRandom().nextDouble() * 0.5, -1 + Game.getRandom().nextDouble() * 0.5, 0.25 - Game.getRandom().nextDouble() * 0.5);
            ArmorStandUtil armorStand = ArmorStandUtil.builder()
                    .viewers(getPlayers())
                    .location(location)
                    .small(true)
                    .head(new ItemStack(getItemMaterial()))
                    .tickConsumer(new Consumer<>() {
                        Vector dir = new Vector();
                        Vector rotation = null;

                        @Override
                        public void accept(ArmorStand stand) {
                            Location playerLoc = getPlayer().getPlayer().getLocation();
                            if (location.distanceSquared(playerLoc) <= 1) {
                                stand.discard();
                                getPlayer().popSound();
                                ParticlesUtil.send(ParticleTypes.CRIT, stand.getBukkitEntity().getLocation().add(0, 1, 0), Triple.of(0.2F, 0.2F, 0.2F), 5, getPlayers());
                                return;
                            }

                            if (rotation == null || stand.tickCount % 10 == 0) {
                                dir = playerLoc.toVector()
                                        .subtract(location.toVector())
                                        .normalize();
                                if (rotation == null) {
                                    rotation = VectorUtil.getPerpendicularTo(dir, true).multiply(0.5);
                                }
                            }
                            location.add(dir.clone().multiply(0.15));

                            VectorUtil.rotateOnVector(dir, rotation, Math.PI / 20);
                            Location offset = location.clone().add(rotation);
                            stand.moveTo(offset.getX(), offset.getY(), offset.getZ());
                        }
                    })
                    .build();

            armorStand.spawn();
            stands.add(armorStand);
        }

        stands.removeIf(ArmorStandUtil::allRemoved);
    }

    @Override
    public void onReset(boolean premature) {
        if (premature) {
            stands.forEach(ArmorStandUtil::removeAll);
        }
    }
}
