package net.mineclick.game.service;

import net.mineclick.game.Game;
import net.mineclick.game.menu.UpgradesMenu;
import net.mineclick.game.model.AscendReward;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.IncrementalModel;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.type.StatisticType;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.config.DimensionConfig;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.SingletonInit;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Comparator;
import java.util.function.Consumer;

@SingletonInit
public class AscensionServices {
    private static AscensionServices i;

    private AscensionServices() {
    }

    public static AscensionServices i() {
        return i == null ? i = new AscensionServices() : i;
    }

    /**
     * @param player The player
     * @return True if the player has the minimum required gold
     */
    public boolean hasMinimumGold(GamePlayer player) {
        DimensionConfig dimension = player.getDimensionsData().getDimension();
        return !player.getLifelongGold().smallerThan(dimension.getMinGold());
    }

    /**
     * Calculate the amount of schmepls a players would get upon ascension
     *
     * @param player The player
     * @return The amount of ascend schmepls
     */
    public long getAscendSchmepls(GamePlayer player) {
        if (!hasMinimumGold(player))
            return 0;

        double multiplier = player.getDimensionsData().getDimension().getMultiplier();
        if (SkillsService.i().has(player, SkillType.ASCENSION_6)) {
            multiplier *= 2;
        }
        return (long) ((getAscendPercent(player) * 1000 + 100) * multiplier);
    }

    /**
     * Calculate the amount of exp a players would get upon ascension
     *
     * @param player The player
     * @return The amount of ascend exp
     */
    public int getAscendExp(GamePlayer player) {
        if (!hasMinimumGold(player))
            return 0;

        double multiplier = player.getDimensionsData().getDimension().getMultiplier();
        if (SkillsService.i().has(player, SkillType.ASCENSION_6)) {
            multiplier *= 2;
        }
        return (int) (((getAscendPercent(player) * 100) + 15) * multiplier);
    }

    /**
     * Get the maximum number of geodes a players would get upon ascension
     *
     * @param player          The player
     * @param withRandomBonus Whether to include the random geodes bonus in number
     * @return The maximum amount of geodes
     */
    public int getAscendGeodesMax(GamePlayer player, boolean withRandomBonus) {
        if (!hasMinimumGold(player)) return 0;

        double multiplier = player.getDimensionsData().getDimension().getMultiplier();
        boolean isPaid = player.isRankAtLeast(Rank.PAID);
        if (isPaid) {
            multiplier *= 2;
        }

        return (int) (2 * multiplier) + (withRandomBonus ? isPaid ? 2 : 1 : 0);
    }

    public double getAscendPercent(GamePlayer player) {
        DimensionConfig dimension = player.getDimensionsData().getDimension();
        if (!hasMinimumGold(player))
            return 0;

        if (player.getLifelongGold().greaterThanOrEqual(dimension.getMaxGold()))
            return 1;

        double x = player.getLifelongGold().divide(dimension.getMaxGold()).doubleValue();
        if (x < 0)
            return 1;

        double f1 = Math.sin(Math.PI / 2 * x) / 2;
        double f2 = Math.pow(Math.cos(Math.PI / 2 * x), 10) / 2;

        return f1 - f2 + 0.5;
    }

    /**
     * Get the amount of gold required for ascension
     *
     * @param player The player
     * @return Amount of gold needed for ascension
     */
    public BigNumber getAscendRequiredGold(GamePlayer player) {
        BigNumber min = player.getDimensionsData().getDimension().getMinGold();
        return player.getLifelongGold().smallerThan(min) ? new BigNumber(min.subtract(player.getLifelongGold())) : BigNumber.ZERO;
    }

