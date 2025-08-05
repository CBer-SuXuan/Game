package net.mineclick.game.service;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.model.ParkourData;
import net.mineclick.game.type.StatisticType;
import net.mineclick.global.config.DimensionConfig;
import net.mineclick.global.config.field.ParkourConfig;
import net.mineclick.global.config.field.UpgradeConfig;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.*;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SingletonInit
public class ParkourService implements Listener {
    public static final ItemStack ITEM = ItemBuilder.builder()
            .material(Material.WOODEN_HOE)
            .title(ChatColor.YELLOW + "Checkpoint Marker" + ChatColor.GRAY + ChatColor.ITALIC + " right-click")
            .lore(ChatColor.GRAY + "Places a single use checkpoint")
            .lore(ChatColor.GRAY + "at your current location")
            .lore(" ")
            .lore(ChatColor.GRAY + "Checkpoints are placed automatically")
            .lore(ChatColor.GRAY + "if you have a " + ChatColor.LIGHT_PURPLE + "Premium Membership.")
            .lore(ChatColor.GRAY + "Get yours at " + ChatColor.AQUA + "store.mineclick.net")
            .build().toItem();
    public static final ItemStack RUNNING_SHOES = ItemBuilder.builder().title(ChatColor.YELLOW + "Smelly running shoes")
            .lore(ChatColor.GREEN + "Increased movement speed")
            .lore(" ")
            .lore(ChatColor.GRAY + "Click to swap")
            .material(Material.LEATHER_BOOTS)
            .build().toItem();
    public static final ItemStack ELYTRA = ItemBuilder.builder().title(ChatColor.YELLOW + "Fairly used elytra")
            .lore(" ")
            .lore(ChatColor.GRAY + "Click to swap")
            .material(Material.ELYTRA)
            .unbreakable(true)
            .build().toItem();
    private static ParkourService i;
    private final Set<UUID> damageCooldown = new HashSet<>();

    private ParkourService() {
        Bukkit.getPluginManager().registerEvents(this, Game.i());
    }

    public static ParkourService i() {
        return i == null ? i = new ParkourService() : i;
    }

    public void handleClick(GamePlayer player) {
        ParkourData parkourData = player.getParkour();
        if (!parkourData.isStarted())
            return;
        if (parkourData.getCheckpoints() - parkourData.getCheckpointsUsed() <= 0) {
            return;
        }

        Location checkpointLoc = findCheckpointLocation(player);
        if (checkpointLoc != null) {
            parkourData.setCheckpoint(checkpointLoc);
            parkourData.setCheckpointsUsed(parkourData.getCheckpointsUsed() + 1);
            player.updateParkourItem();

            player.playSound(Sound.BLOCK_SNOW_PLACE);
            ParticlesUtil.send(ParticleTypes.LAVA, checkpointLoc, Triple.of(0F, 0F, 0F), 2, player);
        } else {
            player.getPlayer().sendMessage(ChatColor.RED + "Can't place a checkpoint at this location");
        }
    }

    private Location findCheckpointLocation(GamePlayer player) {
        Block block = player.getPlayer().getLocation().getBlock();
        Block down = block.getRelative(BlockFace.DOWN);
        Block up = block.getRelative(BlockFace.UP);

        Material type = block.getType();
        Material downType = down.getType();
        Material upType = up.getType();

        if (type.equals(Material.LAVA) || upType.equals(Material.LAVA) || downType.equals(Material.LAVA)) return null;
        if ((type.isSolid() || downType.isSolid()) && up.isEmpty()) {
            Location loc = (type.isSolid() ? up : block).getLocation();
            loc.setYaw(player.getPlayer().getLocation().getYaw());

            return loc;
        }

        return null;
    }

