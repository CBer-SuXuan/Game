package net.mineclick.game.service;

import com.comphenix.protocol.wrappers.BlockPosition;
import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.menu.LobbyTpMenu;
import net.mineclick.game.minigames.spleef.SpleefService;
import net.mineclick.game.model.*;
import net.mineclick.game.type.DynamicMineBlockType;
import net.mineclick.game.type.Holiday;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.StatisticType;
import net.mineclick.game.type.quest.Quest;
import net.mineclick.game.type.quest.villager.BookshelvesQuest;
import net.mineclick.game.type.quest.villager.CollectorQuest;
import net.mineclick.game.type.quest.villager.ElytraQuest;
import net.mineclick.game.util.packet.BlockPolice;
import net.mineclick.game.util.visual.PacketNPC;
import net.mineclick.global.Constants;
import net.mineclick.global.commands.Commands;
import net.mineclick.global.model.PlayerModel;
import net.mineclick.global.service.ConfigurationsService;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.*;
import net.mineclick.global.util.location.LocationParser;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@SingletonInit
public class LobbyService implements Listener {
    private static LobbyService i;
    @Getter
    private final Map<Block, EasterEgg> easterEggs = new HashMap<>();
    private final Set<UUID> justTeleported = new HashSet<>();
    private final List<GeodeCrusher> geodeCrushers = new ArrayList<>();
    private final Set<Material> crimsonAttachMaterials = Set.of(
            Material.ANDESITE,
            Material.STONE,
            Material.COBBLESTONE,
            Material.CYAN_TERRACOTTA
    );
    private final Set<DynamicMineBlock> crimsonBlocks = new HashSet<>();
    private final BiConsumer<GamePlayer, DynamicMineBlock> crimsonBreakConsumer = (player, block) -> {
        crimsonBlocks.remove(block);

        StatisticsService.i().increment(player.getUuid(), StatisticType.BROKEN_CRIMSON);

        // check collector quest
        Quest collector = QuestsService.i().getQuest("collector");
        if (collector != null) {
            ((CollectorQuest) collector).checkMinedBlock(player, Material.CRIMSON_HYPHAE);
        }

        // Check quest
        Quest quest = QuestsService.i().getQuest("crimson");
        if (quest == null) {
            return;
        }
        QuestProgress progress = quest.getQuestProgress(player);
        if (progress == null) {
            return;
        }

        // Increment quest progress
        if (!progress.isComplete() && progress.getObjective() == 1) {
            progress.setTaskProgress(progress.getTaskProgress() + 1);
        } else if (progress.isComplete()) {
            // Otherwise, check for rewards every 100 blocks
            Statistic statistic = StatisticsService.i().get(player.getUuid(), StatisticType.BROKEN_CRIMSON);
            if (statistic != null) {
                if (statistic.getScore() % 100 == 0) {
                    player.sendMessage(ChatColor.GREEN + "Cleaned " + Formatter.format(statistic.getScore()) + " crimson blocks!");
                    player.sendMessage(ChatColor.GREEN + "+" + ChatColor.YELLOW + "15" + ChatColor.AQUA + " schmepls");
                    player.sendMessage(ChatColor.GREEN + "+" + ChatColor.YELLOW + "10" + ChatColor.AQUA + " EXP");
                    player.addSchmepls(15);
                    player.addExp(10);
                    player.levelUpSound();
                }
                if (statistic.getScore() % 10 == 0 && Game.getRandom().nextDouble() < 0.01) {
                    player.sendMessage("You found a geode!", MessageType.INFO);

                    Rarity rarity = GeodesService.i().addGeode(player);
                    player.sendMessage(ChatColor.GREEN + "+" + ChatColor.YELLOW + "1 " + rarity.getGeodeName() + ChatColor.AQUA + " geode");
                    player.levelUpSound();
                }
            }

            // Check daily quest
            QuestsService.i().incrementProgress(player, "dailyCrimson", 0, 1);
        }
    };
    private Set<Sign> signs = new HashSet<>();
    private Set<GamePlayer> cachedPlayersInLobby;
    @Getter
    private Location spawn;
    @Getter
    private Location rewardsChest;
    @Getter
    private Location teleporter;