    /**
     * Open the ascend menu
     *
     * @param player The player
     */
    public void openAscendMenu(GamePlayer player, Consumer<GamePlayer> closeCallback) {
        InventoryUI menu = new InventoryUI("          MineClick Ascend", 27);

        int index = 11;
        for (DimensionConfig d : DimensionConfig.getDimensionList()) {
            ItemUI itemUI = new ItemUI(d.getSkin(), clickPack -> {
                boolean isAdmin = player.isRankAtLeast(Rank.SUPER_STAFF);

                if (!canAscendIn(player, d) && !isAdmin) {
                    player.sendMessage("This dimension is locked", MessageType.ERROR);
                } else if (!hasMinimumGold(player) && player.getDimensionsData().getAscensionsIn(d.getId()) <= 0 && !isAdmin) {
                    player.sendMessage(ChatColor.GRAY + "You need to make " + getAscendRequiredGold(player).print(player) + ChatColor.GRAY + " more gold to ascend");
                } else {
                    MenuUtil.openConfirmationMenu(player, confirm ->
                            {
                                if (confirm) {
                                    ascend(player, d);
                                }
                            },
                            "You will get " + ChatColor.GREEN + "+" + ChatColor.AQUA + getAscendSchmepls(player) + ChatColor.GRAY + " schmepls, " + ChatColor.GREEN + "+" + ChatColor.AQUA + getAscendExp(player) + ChatColor.GRAY + " EXP",
                            "and " + ChatColor.AQUA + "+" + (hasMinimumGold(player) ? "1 to " : "") + getAscendGeodesMax(player, true) + ChatColor.GRAY + " geodes",
                            " ",
                            "However, your gold, pickaxe, workers,",
                            "islands and buildings will reset"
                    );
                }
            });
            itemUI.setUpdateConsumer(item -> {
                item.setTitle(ChatColor.YELLOW + d.getName() + (d.equals(player.getDimensionsData().getDimension()) ? ChatColor.DARK_GREEN + " - current dimension" : ""));
                item.setLore(ChatColor.GRAY + d.getDescription());
                item.addLore(" ");
                if (!canAscendIn(player, d)) {
                    item.addLore(ChatColor.RED + "Dimension is locked!");
                    item.addLore(ChatColor.GRAY + "Ascend in the " + DimensionConfig.getDimensionList().get(d.getPreDimension()).getName() + " " + (d.getPreAscends() == 1 ? "once" : d.getPreAscends() + " times"));
                    item.addLore(ChatColor.GRAY + "to unlock this dimension");
                } else if (hasMinimumGold(player)) {
                    item.addLore(ChatColor.GOLD + "Click to ascend!");
                    item.addLore(ChatColor.GREEN + "+" + getAscendSchmepls(player) + ChatColor.YELLOW + " schmepls");
                    item.addLore(ChatColor.GREEN + "+" + getAscendExp(player) + ChatColor.YELLOW + " EXP");
                    item.addLore(ChatColor.GREEN + "+" + (hasMinimumGold(player) ? "1 to " : "") + getAscendGeodesMax(player, true) + ChatColor.YELLOW + " geodes");
                    item.addLore(" ");
                    item.addLore(ChatColor.GRAY + "You've made " + player.getLifelongGold().print(player) + ChatColor.GRAY + " gold so far");
                    item.addLore(ChatColor.GRAY + "More gold means more schmepls and EXP");
                } else if (player.getDimensionsData().getAscensionsIn(d.getId()) > 0) {
                    item.addLore(ChatColor.RED + "You need more gold");
                    item.addLore(ChatColor.RED + "to receive ascension rewards.");
                    item.addLore(ChatColor.RED + "You can still ascend to this dimension,");
                    item.addLore(ChatColor.RED + "but you won't get any rewards!");
                } else {
                    item.addLore(ChatColor.RED + "You need more gold to ascend");
                }

                item.addLore(" ");
                item.addLore(ChatColor.GRAY + "Min gold to ascend: " + d.getMinGold().print(player));
                item.addLore(ChatColor.GRAY + "Extra ascend reward: " + ChatColor.GREEN + "x" + ChatColor.YELLOW + Formatter.format(d.getMultiplier()));
                item.addLore(" ");
                item.addLore(ChatColor.GRAY + "Number of ascends: " + ChatColor.BOLD + player.getDimensionsData().getAscensionsIn(d.getId()));
            });
            menu.setItem(index++, itemUI);
        }

        for (int i = index; i < 16; i++) {
            menu.setItem(i, MenuUtil.setLockedSkull(new ItemUI(Material.PLAYER_HEAD, clickPack -> {
            }), "More dimensions coming soon!"));
        }

        menu.setItem(0, MenuUtil.getCloseMenu(closeCallback));
        menu.open(player.getPlayer());
    }

    private boolean canAscendIn(GamePlayer player, DimensionConfig dimension) {
        if (player.getDimensionsData().getDimension().equals(dimension) || player.getDimensionsData().getAscensionsIn(dimension.getId()) > 0)
            return true;

        return dimension.getPreDimension() <= 0
                || player.getDimensionsData().getAscensionsIn(dimension.getPreDimension()) >= dimension.getPreAscends();
    }

