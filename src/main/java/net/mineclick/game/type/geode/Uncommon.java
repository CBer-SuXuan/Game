package net.mineclick.game.type.geode;

import lombok.Data;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Uncommon extends GeodeAnimation {
    public static double RADIUS_INIT = 2;
    public static double ANGLE_INIT = 0.05;
    public static double ANGLE_STEP = 0.015;
    public static Color COLOR = new Color(0, 47, 75);

    private final List<Stream> streams = new ArrayList<>();

    public Uncommon(GeodeCrusher geodeCrusher, GamePlayer player) {
        super(geodeCrusher, player);
    }

    @Override
    public int getPeriod() {
        return 30;
    }

    @Override
    public void tick(long ticks) {
        if (ticks == 0) {
            streams.add(new Stream(new Vector(1, 0, 0)));
            streams.add(new Stream(new Vector(-1, 0, 0)));
            streams.add(new Stream(new Vector(0, 0, 1)));
            streams.add(new Stream(new Vector(0, 0, -1)));
        }

        if (ticks == 30) {
            Location loc = getGeodeCrusher().getBlockLocation().clone().add(0.5, 1, 0.5);
            getPlayer().playSound(Sound.ENTITY_DOLPHIN_SWIM, loc, 1, 1);
            ParticlesUtil.send(ParticleTypes.NAUTILUS, loc, Triple.of(0.5F, 0.5F, 0.5F), 30, getPlayers());
        }

        streams.forEach(Stream::tick);
    }

    @Data
    private class Stream {
        private final List<Location> locations = new ArrayList<>();
        private final Vector rotationVector;
        private double rotationAngle = ANGLE_INIT;
        private double radius = RADIUS_INIT;
        private double y = 0;

        public void tick() {
            Location location = getGeodeCrusher().getBlockLocation().clone()
                    .add(0.5, y += 0.05, 0.5)
                    .add(rotationVector.rotateAroundY(rotationAngle += ANGLE_STEP).clone().multiply(radius -= RADIUS_INIT / 30D));

            if (locations.size() > 10) {
                locations.remove(0);
            }
            locations.add(location);

            for (Location loc : locations) {
                ParticlesUtil.sendColor(loc, COLOR, getPlayers());
            }
        }
    }
}
