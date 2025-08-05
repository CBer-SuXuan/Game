package net.mineclick.game.gadget.christmas;

import com.google.common.collect.EvictingQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.util.*;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class MagicWand extends Gadget {
    private static final double ROT_BIG_ANGLE = Math.PI / 15;
    private static final double ROT_SMALL_ANGLE = Math.PI / 3;
    private static final double ROT_BIG_RADIUS = 0.8;
    private static final double ROT_SMALL_RADIUS = 0.25;
    private static final int QUEUE_SIZE = 30;
    private static final double SPEED = 0.8;
    private static final int RESOLUTION = 5;

    @Override
    public void run(GamePlayer player, Action action) {
        player.playSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.8);

        Location location = player.getPlayer().getEyeLocation().add(0.5, 0, 0.5);
        Vector dir = location.getDirection();
        Vector rotBig = VectorUtil.getPerpendicularTo(dir, true);
        Vector rotSmall = rotBig.clone();
        rotBig.multiply(ROT_BIG_RADIUS);
        rotSmall.multiply(ROT_SMALL_RADIUS);

        location.add(dir.clone().multiply(2));
        Vector moveDir = dir.clone().multiply(SPEED / RESOLUTION);

        EvictingQueue<Pair<Location, Color>> particles = EvictingQueue.create(QUEUE_SIZE * RESOLUTION);
        Set<Flare> flares = new HashSet<>();
        Runner.sync(0, 1, new java.util.function.Consumer<Runner.State>() {
            final boolean rainbow = Game.getRandom().nextInt(5) == 0;
            Color color = getRandomColor();
            float hue = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];

            @Override
            public void accept(Runner.State state) {
                if (state.getTicks() > 100) {
                    state.cancel();
                    return;
                }

                if (!location.getBlock().getType().equals(Material.AIR)) {
                    MagicWand.this.explode(color, rainbow, location);
                    state.cancel();
                    return;
                }

                //Main swirls
                for (int i = 0; i < RESOLUTION; i++) {
                    VectorUtil.rotateOnVector(dir, rotBig, ROT_BIG_ANGLE / RESOLUTION);
                    VectorUtil.rotateOnVector(dir, rotSmall, ROT_SMALL_ANGLE / (double) RESOLUTION);

                    location.add(moveDir);

                    if (rainbow) {
                        hue += 0.05f / RESOLUTION;
                        if (hue > 1)
                            hue = 0;
                        color = Color.getHSBColor(hue, 1, 1);
                    }
                    particles.add(Pair.of(location.clone().add(rotBig).add(rotSmall), new Color(color.getRGB())));
                }
                for (Pair<Location, Color> pair : particles) {
                    sendParticle(pair.key(), pair.value());
                }

                //Occasional flare
                flares.removeIf(Flare::isDone);
                flares.forEach(f -> MagicWand.this.sendParticle(f.tick(), f.color));
                if (Game.getRandom().nextInt(4) == 0) {
                    Vector v = rotSmall.clone();
                    VectorUtil.rotateOnVector(dir, v, Game.getRandom().nextDouble() * Math.PI * 2);
                    double y = Math.abs(v.getY()) / 2;
                    v.setY(0).normalize().multiply(0.1).setY(y);
                    flares.add(new Flare(location.clone(), v, new Color(color.getRGB())));
                }
            }
        });
    }

    public void explode(Color color, boolean rainbow, Location location) {
        Objects.requireNonNull(location.getWorld()).playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1);
        ParticlesUtil.send(ParticleTypes.CLOUD, location, Triple.of(0.5f, 0.5f, 0.5f), 15, getPlayersInLobby(location));

        Set<Spiral> spirals = new HashSet<>();
        int max = 5 + Game.getRandom().nextInt(5);
        for (int i = 0; i < max; i++) {
            spirals.add(new Spiral(location.clone(), rainbow ? getRandomColor() : color));
        }

        Runner.sync(0, 1, state -> {
            spirals.removeIf(spiral -> {
                if (spiral.isDone()) {
                    ParticlesUtil.send(ParticleTypes.CLOUD, spiral.location, Triple.of(0.2f, 0.2f, 0.2f), 3, getPlayersInLobby(spiral.location));
                    return true;
                }
                return false;
            });

            if (spirals.isEmpty()) {
                state.cancel();
                return;
            }

            for (Spiral spiral : spirals) {
                spiral.tick(l -> ParticlesUtil.sendColor(l, spiral.color.getRed(), spiral.color.getGreen(), spiral.color.getBlue(), getPlayersInLobby(spiral.location)));
            }
        });
    }

    private Color getRandomColor() {
        return Color.getHSBColor(Game.getRandom().nextFloat(), 1, 1);
    }

    private void sendParticle(Location loc, Color color) {
        ParticlesUtil.sendColor(loc, color.getRed(), color.getGreen(), color.getBlue(), getPlayersInLobby(loc));
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().material(Material.BLAZE_ROD);
    }

    @Override
    public String getImmutableName() {
        return "magicWand";
    }

    @Override
    public String getName() {
        return "Magic Wand";
    }

    @Override
    public String getDescription() {
        return "Show off your magic skills";
    }

    @Override
    public int getCooldown() {
        return 2;
    }

    @RequiredArgsConstructor
    public static class Flare {
        private final static double ACC = -0.01;
        private final Location location;
        private final Vector dir;
        @Getter
        private final Color color;
        private int lifeTicks = 30;

        public Location tick() {
            if (!isDone()) {
                double y = Math.max(dir.getY() + ACC, -0.1);
                dir.multiply(0.95);
                dir.setY(y);

                location.add(dir);

                lifeTicks--;
            }
            return location;
        }

        public boolean isDone() {
            return lifeTicks <= 0;
        }
    }

    static class Spiral {
        private static final int RESOLUTION = 10;
        private final Location location;
        private final Vector dir;
        private final Vector axis;
        private final Color color;
        private final Vector rot;
        private double radius = 0.5 + Game.getRandom().nextDouble() / 2;

        Spiral(Location location, Color color) {
            this.location = location;
            this.color = color;
            Random r = Game.getRandom();
            axis = new Vector(r.nextDouble() - 0.5, r.nextDouble() * 0.5, r.nextDouble() - 0.5).normalize();
            dir = axis.clone().multiply(0.3 / RESOLUTION);
            rot = VectorUtil.getPerpendicularTo(axis, true);
        }

        void tick(Consumer<Location> consumer) {
            if (!isDone()) {
                for (int i = 0; i < RESOLUTION; i++) {
                    radius -= 0.03 / RESOLUTION;
                    location.add(dir);
                    VectorUtil.rotateOnVector(axis, rot, Math.PI / RESOLUTION);
                    consumer.accept(location.clone().add(rot.clone().multiply(radius)));
                }
            }
        }

        boolean isDone() {
            return radius <= 0.01;
        }
    }
}
