package net.mineclick.game.ai;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.util.Runner;
import org.bukkit.Location;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Getter
public abstract class Robot {
    private final List<BodyPart> bodyParts = new ArrayList<>();
    private final Set<GamePlayer> players;
    private final Pathfinder pathfinder = new Pathfinder();
    private Location location;
    private boolean dead;
    @Setter
    private double height = 1;
    @Setter
    private double speed = 0.1;
    @Setter
    private Supplier<Location> locationSupplier;
    @Setter
    private Location walkTarget;
    private List<PathPoint> pathPoints;
    private int pathPointIndex;

    public Robot(Location location, Set<GamePlayer> players) {
        this.location = location;
        this.players = players;

        Runner.sync(0, 1, state -> {
            if (dead) {
                state.cancel();
                return;
            }

            if (locationSupplier != null) {
                Location newTarget = locationSupplier.get();
                if (newTarget == null) {
                    pathPointIndex = 0;
                    pathPoints = null;
                    return;
                }

                if (walkTarget == null
                        || newTarget.getX() != walkTarget.getX()
                        || newTarget.getZ() != walkTarget.getZ()) {
                    walkTarget = newTarget;
                    pathPoints = pathfinder.findPath(location, walkTarget, (int) height);
                    pathPointIndex = 0;
                }
            }

            if (pathPoints != null && pathPointIndex < pathPoints.size()) {
                PathPoint currentPoint = pathPoints.get(pathPointIndex);
                Location loc = currentPoint.getLocation();

                Vector vector = loc.toVector().subtract(location.toVector()).multiply(speed);
                location.add(vector).setDirection(vector.multiply(-1));
                move(location);

                if ((NumberConversions.square(location.getX() - loc.getX()) + NumberConversions.square(location.getZ() - loc.getZ())) <= 1) {
                    pathPointIndex++;
                }
            } else {
                pathPoints = null;
                pathPointIndex = 0;
            }
        });
    }

    public void addBodyParts(BodyPart... parts) {
        bodyParts.addAll(Arrays.asList(parts));

        bodyParts.forEach(BodyPart::move);
    }

    public void remove() {
        dead = true;
        bodyParts.forEach(b -> b.getArmorStand().removeAll());
        bodyParts.clear();
    }

    public void move(Location location) {
        this.location = location;
        bodyParts.forEach(BodyPart::move);
    }

    public Location getHeadLocation() {
        return location.clone().add(0, height, 0);
    }

    public double getSpeed() {
        return pathPoints == null ? 0 : speed;
    }
}