    private LobbyService() {
        ConfigurationsService.i().onUpdate("lobby", () -> Runner.sync(this::load));

        // Invalid cached players in lobby every 10 ticks
        Runner.sync(10, 10, state -> cachedPlayersInLobby = null);

        Runner.sync(20, 20, (state) -> {
            // flick players off of geode crushers
            geodeCrushers.forEach(crusher -> crusher.checkPlayersStanding(getPlayersInLobby()));

            // place crimson blocks
            spreadCrimson();
        });

        // Update signs
        // TODO might be causing lag
        Runner.sync(0, 100, state -> signs.forEach(Sign::update));

        // Add commands
        Commands.addCommand(Commands.Command.builder()
                .name("lobby")
                .alias("hub")
                .description("Teleport to lobby")
                .callFunction((playerData, strings) -> {
                    spawn((GamePlayer) playerData);
                    return null;
                })
                .build());
        Commands.addCommand(Commands.Command.builder()
                .name("spleef")
                .description("Teleport to the Spleef arena")
                .callFunction((playerData, strings) -> {
                    spawn((GamePlayer) playerData);

                    Runner.sync(40, () -> {
                        Player player = playerData.getPlayer();
                        if (player != null) {
                            player.teleport(SpleefService.i().getRespawn());
                        }
                    });
                    return null;
                })
                .build());

        Bukkit.getPluginManager().registerEvents(this, Game.i());
    }

    public static LobbyService i() {
        return i == null ? i = new LobbyService() : i;
    }

    private void load() {
        ConfigurationSection section = ConfigurationsService.i().get("lobby");

        if (section == null) {
            return;
        }

        spawn = LocationParser.parse(section.getString("spawn"));
        rewardsChest = LocationParser.parse(section.getString("rewardsChest"));
        teleporter = LocationParser.parse(section.getString("teleporter"));

        // Easter eggs
        easterEggs.keySet().forEach(b -> {
            b.setType(Material.AIR);
            BlockPolice.exclude.remove(new BlockPosition(b.getX(), b.getY(), b.getZ()));
        });
        easterEggs.clear();
        List<String> stringList = section.getStringList("easterEggs");
        for (int i = 0; i < stringList.size(); i++) {
            Location eggLoc = LocationParser.parse(stringList.get(i));
            EasterEgg easterEgg = new EasterEgg(eggLoc, "lobby:" + i);
            easterEggs.put(easterEgg.location().getBlock(), easterEgg);

            eggLoc.getBlock().setType(Material.COBWEB);
            BlockPolice.exclude.add(new BlockPosition(eggLoc.getBlockX(), eggLoc.getBlockY(), eggLoc.getBlockZ()));
        }

        // Signs
        signs = new HashSet<>();
        for (Map<?, ?> map : section.getMapList("signs")) {
            String image = String.valueOf(map.get("image"));
            int width = Integer.parseInt(String.valueOf(map.get("width")));
            int height = Integer.parseInt(String.valueOf(map.get("height")));
            Location loc = LocationParser.parse(String.valueOf(map.get("loc")));
            Direction face = Direction.valueOf(String.valueOf(map.get("face")));

            signs.add(new Sign(width, height, image, loc, face));
        }

        // Preload chunks
        if (teleporter != null && !Constants.QUICK_LOAD) {
            Runner.sync(100, () -> ChunkPreload.preload(teleporter.getChunk()));
        }

        // Load geode crushers
        geodeCrushers.forEach(GeodeCrusher::clear);
        geodeCrushers.clear();
        for (String loc : section.getStringList("geodeCrushers")) {
            geodeCrushers.add(new GeodeCrusher(LocationParser.parse(loc)));
        }

        // Spawn ascend npc
        Location ascendNpcSpawn = LocationParser.parse(section.getString("ascendNpcSpawn"));
        PacketNPC npc = NPCService.i().spawn(ascendNpcSpawn, VillagerType.SAVANNA, VillagerProfession.LIBRARIAN, p -> true);
        npc.addHologram(p -> {
            long schmepls = AscensionServices.i().getAscendSchmepls(p);
            if (schmepls > 0) {
                int exp = AscensionServices.i().getAscendExp(p);
                return ChatColor.YELLOW + "and " + ChatColor.GREEN + "+" + ChatColor.AQUA + exp + ChatColor.YELLOW + " EXP";
            } else {
                return ChatColor.GRAY + "more gold to ascend";
            }
        }, false);
        npc.addHologram(p -> {
            long schmepls = AscensionServices.i().getAscendSchmepls(p);
            if (schmepls > 0) {
                return ChatColor.YELLOW + "Ascend now to get " + ChatColor.GREEN + "+" + ChatColor.AQUA + schmepls + ChatColor.YELLOW + " schmepls";
            } else {
                return ChatColor.GRAY + "You need to make " + AscensionServices.i().getAscendRequiredGold(p).print(p);
            }
        }, false);
        npc.addHologram(p -> AscensionServices.i().hasMinimumGold(p) ? (ChatColor.GREEN + "Ready to ascend!") : ChatColor.GRAY + "Click for details", true);
        npc.setHasParticles(p -> AscensionServices.i().hasMinimumGold(p));
        npc.setClickConsumer(p -> AscensionServices.i().openAscendMenu(p, null));

        // Rewards chest
        HologramsService.i().spawn(rewardsChest.clone().add(0.5, 1, 0.5), player -> {
            DailyRewardChest chest = player.getDailyRewardChest();
            return chest.isEmpty(player)
                    ? ChatColor.GRAY + "Refills in " + ChatColor.GOLD + chest.getTimeLeft()
                    : (ChatColor.YELLOW + (chest.getClicks() > 0 ? "Keep mining!" : "Mine with your pickaxe!"));
        }, false);
        HologramsService.i().spawn(rewardsChest.clone().add(0.5, 1.25, 0.5), player -> ChatColor.GREEN + "Daily Rewards Chest", false);
    }

