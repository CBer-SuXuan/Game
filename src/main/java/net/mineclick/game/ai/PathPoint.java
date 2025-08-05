package net.mineclick.game.ai;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.mineclick.global.util.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PathPoint {
    private static final int BLUR_RADIUS = 2;
    private final Block block;
    private final int height;
    @EqualsAndHashCode.Include
    private final Location location;
    private int gCost; //Distance from starting node
    private int hCost; //Distance from end node (heuristic)
    private int penalty;
    private PathPoint parent;

    public PathPoint(Block block, int height) {
        this.block = block;
        this.height = height;
        location = block.getLocation().add(0.5, 0, 0.5);
    }

    public int getFCost() {
        return gCost + hCost;
    }

    public Set<PathPoint> getNeighbours() {
        Set<Pair<Integer, Integer>> acceptable = new HashSet<>();
        Set<PathPoint> points = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            if (i != 4 && i != 5) { //Exclude up and down
                BlockFace face = BlockFace.values()[i];
                Block b = block.getRelative(face);
                b = Pathfinder.getWalkableBlock(b, 2, 1, height);
                if (b != null) {
                    // Make sure we can jump this high
                    Block bDown = b.getRelative(BlockFace.DOWN);
                    if (bDown.getBoundingBox().getMaxY() - block.getRelative(BlockFace.DOWN).getBoundingBox().getMaxY() > 1) {
                        continue;
                    }

                    // Check for non jumpable blocks
                    if (!Pathfinder.canJump(bDown)) {
                        continue;
                    }

                    // Make we don't walk on water/lava
                    Material typeBelow = b.getRelative(BlockFace.DOWN).getType();
                    if (typeBelow.equals(Material.WATER) || typeBelow.equals(Material.LAVA)) {
                        continue;
                    }

                    if (i > 5) {
                        // Make sure if walking diagonally there are acceptable blocks on adjacent faces
                        if (!acceptable.contains(Pair.of(face.getModX(), 0)) && !acceptable.contains(Pair.of(0, face.getModZ()))) {
                            continue;
                        }
                    } else {
                        acceptable.add(Pair.of(face.getModX(), face.getModZ()));
                    }
                    PathPoint point = new PathPoint(b, height);

                    //Calculate penalty
                    int penalty = Math.max(0, b.getY() - block.getY()) * 5;
                    int closest = BLUR_RADIUS + 1;
                    for (int x = -BLUR_RADIUS; x <= BLUR_RADIUS; x++) {
                        for (int z = -BLUR_RADIUS; z <= BLUR_RADIUS; z++) {
                            if (closest == 1)
                                break;
                            if (x == 0 && z == 0)
                                continue;
                            if (Pathfinder.getWalkableBlock(b.getRelative(x, 0, z), 0, 0, height) == null) {
                                int d = Math.max(Math.abs(x), Math.abs(z));
                                if (d < closest) {
                                    closest = d;
                                }
                            }
                        }
                    }
                    if (closest < BLUR_RADIUS + 1) {
                        penalty += BLUR_RADIUS * 5 / closest;
                    }
                    point.setPenalty(penalty);
                    points.add(point);
                }
            }
        }

        return points;
    }
}
