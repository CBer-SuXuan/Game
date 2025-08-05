package net.mineclick.game.service;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.BuildingModel;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.type.BoosterType;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.game.util.visual.PacketNPC;
import net.mineclick.global.commands.Commands;
import net.mineclick.global.config.BuildingConfig;
import net.mineclick.global.config.DimensionConfig;
import net.mineclick.global.config.IslandConfig;
import net.mineclick.global.config.field.IslandUnlockRequired;
import net.mineclick.global.util.*;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@SingletonInit
public class IslandsService {
    private static IslandsService i;

    private final ItemStack visitingItem = ItemBuilder.builder()
            .material(Material.BARRIER)
            .title(ChatColor.YELLOW + "Go back to your island" + ChatColor.GRAY + ChatColor.ITALIC + " right-click")
            .build().toItem();

    private IslandsService() {
        Commands.addCommand(Commands.Command.builder()
                .name("island")
                .alias("home")
                .alias("spawn")
                .alias("is")
                .description("Teleport to your island")
                .callFunction((playerData, strings) -> {
                    GamePlayer player = (GamePlayer) playerData;
                    player.tpToIsland(player.getCurrentIsland(false), !player.isOnOwnIsland());
                    return null;
                })
                .build());

        Runner.sync(1, 10, state -> {
            if (DimensionConfig.getDimensionList().isEmpty()) {
                return;
            }

            Game.i().getLogger().info("Spawning building villagers");
            state.cancel();
            for (DimensionConfig dimensionConfig : DimensionConfig.getDimensionList()) {
                spawnBuildingVillagers(dimensionConfig.getIslands());
            }
        });
    }

    public static IslandsService i() {
        return i == null ? i = new IslandsService() : i;
    }

    /**
     * Load island's and its buildings' configs
     *
     * @param player The player
     */
    public void loadPlayerIslands(GamePlayer player) {
        Map<Integer, IslandModel> islands = player.getIslands();
        if (islands.isEmpty()) {
            islands.put(0, new IslandModel(0));
        }

        DimensionConfig dimension = player.getDimensionsData().getDimension();
        if (dimension == null) return;

        islands.values().forEach(island -> loadIslandConfig(island, dimension, player));
    }

    private void loadIslandConfig(IslandModel island, DimensionConfig dimension, GamePlayer player) {
        island.setPlayer(player);

        IslandConfig islandConfig = dimension.getIslands().get(island.getId());
        if (islandConfig != null) {
            island.setConfig(islandConfig);

            if (island.getBuildings().isEmpty()) {
                for (int i = 0; i < islandConfig.getBuildings().size(); i++) {
                    island.getBuildings().add(new BuildingModel(i));
                }
            }

            island.getBuildings().forEach(building -> {
                building.setIsland(island);

                BuildingConfig buildingConfig = islandConfig.getBuildings().get(building.getId());
                if (buildingConfig != null) {
                    building.setConfig(buildingConfig);
                } else {
                    Game.i().getLogger().warning("Could not load building config ("
                            + building.getId()
                            + ") for "
                            + player.getName()
                            + " "
                            + player.getUuid());
                }
            });
        } else {
            Game.i().getLogger().warning("Could not load island config ("
                    + island.getId()
                    + ") for "
                    + player.getName()
                    + " "
                    + player.getUuid());
        }
    }

    /**
     * Unlock an island
     *
     * @param player The player
     * @param id     Island id
     */
    public void unlockIsland(GamePlayer player, int id) {
        DimensionConfig dimension = player.getDimensionsData().getDimension();
        if (dimension == null) return;

        player.getIslands().computeIfAbsent(id, i -> {
            IslandModel island = new IslandModel(i);
            loadIslandConfig(island, dimension, player);
            player.tpToIsland(island, true);

            return island;
        });
    }

    /**
     * Check if the player has the prerequisites to unlock this island
     *
     * @param player The player
     * @param config The island config
     * @return True is the player has everything required to unlock this island
     */
    public boolean hasPrerequisites(GamePlayer player, IslandConfig config) {
        for (IslandUnlockRequired required : config.getUnlockRequired()) {
            WorkerType type = WorkerType.valueOf(required.getMobType().toString());
            if (!player.getWorkers().containsKey(type) || player.getWorkers().get(type).getLevel() < required.getLevel())
                return false;
        }

        return true;
    }

    /**
     * Visit an island
     *
     * @param player  The player who wants to visit
     * @param toVisit The player whom to visit
     */
    public void visitPlayer(GamePlayer player, GamePlayer toVisit) {
        if (!toVisit.getTutorial().isComplete()) {
            player.sendMessage("Can't visit this player, they didn't finish their tutorial");
            return;
        }

        if (!toVisit.isOnOwnIsland()) {
            player.sendMessage("Can't visit this player, they're not on their island");
            return;
        }

        player.visitIsland(toVisit.getCurrentIsland(false));
    }

    private BuildingModel getBuilding(GamePlayer player, int islandId, int buildingIndex) {
        IslandModel islandModel = player.getIslands().get(islandId);
        if (islandModel == null || buildingIndex >= islandModel.getBuildings().size()) {
            return null;
        }

        BuildingModel buildingModel = islandModel.getBuildings().get(buildingIndex);
        if (buildingModel.canLevelUp()) {
            return buildingModel;
        }

        return null;
    }

    private GamePlayer getActingPlayer(GamePlayer player) {
        IslandModel visitingIsland = player.getVisitingIsland();
        if (visitingIsland != null) {
            return visitingIsland.getPlayer();
        }

        return player;
    }

