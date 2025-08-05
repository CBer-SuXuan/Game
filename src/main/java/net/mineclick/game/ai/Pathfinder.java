package net.mineclick.game.ai;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Pathfinder {
    private final static Comparator<PathPoint> COMPARATOR = Comparator.comparingInt(PathPoint::getFCost).thenComparingInt(PathPoint::getHCost);
    private final static Comparator<PathPoint> COMPARATOR_CLOSEST = Comparator.comparingInt(PathPoint::getHCost).thenComparingInt(PathPoint::getFCost);
    private final static Set<Material> WALKABLE_MATERIALS = new HashSet<>(Arrays.asList(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.DEAD_BUSH,
            Material.DANDELION,
            Material.POPPY,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE,
            Material.SUNFLOWER,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY,
            Material.REDSTONE_WIRE,
            Material.LADDER,
            Material.SNOW,
            Material.SUGAR_CANE,
            Material.VINE,
            Material.NETHER_WART_BLOCK,
            Material.POTATOES,
            Material.WHEAT,
            Material.CARROTS
    ));
    private final static Set<Material> NOT_JUMPABLE = new HashSet<>();
    private final static long MAX_TIME = 25;

    static {
        WALKABLE_MATERIALS.addAll(getAllMaterials(s -> s.endsWith("_PLANT")));
        WALKABLE_MATERIALS.addAll(getAllMaterials(s -> s.endsWith("_PRESSURE_PLATE")));
        WALKABLE_MATERIALS.addAll(getAllMaterials(s -> s.endsWith("_TORCH")));
        WALKABLE_MATERIALS.addAll(getAllMaterials(s -> s.endsWith("_TULIP")));
        WALKABLE_MATERIALS.addAll(getAllMaterials(s -> s.endsWith("_BUTTON")));
        WALKABLE_MATERIALS.addAll(getAllMaterials(s -> s.endsWith("_CARPET")));

        NOT_JUMPABLE.addAll(getAllMaterials(s -> s.endsWith("_FENCE")));
        NOT_JUMPABLE.addAll(getAllMaterials(s -> s.endsWith("_FENCE_GATE")));
        NOT_JUMPABLE.addAll(getAllMaterials(s -> s.endsWith("_WALL")));
        NOT_JUMPABLE.addAll(getAllMaterials(s -> s.endsWith("_RAIL")));
        NOT_JUMPABLE.add(Material.RAIL);
        NOT_JUMPABLE.add(Material.WATER);
        NOT_JUMPABLE.add(Material.LAVA);
    }

    private static Set<Material> getAllMaterials(Function<String, Boolean> filter) {
        return Arrays.stream(Material.values())
                .filter(material -> !material.isLegacy())
                .filter(material -> filter.apply(material.name()))
                .collect(Collectors.toSet());
    }

    public static Block getWalkableBlock(Block original, int maxFall, int maxJump, double height) {
        Block block = original.getRelative(BlockFace.UP, maxJump);
        // find the lowest block
        while (canWalkThrough(block.getRelative(BlockFace.DOWN))) {
            if (maxFall >= 0 && block.getY() < (original.getY() - maxFall) || block.getY() <= 0) {
                return null;
            }

            block = block.getRelative(BlockFace.DOWN);
        }

        // check we can fit through
        Block up = block;
        for (int i = 0; i < height; i++) {
            if (!canWalkThrough(up)) {
                return null;
            }
            up = up.getRelative(BlockFace.UP);
        }

        return block;
    }

    private static boolean canWalkThrough(Block block) {
        return WALKABLE_MATERIALS.contains(block.getType());
    }

    public static boolean canJump(Block block) {
        return !NOT_JUMPABLE.contains(block.getType());
    }

    public List<PathPoint> findPath(Location from, Location to, int height) {
        Set<PathPoint> closed = new HashSet<>();
        PriorityQueue<PathPoint> open = new PriorityQueue<>(COMPARATOR);
        Block walkableBlock = getWalkableBlock(from.getBlock(), 2, 1, height);
        if (walkableBlock == null) {
            walkableBlock = from.getBlock();
        }
        PathPoint start = new PathPoint(walkableBlock, height);
        open.add(start);

        Block toBlock = getWalkableBlock(to.getBlock(), 2, 1, height);
        if (toBlock == null) {
            toBlock = to.getBlock();
        }
        PathPoint destination = new PathPoint(toBlock, height);
        start.setHCost(distance(start, destination));

        long time = System.currentTimeMillis();
        PathPoint closest = start;
        while (true) {
            PathPoint current = open.poll();
            if (current == null || System.currentTimeMillis() - time > MAX_TIME) {
                return retrace(closest);
            }

            if (COMPARATOR_CLOSEST.compare(current, closest) < 0) {
                closest = current;
            }
            if (current.equals(destination)) {
                return retrace(current);
            }

            closed.add(current);
            for (PathPoint neighbour : current.getNeighbours()) {
                if (!closed.contains(neighbour)) {
                    int newCost = current.getGCost() + distance(current, neighbour) + neighbour.getPenalty();
                    if (newCost < neighbour.getGCost() || !open.contains(neighbour)) {
                        neighbour.setGCost(newCost);
                        neighbour.setHCost(distance(neighbour, destination));
                        neighbour.setParent(current);

                        open.remove(neighbour);
                        open.add(neighbour);
                    }
                }
            }
        }
    }

    private List<PathPoint> retrace(PathPoint current) {
        List<PathPoint> list = new ArrayList<>();
        while (current != null) {
            list.add(current);
            current = current.getParent();
        }
        return Lists.reverse(list);
    }

    private int distance(PathPoint from, PathPoint to) {
        int x = Math.abs(from.getBlock().getX() - to.getBlock().getX());
        int y = Math.abs(from.getBlock().getY() - to.getBlock().getY());
        int z = Math.abs(from.getBlock().getZ() - to.getBlock().getZ());

        if (x > z)
            return (x - z) * 10 + (z * 14) + (y * 10);
        return (z - x) * 10 + (x * 14) + (y * 10);
    }
}
