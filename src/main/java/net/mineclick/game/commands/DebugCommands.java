package net.mineclick.game.commands;

import net.mineclick.game.Game;
import net.mineclick.game.model.BuildingModel;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeItem;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.model.achievement.AchievementNode;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.model.worker.WorkerConfiguration;
import net.mineclick.game.service.*;
import net.mineclick.game.type.PermanentMultiplier;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.powerup.PowerupCategory;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.global.commands.Commands;
import net.mineclick.global.config.BuildingConfig;
import net.mineclick.global.config.DimensionConfig;
import net.mineclick.global.config.IslandConfig;
import net.mineclick.global.config.field.UpgradeConfig;
import net.mineclick.global.model.PlayerModel;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SingletonInit
public class DebugCommands {
    private static final Set<GamePlayer> entityTracking = new HashSet<>();
    private static final Set<GamePlayer> taskTracking = new HashSet<>();
    private static DebugCommands i;

    private DebugCommands() {
        init();
    }

    public static DebugCommands i() {
        return i == null ? i = new DebugCommands() : i;
    }

    private void init() {
        Runner.sync(0, 1, state -> {
            if (!entityTracking.isEmpty()) {
                entityTracking.removeIf(PlayerModel::isOffline);
                int entities = Game.i().getWorld().getEntities().size();
                entityTracking.forEach(p -> p.getPlayer().setLevel(entities));
            }

            if (!taskTracking.isEmpty()) {
                taskTracking.removeIf(PlayerModel::isOffline);
                int tasks = Bukkit.getScheduler().getPendingTasks().size();
                taskTracking.forEach(p -> p.getPlayer().setLevel(tasks));
            }
        });


        Commands.addCommand(Commands.Command.builder()
                .name("treasure")
                .minRank(Rank.DEV)
                .callFunction((playerModel, strings) -> {
                    ((GamePlayer) playerModel).addTreasureMap();

                    return null;
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("geode")
                .minRank(Rank.DEV)
                .callFunction((playerModel, strings) -> {
//                    Rare.SPEED_INIT = Double.parseDouble(strings[0]);
//                    Rare.SPEED = Double.parseDouble(strings[1]);

                    GamePlayer player = (GamePlayer) playerModel;
                    player.getGeodes().clear();
                    for (Rarity rarity : Rarity.values()) {
                        if (rarity.equals(Rarity.SPECIAL)) continue;
                        player.getGeodes().put(rarity, 1000);
                    }

                    // test rarity
                    if (strings.length > 0) {
                        for (Rarity geodeRarity : Rarity.values()) {
                            if (geodeRarity.equals(Rarity.SPECIAL)) continue;
                            Map<Rarity, Integer> countMap = new HashMap<>();
                            List<GeodeItem> items = GeodesService.i().openGeode(player, geodeRarity, true);
                            for (GeodeItem item : items) {
                                countMap.compute(item.getRarity(), (r, integer) -> integer == null ? 1 : integer + 1);
                            }

                            StringBuilder out = new StringBuilder(geodeRarity.getGeodeName() + ": \n ");
                            for (Rarity rarity : Rarity.values()) {
                                if (rarity.equals(Rarity.SPECIAL)) continue;
                                out.append(countMap.getOrDefault(rarity, 0) / 10).append(" ");
                            }
                            player.sendMessage(out.toString());
                        }
                    }

                    return null;
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("orb")
                .minRank(Rank.DEV)
                .callFunction((playerModel, strings) -> {
                    if (strings.length > 0) {
//                        Orb.Y = Double.parseDouble(strings[0]);
                    } else {
                        PowerupType.CANDY_CANE_ORB.run((GamePlayer) playerModel);
                    }

                    return null;
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("mineshafttest")
                .hidden(true)
//                .usage("<width> <frequency> <amplitude> <minHeight>")
//                .minArgs(4)
                .callFunction((playerModel, strings) -> {
                    GamePlayer player = ((GamePlayer) playerModel);
//                    double width = Double.parseDouble(strings[0]);
//                    double frequency = Double.parseDouble(strings[1]);
//                    double amp = Double.parseDouble(strings[2]);
//                    // TODO [10 2 10 5] is nice ^

                    MineshaftService.i().spawn(player);

//
//
//                    DynamicMineBlocksService.i().clear(player, true);
//
//                    Location origin = LocationParser.parse("0 100 -2000");
//                    Player p = playerModel.getPlayer();
//                    if (p.getLocation().distanceSquared(origin) > 2000) {
//                        p.teleport(origin.clone().add(0, 5, 0));
//                    }
//
//                    Function<Location, Integer> getHighest = location -> player.getDynamicMineBlocks().keySet().stream()
//                            .filter(b -> b.getX() == location.getBlockX() && b.getZ() == location.getBlockZ())
//                            .max(Comparator.comparingInt(Block::getY))
//                            .map(Block::getY)
//                            .orElse(origin.getBlockY());
//
//                    List<Location> locations = new ArrayList<>();
//                    PerlinNoiseGenerator perlin = new PerlinNoiseGenerator(Game.getRandom());
//
//                    int r = 8;
//                    int radiusSquared = r * r;
//                    for (int x = -r; x <= r; x++) {
//                        for (int z = -r; z <= r; z++) {
//                            double distanceSquared = x * x + z * z;
//
//                            if (distanceSquared <= radiusSquared) {
//                                double noise = perlin.noise(x / width * frequency, 0, z / width * frequency) * amp + (amp / 2F);
//                                if (noise > min) {
//                                    for (int i = 1; i <= noise - min; i++) {
//                                        locations.add(origin.clone().add(x, 20, z));
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    Collections.shuffle(locations);
//
//                    Runner.sync(0, 1, state -> {
//                        if (locations.isEmpty()) {
//                            state.cancel();
//                            return;
//                        }
//                        Location spawn = locations.remove(0);
//
//
////                        Vector v = new RandomVector(true).multiply(Math.min(1, Math.abs(Game.getRandom().nextGaussian() / 2)) * 8);
////                        Location spawn = origin.clone().add(0, 20, 0).add(v);
//
//                        BlockData blockData = Bukkit.createBlockData(Material.COAL_ORE);
//                        EntityFallingBlock fallingBlock = new EntityFallingBlock(((CraftWorld) spawn.getWorld()).getHandle(), spawn.getBlockX() + 0.5, spawn.getY(), spawn.getBlockZ() + 0.5, ((CraftBlockData) blockData).getState()) {
//                            @Override
//                            public void tick() {
//                                setMot(getMot().add(0.0D, -0.04D, 0.0D));
//                                move(EnumMoveType.SELF, getMot());
//
//                                int endY = getHighest.apply(spawn) + 1;
//                                if (onGround || getY() - 0.25 <= endY) {
//                                    discard();
//
//                                    Location endLocation = spawn.clone().getBlock().getLocation().add(0.5, 0.5, 0.5);
//                                    endLocation.setY(endY);
//
//                                    player.playSound(Sound.BLOCK_SAND_FALL, endLocation, 0.3, 1);
//                                    ParticlesUtil.send(ParticleTypes.CRIT, endLocation, Triple.of(0.5F, 0.5F, 0.5F), 10, player);
//
//                                    DynamicMineBlock mineBlock = DynamicMineBlocksService.i().create(DynamicMineBlockType.GUILD, endLocation.getBlock(), Material.COAL_ORE, 25, Collections.singleton(player));
//                                    mineBlock.setOnBreak(() -> {
//                                        ParticlesUtil.send(ParticleTypes.CLOUD, endLocation, Triple.of(0.25F, 0.25F, 0.25F), 5, player);
//                                        for (int i = 0; i < 10; i++) {
//                                            DroppedItem.spawn(Material.COAL_ORE, endLocation, 100, Collections.singleton(player), (location, p) -> true);
//                                        }
//                                    });
//                                }
//                            }
//                        };
//                        fallingBlock.setInvulnerable(true);
//                        fallingBlock.tickCount = 1;
//
//                        player.getAllowedEntities().add(fallingBlock.getId());
//                        fallingBlock.getWorld().addEntity(fallingBlock);
//                    });

                    return null;
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("stats")
                .description("Dimension stats")
                .minRank(Rank.DEV)
                .callFunction((playerModel, strings) -> {
                    int dimensionId = 0;
                    if (strings.length > 0) {
                        dimensionId = Integer.parseInt(strings[0]);
                    }

                    GamePlayer player = (GamePlayer) playerModel;
                    DimensionConfig dimension = DimensionConfig.getById(dimensionId);

                    double totalIslandMultiplier = 0;
                    double superBlock = 0;
                    int pickaxes = 0;
                    Map<String, Double> totalWorkerMultipliers = new LinkedHashMap<>();
                    Map<String, BigNumber> totalWorkerCost = new LinkedHashMap<>();
                    for (IslandConfig island : dimension.getIslands()) {
                        totalIslandMultiplier += island.getMultiplier();

                        for (BuildingConfig building : island.getBuildings()) {
                            ArrayList<UpgradeConfig> upgrades = building.getUpgrades();
                            for (int i = 0; i < upgrades.size(); i++) {
                                UpgradeConfig upgrade = upgrades.get(i);
                                if (upgrade.getUpgradeType().equals(UpgradeConfig.UpgradeType.WORKER_BPS)) {
                                    String s = upgrade.getArg();
                                    String[] split = s.split(":");
                                    double percent = Double.parseDouble(split[1]);

                                    BigNumber cost = building.getCosts().get(i);

                                    totalWorkerMultipliers.compute(split[0], (s1, sum) -> sum == null ? percent : sum + percent);
                                    totalWorkerCost.compute(split[0], (s1, c) -> c == null ? cost : c.add(cost));
                                } else if (upgrade.getUpgradeType().equals(UpgradeConfig.UpgradeType.PICKAXE_AMOUNT)) {
                                    pickaxes++;
                                } else if (upgrade.getUpgradeType().equals(UpgradeConfig.UpgradeType.SUPER_BLOCK)) {
                                    double percent = Double.parseDouble(upgrade.getArg());

                                    superBlock += percent;
                                }
                            }
                        }
                    }

                    player.sendMessage("Dimension " + dimension.getName());
                    player.sendMessage(" Total island multiplier: " + totalIslandMultiplier);
                    player.sendMessage(" Total pickaxes: " + pickaxes);
                    player.sendMessage(" Total super block: " + superBlock);
                    for (Map.Entry<String, Double> entry : totalWorkerMultipliers.entrySet()) {
                        player.sendMessage(" " + entry.getKey() + ": " + entry.getValue() + " " + totalWorkerCost.get(entry.getKey()).toEngineeringString());
                    }

                    return null;
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("g")
                .description("Auto-progress by an hour or start over and until ascending to the last dimension (/g ascend 3 [nobuy])")
                .minRank(Rank.SUPER_STAFF)
                .callFunction((playerModel, strings) -> {
                    boolean nobuy = String.join(" ", strings).contains("nobuy");
                    boolean toAscend = strings.length > 0 && strings[0].equalsIgnoreCase("ascend");
                    boolean toIsland = strings.length > 0 && strings[0].equalsIgnoreCase("island");
                    AtomicInteger progressCount = new AtomicInteger(strings.length > 1 ? Integer.parseInt(strings[1]) : 1);
                    GamePlayer player = ((GamePlayer) playerModel);

                    Map<WorkerType, Integer> workerUpgrades = new HashMap<>();
                    AtomicInteger pickaxeUpgrades = new AtomicInteger();
                    AtomicReference<BigNumber> totalIncome = new AtomicReference<>(new BigNumber("0"));
                    AtomicReference<BigNumber> pickaxeIncome = new AtomicReference<>(new BigNumber("0"));

                    Runner.sync(0, 1, state -> {
                        if (player.isOffline()) {
                            state.cancel();
                            return;
                        }

                        if (state.getTicks() > 60 && !toAscend && !toIsland) {
                            playerModel.sendMessage("Done:", MessageType.INFO);

                            workerUpgrades.forEach((workerType, integer) -> playerModel.sendMessage(workerType + ": " + integer, MessageType.INFO));
                            playerModel.sendMessage("Pickaxe: " + pickaxeUpgrades.get(), MessageType.INFO);
                            playerModel.sendMessage(" ");
                            playerModel.sendMessage("Pickaxe income: " + pickaxeIncome.get().print(playerModel), MessageType.INFO);
                            playerModel.sendMessage("Workers income: " + totalIncome.get().print(playerModel), MessageType.INFO);

                            state.cancel();
                            return;
                        }

                        if ((toAscend || toIsland) && !player.getTutorial().isComplete()) {
                            player.getTutorialVillager().remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                            player.getTutorial().setComplete(true);
                            player.updateInventory();

                            player.createScoreboard();
                            player.updateScoreboard();
                        }

                        // add income
                        BigNumber income = player.getGoldRate().multiply(new BigNumber(toAscend || toIsland ? "12000" : "1200"));
                        player.addGold(income);
                        totalIncome.set(totalIncome.get().add(income));
                        // 150 clicks per minute, 9000 clicks in an hour
                        BigNumber pickaxeGold = player.getPickaxe().getIncome().multiply(new BigNumber(toAscend || toIsland ? "1500" : "150"));
                        player.addGold(pickaxeGold);
                        pickaxeIncome.set(pickaxeIncome.get().add(pickaxeGold));

                        if (!nobuy) {
                            // achievements
                            for (AchievementNode achievement : AchievementsService.i().getAchievements()) {
                                if (AchievementsService.i().awardAchievement(player, achievement)) {
                                    player.expSound();
                                }
                            }

                            // permanent multipliers
                            PermanentMultiplier nextMultiplier = PermanentMultiplier.next(player.getMultiplier());
                            if (nextMultiplier != null && player.chargeSchmepls(nextMultiplier.getCost())) {
                                player.setMultiplier(nextMultiplier);
                                player.recalculateGoldRate();
                            }

                            // skills
                            for (SkillType skillType : SkillType.values()) {
                                if (!SkillsService.i().has(player, skillType) && SkillsService.i().canUnlock(player, skillType) && player.chargeSchmepls(skillType.getCost())) {
                                    SkillsService.i().unlock(player, skillType);
                                }
                            }

                            // powerups
                            PowerupService.i().unlockNextLevel(player, PowerupCategory.PICKAXE);
                            if (PowerupService.i().getProgress(player, PowerupCategory.PICKAXE).getLevel() > 0) {
                                BigNumber gold = PowerupService.i().getGoldReward(player, PowerupCategory.PICKAXE, false);
                                double multiplier = SkillsService.i().has(player, SkillType.POWERUP_5) && Game.getRandom().nextDouble() <= 0.1 ? 5 : 1;
                                gold = gold.multiply(new BigNumber(multiplier));
                                player.addGold(gold);
                            }
                        }

                        // unlock islands
                        int islandId = player.getIslands().size();
                        List<IslandConfig> islands = player.getDimensionsData().getDimension().getIslands();
                        if (islandId < islands.size()) {
                            IslandConfig islandConfig = islands.get(islandId);
                            if (IslandsService.i().hasPrerequisites(player, islandConfig) && player.chargeGold(islandConfig.getBaseCost())) {
                                IslandsService.i().unlockIsland(player, islandId);
                                player.levelUpSound();

                                if (toIsland && progressCount.decrementAndGet() <= 0) {
                                    player.sendMessage("COMPLETE ", MessageType.INFO);
                                    player.sendMessage(Formatter.duration(state.getTicks() * 10, TimeUnit.MINUTES), MessageType.INFO);

                                    state.cancel();
                                    return;
                                }
                            }
                        }

                        // unlock buildings
                        for (IslandModel island : player.getIslands().values()) {
                            for (BuildingModel building : island.getBuildings()) {
                                if (building.canLevelUp()) {
                                    String prerequisite = building.getConfig().getUpgrades().get(building.getLevel()).checkPrerequisite(player);
                                    if (prerequisite == null && player.chargeGold(building.getConfig().getCosts().get(building.getLevel()))) {
                                        building.getConfig().getUpgrades().get(building.getLevel()).apply(player);
                                        building.upgrade();
                                    }
                                }
                            }
                        }

                        // Give it a chance to save up for island unlocks
                        if (state.getTicks() % 2 == 0) {
                            // buy workers
                            for (WorkerType workerType : Arrays.stream(WorkerType.values()).sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                                Worker worker = player.getWorkers().get(workerType);
                                if (worker == null) {
                                    WorkerConfiguration configuration = WorkersService.i().getConfigurations().get(workerType);
                                    if (player.chargeGold(configuration.getBaseCost())) {
                                        WorkersService.i().unlockWorker(player, workerType);
                                        worker = player.getWorkers().get(workerType);
                                    }
                                }
                                if (worker == null) continue;

                                long max = worker.maxCanBuy(-1);
                                if (max > 0) {
                                    worker.increaseLevel(max);
                                    player.chargeGold(worker.cost(max));

                                    workerUpgrades.compute(workerType, (type, count) -> Math.toIntExact((count == null ? 0 : count) + max));
                                }
                            }

                            // buy pickaxe
                            long maxPicks = player.getPickaxe().maxCanBuy(-1);
                            if (player.chargeGold(player.getPickaxe().cost(maxPicks))) {
                                player.getPickaxe().increaseLevel(maxPicks);
                                pickaxeUpgrades.getAndAdd((int) maxPicks);
                            }
                        }

                        // check if can ascend
                        if (toAscend && AscensionServices.i().hasMinimumGold(player)) {
                            List<DimensionConfig> dimensionList = DimensionConfig.getDimensionList();
                            int dimensionId = player.getDimensionsData().getCurrentDimensionId() + (progressCount.get() == 1 ? 0 : 1);
                            if (dimensionId >= dimensionList.size()) {
                                dimensionId = dimensionList.size() - 1;
                            }

                            AscensionServices.i().ascend(player, dimensionList.get(dimensionId));
                            player.getAscendRewards().forEach(r -> r.apply(player));
                            player.getAscendRewards().clear();

                            if (progressCount.decrementAndGet() == 0) {
                                player.sendMessage("COMPLETE ", MessageType.INFO);
                                player.sendMessage(Formatter.duration(state.getTicks() * 10, TimeUnit.MINUTES), MessageType.INFO);

                                state.cancel();
                            }
                        }
                    });

                    return ChatColor.GOLD + "Progressing " + (toAscend ? "until nth dimension" : toIsland ? "until nth island" : "for an hour") + "...";
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("gold")
                .description("Set your own gold amount")
                .minArgs(1)
                .minRank(Rank.SUPER_STAFF)
                .callFunction((data, strings) -> {
                    try {
                        BigNumber gold = new BigNumber(strings[0]);
                        ((GamePlayer) data).setGold(gold);
                        return "Set gold to " + gold.print(data);
                    } catch (Exception e) {
                        return "Invalid number. Try something like 10E+12";
                    }
                }).build());
        Commands.addCommand(Commands.Command.builder()
                .name("schmepls")
                .description("Set or add schmepls")
                .minArgs(2)
                .minRank(Rank.SUPER_STAFF)
                .callFunction((data, strings) -> {
                    boolean add = strings[0].equalsIgnoreCase("add");

                    long amount;
                    try {
                        amount = Long.parseLong(strings[1]);
                    } catch (Exception e) {
                        return "Invalid number. Try something like 42069";
                    }

                    if (strings.length > 2) {
                        Player pl = Bukkit.getPlayerExact(strings[2]);
                        if (pl != null) {
                            PlayersService.i().<GamePlayer>get(pl.getUniqueId(), p -> {
                                if (add) {
                                    p.addSchmepls(amount);
                                } else {
                                    p.setSchmepls(amount);
                                }
                                p.updateScoreboard();

                                data.sendMessage("Gave " + pl.getName() + " " + amount + " schmepls");
                            });
                        }
                    } else {
                        if (add) {
                            ((GamePlayer) data).addSchmepls(amount);
                        } else {
                            ((GamePlayer) data).setSchmepls(amount);
                        }
                        ((GamePlayer) data).updateScoreboard();
                    }

                    return null;
                }).build());

        Commands.addCommand(Commands.Command.builder()
                .name("track")
                .usage("[entity/task/report/clear]")
                .hidden(true)
                .minRank(Rank.DEV)
                .minArgs(1)
                .callFunction((PlayerModel, strings) -> {
                    GamePlayer player = ((GamePlayer) PlayerModel);

                    switch (strings[0].toLowerCase()) {
                        case "entity":
                            entityTracking.add(player);

                            return "Monitoring number of entities";
                        case "task":
                            taskTracking.add(player);

                            return "Monitoring number of tasks";
                        case "report":
                            StringBuilder builder = new StringBuilder();
                            List<Entity> entities = player.getPlayer().getWorld().getEntities();
                            builder.append(ChatColor.GRAY).append("Total entities: ").append(ChatColor.RED).append(entities.size());

                            Map<EntityType, List<Entity>> collect = entities.stream().collect(Collectors.groupingBy(Entity::getType));
                            collect.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().size()))
                                    .forEachOrdered(e -> {
                                        builder.append("\n  ").append(ChatColor.GRAY).append(e.getKey().toString())
                                                .append(": ").append(ChatColor.RED).append(e.getValue().size());
                                    });

                            int tasks = Bukkit.getScheduler().getPendingTasks().size();
                            builder.append("\n").append(ChatColor.GRAY).append("Total tasks: ").append(tasks);

                            return builder.toString();
                        case "clear":
                            entityTracking.remove(player);
                            taskTracking.remove(player);

                            LevelsService.i().updateExpBar(player);
                            return "Cleared debug monitoring";
                    }

                    return "Unknown command";
                })
                .build());
    }
}