    /**
     * Check if the player is in the lobby
     *
     * @param playerModel The player in question
     * @return True if in the lobby. Will return false if player is offline
     */
    public boolean isInLobby(PlayerModel playerModel) {
        if (playerModel.isOffline() || spawn == null)
            return false;

        Location l = playerModel.getPlayer().getLocation();
        return l.getX() < spawn.getX() + 500 &&
                l.getX() > spawn.getX() - 500 &&
                l.getZ() < spawn.getZ() + 500 &&
                l.getZ() > spawn.getZ() - 500;
    }

    /**
     * Get all the players in lobby.
     * This method is cached for 10 ticks as it's quite expensive
     *
     * @return A set of players that are currently
     * (for the last 10 ticks) in the lobby
     */
    public Set<GamePlayer> getPlayersInLobby() {
        if (cachedPlayersInLobby != null) return cachedPlayersInLobby;
        return cachedPlayersInLobby = PlayersService.i().<GamePlayer>getAll().stream()
                .filter(this::isInLobby)
                .collect(Collectors.toSet());
    }

    public Set<GamePlayer> getPlayersInLobby(Location closeBy) {
        return getPlayersInLobby().stream()
                .filter(p -> p.getPlayer() != null && p.getPlayer().getLocation().distanceSquared(closeBy) <= 2500)
                .collect(Collectors.toSet());
    }

