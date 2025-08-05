package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.util.Vector;

public class Bees extends Powerup {
    public Bees(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public double getPeriod() {
        return 6;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 5 == 0 && getEntities().size() < Math.min(getLevel() * 3, 12)) {
            Pair<Location, Location> spawnAndTarget = getRandomSpawnAndTarget();
            if (spawnAndTarget == null) return;

            Location spawn = spawnAndTarget.key().add(0, 2, 0);
            Location target = spawnAndTarget.value();
            Bee bee = new Bee(EntityType.BEE, ((CraftWorld) spawn.getWorld()).getHandle()) {
                double distance = 99999;
                Vector dir = null;

                @Override
                public void tick() {
                    Location location = getBukkitEntity().getLocation();
                    double d = target.distanceSquared(location);
                    if (d < distance) {
                        distance = d;
                    } else {
                        removeEntity(this);
                        return;
                    }

                    if (dir == null || tickCount % 20 == 0) {
                        dir = target.toVector().subtract(location.toVector()).normalize();

                        if (Game.getRandom().nextBoolean()) {
                            getPlayer().playSound(Sound.ENTITY_BEE_LOOP, location, 0.5, 1);
                        }
                        dropItem(location, 1);
                        ParticlesUtil.send(ParticleTypes.FALLING_NECTAR, location, Triple.of(0.25F, 0.25F, 0.25F), 5, getPlayers());
                    }

                    location.setDirection(dir);
                    location.add(dir.clone().multiply(0.1));
                    moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

                    super.tick();
                }

                @Override
                protected void customServerAiStep() {
                }

                @Override
                public void aiStep() {
                }

                @Override
                public void move(MoverType enummovetype, Vec3 vec3d) {
                }
            };
            bee.getBukkitEntity().teleport(spawn);
            bee.setBaby(true);

            addEntity(bee);
        }
    }
}
