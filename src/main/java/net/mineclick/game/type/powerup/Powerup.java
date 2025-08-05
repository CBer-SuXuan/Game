package net.mineclick.game.type.powerup;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.DynamicMineBlock;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.DynamicMineBlocksService;
import net.mineclick.game.service.PowerupService;
import net.mineclick.game.type.DynamicMineBlockType;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.global.config.IslandConfig;
import net.mineclick.global.config.field.MineBlock;
import net.mineclick.global.config.field.MineRegionConfig;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.location.Region;
import net.minecraft.world.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
public abstract class Powerup {
    private final PowerupType powerupType;
    private final GamePlayer player;
    private final Set<Entity> entities = new HashSet<>();
    private final List<Block> blocks = new ArrayList<>();
    private final Map<Runnable, AtomicInteger> scheduledTasks = new HashMap<>();
    private Material itemMaterial;
    private DynamicMineBlockType dynamicMineBlockType;
    private boolean loaded;

    public Powerup(PowerupType powerupType, GamePlayer player) {
        this.powerupType = powerupType;
        this.player = player;
        Location startLocation = player.getPlayer().getLocation();

        if (!loadBlocks()) {
            Runner.sync(() -> onReset(true));
            return;
        }
        loaded = true;

        double period = getPeriod() * 20;
        double maxPeriod = period + 100;
        Runner.sync(1, 1, state -> {
            if (state.getTicks() == 0) {
                onStart();
            }

            if (player.isOffline()
                    || player.getPlayer().getLocation().distanceSquared(startLocation) > 225
                    || state.getTicks() > maxPeriod) {
                state.cancel();
                entities.forEach(Entity::kill);
                Set<Integer> ids = entities.stream().map(Entity::getId).collect(Collectors.toSet());
                getPlayers().forEach(p -> p.getAllowedEntities().removeAll(ids));
                onReset(true);
                return;
            }

            if (state.getTicks() <= period) {
                tick(state.getTicks());
            } else if (state.getTicks() == period + 1) {
                onReset(false);
                return;
            }

            // Scheduled tasks
            Set<Runnable> toRun = new HashSet<>();
            scheduledTasks.entrySet().removeIf(entry -> {
                if (entry.getValue().decrementAndGet() <= 0) {
                    toRun.add(entry.getKey());
                    return true;
                }
                return false;
            });
            toRun.forEach(Runnable::run);
        });
    }

    private boolean loadBlocks() {
        List<Block> targetBlocks = player.getTargetBlocks();
        Block block = null;

        // check if targeted block is a mineable
        if (targetBlocks != null && targetBlocks.size() == 2) {
            IslandConfig islandConfig = player.getCurrentIsland().getConfig();
            block = targetBlocks.get(1);
            Material type = block.getType();

            // check dynamic mine block
            DynamicMineBlock dynamicMineBlock = player.getDynamicMineBlocks().get(block);
            if (dynamicMineBlock != null && dynamicMineBlock.getType().isPowerupAllowed()) {
                itemMaterial = dynamicMineBlock.getMaterial();
                dynamicMineBlockType = dynamicMineBlock.getType();
            } else {
                // check global mine blocks
                for (MineBlock mineBlock : islandConfig.getGlobalMineBlocks()) {
                    if (mineBlock.getBlockMaterial().equals(type)) {
                        itemMaterial = mineBlock.getItemMaterial();
                        break;
                    }
                }

                // check mine regions
                if (itemMaterial == null) {
                    for (MineRegionConfig mineRegion : islandConfig.getMineRegions()) {
                        if (mineRegion.getBlockMaterial().equals(type)) {
                            itemMaterial = mineRegion.getItemMaterial();
                            break;
                        }
                    }
                }
            }
        }

        // if the target block is not mineable, find the closest mine region
        if (itemMaterial == null) {
            Location pLocation = player.getPlayer().getLocation();
            double closestDistance = 400;
            for (MineRegionConfig region : player.getCurrentIsland().getMineRegions()) {
                for (Block b : region.getBlocks()) {
                    double distance = b.getLocation().distanceSquared(pLocation);
                    if (distance < closestDistance && (!b.getType().equals(Material.PLAYER_HEAD) || !ignorePlayerHeadBlocks())) {
                        closestDistance = distance;

                        block = b;
                        itemMaterial = region.getItemMaterial();
                    }
                }
            }
        }

        // if all fails, return false
        if (itemMaterial == null
                || block == null
                || (block.getType().equals(Material.PLAYER_HEAD) && ignorePlayerHeadBlocks())
        ) return false;

        // find all adjacent blocks
        recursiveBlockSearch(block);
        return true;
    }