    public void spawn(GamePlayer player) {
        if (!player.getTutorial().isComplete()) {
            player.sendMessage("You must complete the tutorial first", MessageType.ERROR);
            return;
        }
        if (justTeleported.contains(player.getUuid())) return;
        justTeleported.add(player.getUuid());

        player.getTrader().despawn();
        player.getParkour().reset();
        player.removeVisitingIsland();

        if (Holiday.CHRISTMAS.isNow()) {
            player.getPlayer().setPlayerTime(18000, false);
        } else {
            player.getPlayer().setPlayerTime(6000, false);
        }

        if (player.getRank().isAtLeast(Rank.PAID)) {
            player.getPlayer().setAllowFlight(true);
        }

        Runner.sync(20, () -> {
            justTeleported.remove(player.getUuid());

            // Add to crimson blocks
            crimsonBlocks.forEach(dynamicMineBlock -> dynamicMineBlock.addPlayer(player));

            // Place frozen elytra block
            ElytraQuest elytraQuest = (ElytraQuest) QuestsService.i().getQuest("elytra");
            if (elytraQuest != null) {
                elytraQuest.placeElytraBlock(player);
            }

            // Place dusty bookshelves blocks
            BookshelvesQuest bookshelvesQuest = (BookshelvesQuest) QuestsService.i().getQuest("bookshelves");
            if (bookshelvesQuest != null) {
                bookshelvesQuest.placeBlocks(player);
            }
        });

        player.getPlayer().teleport(spawn);
        player.updateInventory();
        LeaderboardsService.i().update(player);
        PlayersService.i().showToAll(player);
    }

    @EventHandler
    public void on(PlayerInteractEvent e) {
        if (e.hasBlock()) {
            EasterEgg egg = easterEggs.get(e.getClickedBlock());
            if (egg != null) {
                PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
                    if (player != null && player.getLobbyData().getCollectedEasterEggs().add(egg.id())) {
                        ParticlesUtil.send(ParticleTypes.WITCH, egg.location(), Triple.of(0.1f, 0.1f, 0.1f), 25, player);
                        player.sendMessage("You found an Easter egg!", MessageType.INFO);
                        player.levelUpSound();

                        AchievementsService.i().incrementProgress(player, "easteregg", 1);
                    }
                });
            }
        }
    }

    public void updateInventory(GamePlayer player) {
        if (LobbyService.i().isInLobby(player)) {
            Player bukkitPlayer = player.getPlayer();
            bukkitPlayer.getInventory().setItem(0, LobbyTpMenu.MENU_ITEM);
            bukkitPlayer.getInventory().setItem(3, GadgetsService.i().getMenuItem());

            Gadget gadget = GadgetsService.i().getGadget(player);
            if (gadget != null) {
                bukkitPlayer.getInventory().setItem(1, GadgetsService.i().buildItem(gadget, false));
            }
        }
    }

    public void checkGeodeCrusherClick(GamePlayer player, Block clickedBlock) {
        if (player.isOpeningGeode()) return;

        Location location = clickedBlock.getLocation();
        geodeCrushers.stream()
                .filter(crusher -> crusher.getBlockLocation().equals(location))
                .findAny()
                .ifPresent(geodeCrusher -> {
                    GeodesService.i().openMenu(player, geodeCrusher);
                    player.clickSound();
                });
    }

    public void spreadCrimson() {
        if (spawn == null || crimsonBlocks.size() > 600) return;

        double r = 150 * Math.sqrt(Game.getRandom().nextDouble());
        double theta = Game.getRandom().nextDouble() * 2 * Math.PI;

        Vector center = new Vector(1, 0, 0);
        center.rotateAroundY(theta);
        center.multiply(r);

        Location loc = spawn.clone().add(center.getX(), 0, center.getZ());
        loc.setY(70);

        int y = 0;
        Block block = loc.getBlock();
        while (y++ < 50 && !crimsonAttachMaterials.contains(block.getType())) {
            block = block.getRelative(BlockFace.UP);
        }
        if (!crimsonAttachMaterials.contains(block.getType())) {
            return;
        }
        Block finalBlock = block;
        if (SpleefService.i().getArenaBlocks().stream().anyMatch(arenaBlock -> NumberConversions.square(arenaBlock.getX() - finalBlock.getX()) + NumberConversions.square(arenaBlock.getZ() - finalBlock.getZ()) < 100)) {
            return;
        }

        crimsonBlocks.addAll(DynamicMineBlocksService.i().generateBlob(DynamicMineBlockType.LOBBY_CRIMSON, block.getLocation(), true, Game.getRandom().nextInt(6) + 2, Material.CRIMSON_HYPHAE, 10, crimsonBreakConsumer, getPlayersInLobby()));
    }
}