    @EventHandler
    public void on(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause().equals(EntityDamageEvent.DamageCause.LAVA)) {
            UUID uuid = e.getEntity().getUniqueId();

            if (damageCooldown.contains(uuid)) return;
            damageCooldown.add(uuid);
            Runner.sync(20, () -> damageCooldown.remove(uuid));

            PlayersService.i().<GamePlayer>get(uuid, player -> {
                ParkourConfig parkourConfig = player.getCurrentIsland(true).getConfig().getParkour();
                if (player.getParkour().isStarted() && parkourConfig != null) {
                    fail(player, parkourConfig);
                }
            });
        }
    }

    @EventHandler
    public void on(PlayerMoveEvent e) {
        PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
            if (!player.isOnOwnIsland())
                return;

            IslandModel island = player.getCurrentIsland(true);
            if (island.getConfig().getParkour() == null || island.getConfig().getParkour().getStartRegion() == null)
                return;

            ParkourData parkour = player.getParkour();
            ParkourConfig config = island.getConfig().getParkour();
            if (parkour.isStarted()) {
                if (e.getTo().getY() < config.getLowestY()) {
                    fail(player, config);
                } else if (player.getRank().isAtLeast(Rank.PAID) && parkour.getCheckpoint() == null && !player.getPlayerSettings().getNoAutoCheckpoints().get()) {
                    Location loc = parkour.getLastLocation();
                    Location newLoc = findCheckpointLocation(player);
                    if (newLoc != null && (loc == null || loc.getBlockX() != newLoc.getBlockX() || loc.getBlockZ() != newLoc.getBlockZ())) {
                        parkour.setLastLocation(newLoc);
                    }
                }

                if (island.getConfig().getParkour() != null) {
                    Block end = island.getConfig().getParkour().getNpcSpawnLocation().toLocation().getBlock();
                    if (end.equals(player.getPlayer().getLocation().getBlock())) {
                        boolean firstTime = !parkour.getCompletedIslands().contains(island.getId());

                        QuestsService.i().incrementProgress(player, "parkourLobby", 1, 1);
                        QuestsService.i().incrementProgress(player, "parkourLobby", 3, 1);
                        QuestsService.i().incrementProgress(player, "dailyParkour", 0, 1);

                        if (firstTime) {
                            StatisticsService.i().increment(player.getUuid(), StatisticType.PARKOUR_SUCCESS);
                            parkour.getCompletedIslands().add(island.getId());

                            if (parkour.getCompletedIslands().size() == player.getDimensionsData().getDimension().getIslands().size()) {
                                parkour.getCompletedDimensions().add(player.getDimensionsData().getCurrentDimensionId());
                            }

                            // All parkour courses challenge
                            if (parkour.getCompletedDimensions().size() == DimensionConfig.getDimensionList().size()) {
                                AchievementsService.i().setProgress(player, "parkourChallenge", 1);
                            }

                            for (UpgradeConfig upgrade : island.getConfig().getParkourUpgrades()) {
                                upgrade.apply(player);
                            }
                        }

                        player.sendMessage(Strings.line());
                        player.sendMessage(Strings.middle(ChatColor.GOLD + island.getConfig().getName() + " parkour complete"));

                        long score = System.currentTimeMillis() - parkour.getStartedOn();
                        long highScore = parkour.getHighScore();
                        player.sendMessage(" ");
                        player.sendMessage(Strings.middle(ChatColor.YELLOW + Formatter.durationWithMilli(score)));
                        if (highScore == 0 || highScore > score) {
                            player.sendMessage(Strings.middle(ChatColor.AQUA + "New personal best!"));
                            parkour.setHighScore(player, score);
                            player.levelUpSound();
                        } else {
                            player.sendMessage(Strings.middle(ChatColor.GRAY + "Personal best: " + ChatColor.DARK_GREEN + Formatter.durationWithMilli(highScore)));
                            player.expSound();
                        }

                        player.sendMessage(Strings.line());

                        parkour.reset();
                        player.updateInventory();
                    }
                }
            } else if (config.getStartRegion().isIn(e.getTo())) {
                if (!player.getPlayerSettings().getFlight().get()) {
                    parkour.setStarted(true);
                    parkour.setStartedOn(System.currentTimeMillis());
                    player.updateParkourItem();
                }
            }
        });
    }

    private void fail(GamePlayer player, ParkourConfig config) {
        ParkourData parkourData = player.getParkour();
        if (!parkourData.isStarted())
            return;

        Location location = parkourData.getCheckpoint();
        if (location == null && parkourData.getCheckpointsUsed() < parkourData.getCheckpoints()) {
            location = parkourData.getLastLocation();
            parkourData.setCheckpointsUsed(parkourData.getCheckpointsUsed() + 1);
            player.updateParkourItem();
        }
        if (location != null) {
            player.getPlayer().teleport(location.clone().add(0.5, 0.1, 0.5));
            parkourData.setCheckpoint(null);

            Runner.sync(1, () -> player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT));
        } else {
            player.getPlayer().getInventory().setItem(0, null);
            player.getPlayer().teleport(config.getSpawnLocation().toLocation());
            parkourData.reset();
            player.updateParkourItem();

            StatisticsService.i().increment(player.getUuid(), StatisticType.PARKOUR_FAILS);

            Runner.sync(1, () -> player.playSound(Sound.ENTITY_PLAYER_DEATH));
        }
    }
}