    private void recursiveBlockSearch(Block block) {
        if (blocks.size() >= 50 || blocks.contains(block)) return;

        blocks.add(block);
        for (BlockFace mainFace : new BlockFace[]{BlockFace.SELF, BlockFace.UP, BlockFace.DOWN}) {
            Block center = block.getRelative(mainFace);

            for (int i = 0; i < 4; i++) {
                Block relative = center.getRelative(BlockFace.values()[i]);
                if (dynamicMineBlockType != null) {
                    if (player.getDynamicMineBlocks().containsKey(relative)) {
                        recursiveBlockSearch(relative);
                    }
                } else if (relative.getType().equals(block.getType())) {
                    recursiveBlockSearch(relative);
                }
            }
        }
    }

    public int getLevel() {
        return PowerupService.i().getProgress(player, powerupType.getCategory()).getLevel();
    }

    public Block getRandomBlock() {
        if (dynamicMineBlockType != null) {
            blocks.removeIf(block -> !player.getDynamicMineBlocks().containsKey(block));
        }
        if (blocks.isEmpty()) return null;

        return blocks.get(Game.getRandom().nextInt(blocks.size()));
    }

    public Pair<Block, BlockFace> getRandomBlockRelative() {
        Block block = getRandomBlock();
        if (block == null) return null;

        List<BlockFace> faces = Arrays.stream(BlockFace.values()).limit(4).collect(Collectors.toList());
        Collections.shuffle(faces);
        faces.add(BlockFace.UP);
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (relative.getType().equals(Material.AIR) || relative.getType().equals(Material.RAIL)) {
                if (dynamicMineBlockType != null && player.getDynamicMineBlocks().containsKey(relative)) continue;
                return Pair.of(relative, face);
            }
        }

        return Pair.of(block, BlockFace.SELF);
    }

    @Nullable
    public Pair<Location, Location> getRandomSpawnAndTarget() {
        Pair<Block, BlockFace> pair = getRandomBlockRelative();
        if (pair == null) return null;

        Location target = pair.key().getLocation().add(0.5, 0.5, 0.5)
                .add(pair.value().getOppositeFace().getDirection());

        Region region = new Region(target.clone().add(-5, -3, -5), target.clone().add(5, 3, 5));
        List<Block> blocks = region.getBlocks();
        Collections.shuffle(blocks);

        Location spawn = null;
        for (Block block : blocks) {
            Block relative = block.getRelative(BlockFace.UP);

            boolean placeable = false;
            if (dynamicMineBlockType != null) {
                DynamicMineBlock dynamicBlock = DynamicMineBlocksService.i().get(player, block);
                DynamicMineBlock dynamicRelative = DynamicMineBlocksService.i().get(player, relative);

                if (dynamicBlock != null && dynamicRelative != null) {
                    placeable = dynamicBlock.getMaterial().isSolid() && dynamicRelative.getMaterial().isAir();
                }
            }
            if (!placeable) {
                placeable = block.getType().isSolid() && relative.getType().isAir();
            }

            if (placeable) {
                spawn = relative.getLocation().add(0.5, 0, 0.5);
                break;
            }
        }

        return spawn == null ? null : Pair.of(spawn, target);
    }

    public Set<GamePlayer> getPlayers() {
        return getPlayer().getCurrentIsland().getAllPlayers();
    }

    public void addEntity(Entity entity) {
        entities.add(entity);

        player.getCurrentIsland().getAllPlayers().forEach(p -> p.getAllowedEntities().add(entity.getId()));
        entity.level().addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);

        entity.discard();
        player.getCurrentIsland().getAllPlayers().forEach(p -> p.getAllowedEntities().remove(entity.getId()));
    }

    public void dropItem(Location location, int count) {
        if (player.isOffline()) return;

        Set<GamePlayer> players = getPlayers();
        for (int i = 0; i < count; i++) {
            DroppedItem.spawn(itemMaterial, location, 40, players);
        }
    }

    public void schedule(int delay, Runnable task) {
        scheduledTasks.put(task, new AtomicInteger(delay));
    }

    public double getPeriod() {
        return 4;
    }

    public abstract void tick(long ticks);

    public void onStart() {
    }

    public void onReset(boolean premature) {
    }

    public boolean ignorePlayerHeadBlocks() {
        return false;
    }
}
