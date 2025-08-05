package net.mineclick.game.type.powerup.pickaxe;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Lightning extends Powerup {
    private final Set<Rod> rods = new HashSet<>();
    private World world = null;

    public Lightning(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public double getPeriod() {
        return 10;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 10 == 0 && rods.size() < Math.min(getLevel(), 3)) {
            spawnRod();
        }

        rods.removeIf(Rod::tick);
    }

    private void spawnRod() {
        Pair<Location, Location> spawnAndTarget = getRandomSpawnAndTarget();
        if (spawnAndTarget != null) {
            Location spawn = spawnAndTarget.key();
            if (world == null) {
                world = spawn.getWorld();
            }

            rods.add(new Rod(spawn.clone().add(0, 4, 0), spawnAndTarget.value()));
        }
    }

    @RequiredArgsConstructor
    private class Rod {
        private final Location spawn;
        private final Location target;
        private final List<Location> particles = new ArrayList<>();
        private Vector dir;
        private int ticks = 0;

        /**
         * @return True if should be removed
         */
        private boolean tick() {
            if (ticks % 5 == 0) {
                if (getDynamicMineBlockType() != null && !getPlayer().getDynamicMineBlocks().containsKey(target.getBlock())) {
                    return true;
                }
            }

            Vector spawnVector = spawn.toVector();

            if (dir == null) {
                dir = target.toVector().subtract(spawnVector).normalize();
            }
            if (ticks % 40 == 0) {
                particles.clear();
                List<Vector> vectors = new ArrayList<>();
                recBolt(spawnVector, target.toVector(), dir, vectors, 2);

                if (vectors.isEmpty()) return false;
                Vector previous = spawnVector;
                for (Vector vector : vectors) {
                    Vector dir = vector.clone().subtract(previous);
                    double length = dir.length();
                    dir.setX(dir.getX() / length * 0.3);
                    dir.setY(dir.getY() / length * 0.3);
                    dir.setZ(dir.getZ() / length * 0.3);

                    Location loc = previous.toLocation(world);
                    for (int j = 0; j < length / 0.3; j++) {
                        particles.add(loc.add(dir).clone());
                    }

                    previous = vector;
                }
            }

            if (ticks % 10 == 0) {
                dropItem(target, 1);
            }

            ParticlesUtil.send(ParticleTypes.LARGE_SMOKE, spawn, Triple.of(0.5F, 0.5F, 0.5F), 5, getPlayers());
            particles.forEach(loc -> ParticlesUtil.sendColor(loc, Color.WHITE, getPlayers()));
            ticks++;

            return false;
        }

        private void recBolt(Vector start, Vector end, Vector dir, List<Vector> bolt, double variance) {
            if (variance > 0.5) {
                Vector var = VectorUtil.getPerpendicularTo(dir, true)
                        .multiply(Game.getRandom().nextDouble() * variance * 2 - variance);
                Vector mid = start.getMidpoint(end).add(var);

                recBolt(start, mid, dir, bolt, variance * 0.55);
                recBolt(mid, end, dir, bolt, variance * 0.55);
            } else {
                bolt.add(end);
            }
        }
    }
}