    public void ascend(GamePlayer player, DimensionConfig dimension) {
        long currentTime = System.currentTimeMillis();
        if (!player.isRankAtLeast(Rank.SUPER_STAFF) && currentTime - player.getLastAscendAt() < 60000) {
            player.sendMessage("Please wait " + Formatter.duration(60000 - (currentTime - player.getLastAscendAt())) + " to ascend again", MessageType.ERROR);
            return;
        }

        long ascensionStartTime = player.getLastAscendAt();
        player.setLastAscendAt(currentTime);

        player.sendImportantMessage(
                "You have ascended!",
                "Welcome to the " + dimension.getName() + " dimension"
        );

        // apply any uncollected ascend rewards
        player.getAscendRewards().forEach(ascendReward -> ascendReward.apply(player));
        player.getAscendRewards().clear();

        // rewards
        if (hasMinimumGold(player)) {
            long speedrunChallengeScore = getSpeedrunChallengeScore(player, currentTime, ascensionStartTime);

            player.getAscendRewards().add(new AscendReward(AscendReward.Type.SCHMEPLS, getAscendSchmepls(player)));
            player.getAscendRewards().add(new AscendReward(AscendReward.Type.EXP, getAscendExp(player)));
            int geodes = Game.getRandom().nextInt(getAscendGeodesMax(player, false)) + 1;
            player.getAscendRewards().add(new AscendReward(AscendReward.Type.GEODES, geodes));

            applyAchievements(player, speedrunChallengeScore);
            StatisticsService.i().increment(player.getUuid(), StatisticType.ASCENDS);

            player.getDimensionsData().incrementCurrentAscensions();
        }
        player.getActivityData().setEverClicked(false);

        // dimensions stuff
        player.getDimensionsData().setCurrentDimensionId(dimension.getId());

        // fix any menu issues
        player.getUpgradesMenu().destroy();
        player.setUpgradesMenu(new UpgradesMenu(player));

        // islands
        player.getIslands().values().forEach(IslandModel::clear);
        player.getIslands().clear();
        player.setCurrentIslandId(0);
        IslandsService.i().loadPlayerIslands(player);

        // parkour
        player.getParkour().reset();
        player.getParkour().getCompletedIslands().clear();

        // gold
        player.setGold(BigNumber.ZERO);
        player.setGoldRate(BigNumber.ZERO);
        player.setLifelongGold(BigNumber.ZERO);
        player.setUncollectedVaultsGold(null);

        // workers
        player.getWorkers().values().forEach(Worker::clear);
        player.getWorkers().clear();

        // add any workers from skills
        if (SkillsService.i().has(player, SkillType.ASCENSION_1)) {
            WorkersService.i().unlockWorker(player, WorkerType.ZOMBIE);
        }
        if (SkillsService.i().has(player, SkillType.ASCENSION_4)) {
            WorkersService.i().unlockWorker(player, WorkerType.SKELETON);
        }
        if (SkillsService.i().has(player, SkillType.ASCENSION_5)) {
            WorkersService.i().unlockWorker(player, WorkerType.SPIDER);
        }

        // trader
        player.getTrader().reset();

        // pickaxe
        String pickaxeId = player.getPickaxe().getId();
        player.setPickaxe(null);
        PickaxeService.i().loadPlayerPickaxe(player);
        PickaxeService.i().upgradePickaxe(player, pickaxeId);

        // super block and pickaxe powerup
        player.getPickaxePowerup().setCharge(0.001);
        player.getPickaxePowerup().setActivatedDischargeRate(0);
        player.getSuperBlockData().setChance(0);

        // add pickaxes from skills
        if (SkillsService.i().has(player, SkillType.ASCENSION_2)) {
            player.getPickaxe().setAmount(3);
            player.getPickaxe().updateItem();
        }

        // add gold from skills
        if (SkillsService.i().has(player, SkillType.ASCENSION_3)) {
            player.addGold(new BigNumber("10000"));
        }

        PlayersService.i().save(player);
        player.tpToIsland(player.getCurrentIsland(), true);
        player.recalculateGoldRate();

        player.schedule(40, () -> QuestsService.i().incrementProgress(player, "dailyAscend", 0, 1));
    }

    private void applyAchievements(GamePlayer player, long speedrunChallengeScore) {
        //Pickaxe challenge
        if (player.getPickaxe().getLevel() <= 2 && player.getPickaxe().getAmount() <= (SkillsService.i().has(player, SkillType.ASCENSION_2) ? 3 : 1)) {
            AchievementsService.i().setProgress(player, "pickaxeChallenge", 1);
        }

        //Workers challenge
        long maxLevel = player.getWorkers().values().stream()
                .map(IncrementalModel::getLevel)
                .max(Comparator.comparingLong(level -> level))
                .orElse(0L);
        int workerProgress = maxLevel <= 25 ? 3 : maxLevel <= 50 ? 2 : maxLevel <= 100 ? 1 : 0;
        if (workerProgress > 0) {
            AchievementsService.i().setProgress(player, "workersChallenge", maxLevel);
        }

        //Never click
        if (!player.getActivityData().isEverClicked()) {
            AchievementsService.i().setProgress(player, "noclicking", 1);
        }

        //Speedrun challenge
        if (speedrunChallengeScore > 0) {
            AchievementsService.i().setProgress(player, "speedrunChallenge", speedrunChallengeScore);
        }
    }

    private long getSpeedrunChallengeScore(GamePlayer player, long currentTime, long ascensionStartTime) {
        long millisDifference = currentTime - ascensionStartTime;

        long speedrunChallengeScore;
        if (millisDifference <= 120_000) { // 2 minutes
            speedrunChallengeScore = 5;
        } else if (millisDifference <= 1800_000) { // 30 minutes
            speedrunChallengeScore = 4;
        } else if (millisDifference <= 3600_000) { // 1 hour
            speedrunChallengeScore = 3;
        } else if (millisDifference <= 36000_000) { // 10 hours
            speedrunChallengeScore = 2;
        } else if (millisDifference <= 86400_000) { // 24 hours
            speedrunChallengeScore = 1;
        } else {
            speedrunChallengeScore = 0;
        }

        return Math.max(speedrunChallengeScore, AchievementsService.i().getProgress(player, "speedrunChallenge"));
    }
}
