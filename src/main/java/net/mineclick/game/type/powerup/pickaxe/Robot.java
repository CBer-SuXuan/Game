package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.ai.BodyPart;
import net.mineclick.game.ai.movables.LookAtTarget;
import net.mineclick.game.ai.movables.Oscillate;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.*;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Robot extends Powerup {
    public static final String HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGVkYzFjNzc2ZDRhZWFmYjc1Y2I4YjkzOGFmODllMjA5MDJkODY4NGI3NDJjNmE4Y2M3Y2E5MjE5N2FiN2IifX19";
    private static final Color COLOR = Color.fromRGB(52, 52, 52);
    private final Set<PowerupRobot> robots = new HashSet<>();

    public Robot(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public double getPeriod() {
        return 7;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 10 == 0 && robots.size() < Math.min(getLevel(), 3)) {
            spawnRobot();
        }

        if (ticks % 5 == 0) {
            robots.removeIf(PowerupRobot::check);
        }
    }

    @Override
    public void onReset(boolean premature) {
        robots.forEach(net.mineclick.game.ai.Robot::remove);
    }

    private void spawnRobot() {
        Pair<Location, Location> spawnAndTarget = getRandomSpawnAndTarget();

        if (spawnAndTarget != null) {
            robots.add(new PowerupRobot(this, spawnAndTarget.key(), spawnAndTarget.value(), getPlayer()));
        }
    }

    private static class PowerupRobot extends net.mineclick.game.ai.Robot {
        final Robot powerup;
        private final Location target;

        public PowerupRobot(Robot powerup, Location spawn, Location target, GamePlayer player) {
            super(spawn, player.getCurrentIsland().getAllPlayers());
            this.powerup = powerup;
            this.target = target;
            setSpeed(0.1);
            player.playSound(Sound.BLOCK_CONDUIT_DEACTIVATE, target, 0.5, 2);

            addBodyParts(
                    //Head
                    new BodyPart(this, new Vector(0, -0.4, 0), 0, ArmorStandUtil.builder()
                            .head(ItemBuilder.builder().skull(HEAD).build().toItem())
                            .tickConsumer(new LaserTarget(this, target, player))
                    ).rotateWithRobot(false),
                    //Base
                    new BodyPart(this, new Vector(-0.22, 0, 0), 90, ArmorStandUtil.builder()
                            .rotation(ArmorStandUtil.Part.LEFT_LEG, Triple.of(90D, 0D, 0D))
                            .rotation(ArmorStandUtil.Part.RIGHT_LEG, Triple.of(90D, 0D, 0D))
                            .legs(ItemBuilder.builder().color(COLOR).material(Material.LEATHER_LEGGINGS).build().toItem())
                    ),
                    new BodyPart(this, new Vector(0.22, 0, 0), -90, ArmorStandUtil.builder()
                            .rotation(ArmorStandUtil.Part.LEFT_LEG, Triple.of(90D, 0D, 0D))
                            .rotation(ArmorStandUtil.Part.RIGHT_LEG, Triple.of(90D, 0D, 0D))
                            .legs(ItemBuilder.builder().color(COLOR).material(Material.LEATHER_LEGGINGS).build().toItem())
                    ),
                    //Legs
                    new BodyPart(this, new Vector(0.22, 0, 0), 90, ArmorStandUtil.builder()
                            .rotation(ArmorStandUtil.Part.LEFT_LEG, Triple.of(30D, 60D, 0D))
                            .rotation(ArmorStandUtil.Part.RIGHT_LEG, Triple.of(30D, 300D, 0D))
                            .legs(ItemBuilder.builder().color(COLOR).material(Material.LEATHER_LEGGINGS).build().toItem())
                            .tickConsumer(new Oscillate(this)
                                    .robotSpeed(ArmorStandUtil.Part.LEFT_LEG, ArmorStandUtil.Axis.Y, 0, 60, true, 100)
                                    .robotSpeed(ArmorStandUtil.Part.RIGHT_LEG, ArmorStandUtil.Axis.Y, 300, 360, true, 100)
                            )
                    ),
                    new BodyPart(this, new Vector(-0.22, 0, 0), -90, ArmorStandUtil.builder()
                            .rotation(ArmorStandUtil.Part.LEFT_LEG, Triple.of(30D, 60D, 0D))
                            .rotation(ArmorStandUtil.Part.RIGHT_LEG, Triple.of(30D, 300D, 0D))
                            .legs(ItemBuilder.builder().color(COLOR).material(Material.LEATHER_LEGGINGS).build().toItem())
                            .tickConsumer(new Oscillate(this)
                                    .robotSpeed(ArmorStandUtil.Part.LEFT_LEG, ArmorStandUtil.Axis.Y, 0, 60, true, 100)
                                    .robotSpeed(ArmorStandUtil.Part.RIGHT_LEG, ArmorStandUtil.Axis.Y, 300, 360, true, 100)
                            )
                    ));
        }

        /**
         * @return True is needs to be removed
         */
        private boolean check() {
            boolean toRemove = powerup.getDynamicMineBlockType() != null && !powerup.getPlayer().getDynamicMineBlocks().containsKey(target.getBlock());
            if (toRemove) {
                remove();
            }

            return toRemove;
        }
    }

    private static class LaserTarget extends LookAtTarget {
        private final PowerupRobot robot;
        private final Vector targetVector;
        private final Location target;
        private final World world;
        private final GamePlayer player;
        private Location lastLocation;
        private Vector direction;
        private int ticks = 0;

        public LaserTarget(PowerupRobot robot, Location target, GamePlayer player) {
            super(robot, target);
            this.robot = robot;
            this.targetVector = target.toVector();
            this.target = target;
            this.world = target.getWorld();
            this.player = player;
        }

        @Override
        public void accept(ArmorStand stand) {
            super.accept(stand);

            Location location = stand.getBukkitEntity().getLocation().add(0, 2, 0);
            Vector vector = location.toVector();
            if (!location.equals(lastLocation)) {
                lastLocation = location;
                direction = targetVector
                        .clone()
                        .subtract(vector)
                        .normalize()
                        .multiply(0.5);
            }

            java.awt.Color color = java.awt.Color.RED;
            if (ticks % 10 == 0) {
                player.playSound(Sound.BLOCK_FIRE_AMBIENT, target, 0.3, 2);

                ParticlesUtil.send(ParticleTypes.LAVA, target, Triple.of(0.5F, 0.5F, 0.5F), 1, robot.getPlayers());
                ParticlesUtil.send(ParticleTypes.SMOKE, location, Triple.of(0.1F, 0.1F, 0.1F), 1, robot.getPlayers());
            }

            if (ticks % 20 == 0) {
                robot.powerup.dropItem(target, 1);
            }

            if ((ticks / 10) % 2 == 0) {
                vector.add(direction.clone().multiply(0.5));
            }

            double distance = 99999;
            for (int i = 0; i < 100; i++) {
                vector.add(direction);
                ParticlesUtil.sendColor(vector.toLocation(world), color, robot.getPlayers());

                double d = vector.distanceSquared(targetVector);
                if (d <= distance) {
                    distance = d;
                } else {
                    break;
                }
            }

            ticks++;
        }
    }
}
