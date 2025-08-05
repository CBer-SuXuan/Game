package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.Game;
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
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Swords extends Powerup {
    private final Set<ArmorStandUtil> stands = new HashSet<>();

    public Swords(PowerupType powerupType, GamePlayer player) {
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
        if (ticks % 3 == 0 && stands.size() <= 10) {
            Pair<Block, BlockFace> pair = getRandomBlockRelative();
            if (pair == null) return;

            Location location = pair.key().getLocation()
                    .add(0.5, 0.5, 0.5)
                    .add(pair.value().getDirection().multiply(0.5));
            Vector dir = pair.value().getOppositeFace().getDirection();
            float yaw = (float) Math.toDegrees((Math.atan2(-dir.getX(), dir.getZ()) + 6.283185307179586D) % 6.283185307179586D);
            VectorUtil.rotateOnVector(new Vector(0, 1, 0), dir, 90);

            location.add(dir.clone().multiply(-0.5));

            dir.multiply(1 / 20D);
            double endYaw = Location.normalizeYaw(yaw - 45);

            // 135 to -45
            double angle = Game.getRandom().nextDouble() * 40 - 20;
            ArmorStandUtil stand = ArmorStandUtil.builder()
                    .head(new ItemStack(Material.DIAMOND_SWORD))
                    .rotation(ArmorStandUtil.Part.HEAD, Triple.of(90D, (double) Location.normalizeYaw(yaw + 135F), angle))
                    .tickConsumer(new Consumer<ArmorStand>() {
                        int ticks = 0;

                        @Override
                        public void accept(ArmorStand armorStand) {
                            float rotation = armorStand.headPose.getY();
                            if (rotation <= endYaw) {
                                armorStand.discard();
                                return;
                            }

                            if (ticks == 15) {
                                getPlayer().playSound(Sound.ENTITY_IRON_GOLEM_ATTACK, location, 0.5, 1);
                                ParticlesUtil.send(ParticleTypes.CRIT, location, Triple.of(0.25F, 0.25F, 0.25F), 5, Swords.this.getPlayers());
                                ParticlesUtil.send(ParticleTypes.SWEEP_ATTACK, location, Triple.of(0F, 0F, 0F), 1, Swords.this.getPlayers());
                                Swords.this.dropItem(pair.key().getLocation(), 1);
                            }

                            // 18
                            armorStand.setHeadPose(new Rotations(armorStand.headPose.getX(), rotation - 9F, armorStand.headPose.getZ()));

                            location.add(dir);
                            armorStand.moveTo(location.getX(), location.getY() - 2, location.getZ());

                            ticks++;
                        }
                    })
                    .location(location.clone().add(0, -2, 0))
                    .viewers(getPlayers())
                    .build();
            stand.spawn();
            stands.add(stand);
        }

        stands.removeIf(ArmorStandUtil::allRemoved);
    }
}