    public void spawnBuildingVillagers(List<IslandConfig> islands) {
        for (int islandIndex = 0; islandIndex < islands.size(); islandIndex++) {
            IslandConfig islandConfig = islands.get(islandIndex);
            List<BuildingConfig> buildings = islandConfig.getBuildings();

            for (int buildingIndex = 0; buildingIndex < buildings.size(); buildingIndex++) {
                int finalIslandIndex = islandIndex;
                int finalBuildingIndex = buildingIndex;

                PacketNPC npc = NPCService.i().spawn(buildings.get(buildingIndex).getNpcSpawn().toLocation(), VillagerType.SAVANNA, VillagerProfession.MASON, p -> {
                    GamePlayer player = getActingPlayer(p);
                    BuildingModel building = getBuilding(player, finalIslandIndex, finalBuildingIndex);
                    return building != null;
                });

                npc.setClickConsumer(player -> {
                    if (player.getVisitingIsland() != null) {
                        player.sendMessage("Only the island owner can unlock this building", MessageType.ERROR);
                        return;
                    }

                    BuildingModel building = getBuilding(player, finalIslandIndex, finalBuildingIndex);
                    if (building == null) {
                        return;
                    }
                    int buildingLevel = building.getLevel();

                    if (player.getTutorialVillager() != null) {
                        if (player.getTutorialVillager().onIslandUpgrade()) {
                            player.playSound(Sound.BLOCK_ANVIL_USE, 0.5f, 1);
                            building.getConfig().getUpgrades().get(buildingLevel).apply(player);
                            building.upgrade();
                            npc.refresh(player);

                            return;
                        }
                    }

                    String prerequisite = building.getConfig().getUpgrades().get(buildingLevel).checkPrerequisite(player);
                    if (prerequisite == null) {
                        if (player.chargeGold(building.getConfig().getCosts().get(buildingLevel).multiply(new BigNumber(BoostersService.i().getActiveBoost(BoosterType.CHEAP_BUILDINGS_BOOSTER))))) {
                            player.playSound(Sound.BLOCK_ANVIL_USE, 0.5f, 1);
                            building.getConfig().getUpgrades().get(buildingLevel).apply(player);
                            building.upgrade();
                            npc.refresh(player);
                        } else {
                            player.sendMessage("Not enough gold!", MessageType.ERROR);
                            // TODO display how much time to get there
                            player.noSound();
                        }
                    } else {
                        player.noSound();
                        player.getPlayer().sendMessage(ChatColor.RED + "You can't unlock this building yet");
                        player.getPlayer().sendMessage(ChatColor.RED + prerequisite);
                    }
                });

                Function<GamePlayer, BigNumber> getCost = (player) -> {
                    BuildingModel building = getBuilding(player, finalIslandIndex, finalBuildingIndex);
                    if (building == null) {
                        return null;
                    }
                    return building.getConfig().getCosts().get(building.getLevel()).multiply(new BigNumber(BoostersService.i().getActiveBoost(BoosterType.CHEAP_BUILDINGS_BOOSTER)));
                };
                Function<GamePlayer, Boolean> canAfford = (player) -> {
                    BigNumber cost = getCost.apply(player);
                    if (cost == null) {
                        return false;
                    }

                    return (player.getTutorialVillager() != null && player.getTutorialVillager().isCheckForIslandUpgrade()) || player.getGold().greaterThanOrEqual(cost);
                };


                npc.setHasParticles(p -> {
                    GamePlayer player = getActingPlayer(p);
                    BuildingModel building = getBuilding(player, finalIslandIndex, finalBuildingIndex);
                    if (building == null) {
                        return false;
                    }
                    return building.getConfig().getUpgrades().stream().allMatch(u -> u.checkPrerequisite(player) == null) && canAfford.apply(player);
                });
                npc.addHologram(p -> {
                    GamePlayer player = getActingPlayer(p);
                    BigNumber cost = getCost.apply(player);
                    if (cost == null) {
                        return null;
                    }

                    return (canAfford.apply(player) ? ChatColor.GREEN : ChatColor.RED) + "Cost: " + cost.print(player) + ChatColor.GREEN + " gold";
                }, false);
                npc.addHologram(p -> {
                    GamePlayer player = getActingPlayer(p);
                    BuildingModel building = getBuilding(player, finalIslandIndex, finalBuildingIndex);
                    if (building == null) {
                        return null;
                    }

                    String description = building.getConfig().getUpgrades().get(building.getLevel()).getDescription(player);
                    if (description != null) {
                        return ChatColor.YELLOW + description;
                    }
                    return null;
                }, false);
                npc.addHologram(p -> {
                    GamePlayer player = getActingPlayer(p);
                    BuildingModel building = getBuilding(player, finalIslandIndex, finalBuildingIndex);
                    if (building == null) {
                        return null;
                    }

                    return (canAfford.apply(player) ? ChatColor.YELLOW : ChatColor.RED) + building.getConfig().getNames().get(building.getLevel());
                }, false);
                npc.addHologram(p -> {
                    GamePlayer player = getActingPlayer(p);
                    return canAfford.apply(player) ? ChatColor.GOLD + "Click to unlock!" : "";
                }, true);
            }

            // spawn parkour hologram
            if (islandConfig.getParkour() != null) {
                Location location = islandConfig.getParkour().getNpcSpawnLocation().toLocation().add(0, 1.5, 0);
                HologramsService.i().spawn(location, p -> p.getParkour().isStarted() ? ChatColor.YELLOW + "Step here to finish" : null, null, true);
            }
        }
    }
}
