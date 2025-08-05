package net.mineclick.game.model.worker.goal;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.ai.PathPoint;
import net.mineclick.game.ai.Pathfinder;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.model.worker.EntityWorker;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.service.SkillsService;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.util.visual.FollowingItem;
import net.mineclick.global.GlobalPlugin;
import net.mineclick.global.config.field.MineRegionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;

import java.awt.*;
import java.util.List;
import java.util.Set;

@Getter
public class MineGoal extends WorkerGoal {
    private static final Set<BlockFace> LOOKUP_FACES = ImmutableSet.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.SELF
    );
    private final Color color = new Color(Game.getRandom().nextInt(255), GlobalPlugin.random.nextInt(255), GlobalPlugin.random.nextInt(255));
    private MineRegionConfig mineRegion;
    private Location walkLocation;
    private Location mineLocation;
    private Location walkBackLocation;
    private boolean gathered;
    private boolean ended;
    private int notWalkingTicks = 0;
    private Location lastLocation;
    private int gatheringTicks = 0;
    private int ticksToGather = 0;
    private FollowingItem followingItem;
    private List<PathPoint> pathPoints;
    private int currentPointIndex;
    private int pathFindingsDone = 0;

    public MineGoal(EntityWorker worker) {
        super(worker);
    }

    @Override
    public void start() {
        IslandModel island = getWorker().getIsland();
        if (island != island.getPlayer().getCurrentIsland(false)) {
            ended = true;
            return;
        }

        walkBackLocation = island.getRandomNpcSpawn();
        mineRegion = island.getRandomMineRegion();
        mineLocation = mineRegion.getRandomAccessibleBlock(getWorker());
        lastLocation = getWorker().getLocation();

        walkLocation = mineLocation;

        ended = mineLocation == null;
        notWalkingTicks = gatheringTicks = 0;
        gathered = false;
        ticksToGather = 30 + Game.getRandom().nextInt(30);
    }

    @Override
    public void tick(int ticks) {
        if (ended)
            return;

        if (lastLocation.getBlockX() == getWorker().getLocation().getBlockX() && lastLocation.getBlockZ() == getWorker().getLocation().getBlockZ()) {
            if ((gathered || gatheringTicks == 0)) {
                if (notWalkingTicks++ > 50) {
                    Location loc;
                    if (pathPoints != null && currentPointIndex < pathPoints.size()) {
                        loc = pathPoints.get(currentPointIndex).getLocation();
                    } else {
                        loc = walkLocation;
                    }

                    getWorker().moveTo(loc.getX(), loc.getY(), loc.getZ());
                    notWalkingTicks = 0;
                } else if (notWalkingTicks % 20 == 0) {
                    getWorker().getJumpControl().jump();
                }
            }
        } else {
            lastLocation = getWorker().getLocation();
            notWalkingTicks = 0;
        }

        if (walkLocation != null) {
            if (!walkLocation.getBlock().getType().equals(Material.AIR))
                walkLocation.add(0, 1, 0);

            if (distanceSquared(getWorker().getLocation(), walkLocation, true) <= 2) {
                pathPoints = null;

                if (!gathered) {
                    if (walkLocation != mineLocation) {
                        walkLocation = mineLocation;
                        return;
                    }

                    if (gatheringTicks == 0) {
                        startMining();
                    }

                    if (gatheringTicks++ > ticksToGather) {
                        gathered = true;
                        mine();
                    }
                } else {
                    if (walkLocation != walkBackLocation) {
                        walkLocation = walkBackLocation;
                        return;
                    }

                    reset();
                }
            } else {
                if (pathPoints == null) {
                    pathPoints = new Pathfinder().findPath(getWorker().getLocation(), walkLocation, (int) Math.ceil(getWorker().getBbHeight()));
                    currentPointIndex = 0;
                }

//                GamePlayer player = getWorker().getWorker().getPlayer();
//                if (player.getRank().isAtLeast(Rank.DEV)) {
//                    pathPoints.forEach(pathPoint -> ParticlesUtil.sendColor(pathPoint.getLocation(), color, player));
//                    for (int i = 0; i < 10; i++) {
//                        ParticlesUtil.sendColor(walkLocation.clone().add(0, 0.1 * i, 0), color, player);
//                    }
//                }

                if (currentPointIndex < pathPoints.size()) {
                    PathPoint currentPoint = pathPoints.get(currentPointIndex);
                    Location location = currentPoint.getLocation();

                    BlockPos block = new BlockPos(currentPoint.getBlock().getX(), currentPoint.getBlock().getY(), currentPoint.getBlock().getZ());
                    getWorker().getMoveControl().setWantedPosition(location.getX(), getWorker().level().getBlockState(block.below()).isAir() ? location.getY() : WalkNodeEvaluator.getFloorLevel(getWorker().level(), block), location.getZ(), getWalkSpeed());

                    Location workerLocation = getWorker().getLocation();
                    if (distanceSquared(workerLocation, location, true) <= 0.5) {
                        currentPointIndex++;
                    }
                } else if (++pathFindingsDone < 5) {
                    pathPoints = null;
                }

                walkingTowards(walkLocation);
            }
        }
    }

    protected double getWalkSpeed() {
        Worker worker = getWorker().getWorker();
        return worker.isExcited() ? (SkillsService.i().has(worker.getPlayer(), SkillType.COOKIE_3) ? 2 : 1.5) : 1.2;
    }

    protected void walkingTowards(Location loc) {
    }

    protected void startMining() {
    }

    protected void mine() {
        if (getWorker().isFollowingItemEnabled()) {
            if (followingItem != null)
                followingItem.discard();
            followingItem = new FollowingItem(getWorker(), getMineRegion().getItemMaterial(), getWorker().getWorker().getPlayer());
        }
        getWorker().setItemInHand(InteractionHand.MAIN_HAND, CraftItemStack.asNMSCopy(new ItemStack(mineRegion.getItemMaterial())));
    }

    @Override
    public void reset() {
        super.reset();
        pathPoints = null;
        ended = true;
        pathFindingsDone = 0;

        getWorker().setItemInHand(InteractionHand.MAIN_HAND, CraftItemStack.asNMSCopy(new ItemStack(Material.AIR)));
        if (followingItem != null) {
            followingItem.discard();
        }
    }

    @Override
    public boolean hasEnded() {
        return ended;
    }

    private double distanceSquared(Location from, Location to, boolean ignoreY) {
        double x = NumberConversions.square(from.getX() - to.getX());
        double y = ignoreY ? 0 : NumberConversions.square(from.getY() - to.getY());
        double z = NumberConversions.square(from.getZ() - to.getZ());

        return x + y + z;
    }
}
