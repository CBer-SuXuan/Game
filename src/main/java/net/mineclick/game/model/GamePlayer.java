package net.mineclick.game.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.mineclick.game.Game;
import net.mineclick.game.menu.*;
import net.mineclick.game.minigames.spleef.SpleefService;
import net.mineclick.game.model.achievement.AchievementProgress;
import net.mineclick.game.model.pickaxe.Pickaxe;
import net.mineclick.game.model.pickaxe.PickaxePowerup;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.model.worker.WorkerConfiguration;
import net.mineclick.game.service.*;
import net.mineclick.game.type.*;
import net.mineclick.game.type.powerup.PowerupCategory;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.type.quest.Quest;
import net.mineclick.game.type.quest.villager.*;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.game.util.TutorialVillager;
import net.mineclick.game.util.Vault;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.game.util.visual.GoldBat;
import net.mineclick.game.util.visual.SwingAnimation;
import net.mineclick.global.config.field.MineBlock;
import net.mineclick.global.config.field.MineRegionConfig;
import net.mineclick.global.model.PlayerModel;
import net.mineclick.global.service.ChatService;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.*;
import net.mineclick.global.util.location.RandomVector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Setter
@Getter
@NoArgsConstructor
public class GamePlayer extends PlayerModel {
    private Map<String, AchievementProgress> achievements = new HashMap<>();
    private BigNumber gold = BigNumber.ZERO;
    private BigNumber goldRate = BigNumber.ZERO;
    private BigNumber lifelongGold = BigNumber.ZERO;
    private long schmepls = 0;
    private long exp = 0;
    private TutorialData tutorial = new TutorialData();
    private ParkourData parkour = new ParkourData();
    private Pickaxe pickaxe = new Pickaxe();
    private int powerupParts = 0;
    private Map<PowerupCategory, PowerupProgress> powerupsProgress = new HashMap<>();
    private Set<PowerupType> unlockedPowerups = new HashSet<>();
    private PickaxePowerup pickaxePowerup = new PickaxePowerup();
    private Map<WorkerType, Worker> workers = new HashMap<>();
    private Map<BoosterType, Integer> boosters = new HashMap<>();
    private LobbyData lobbyData = new LobbyData();
    private DailyRewardChest dailyRewardChest = new DailyRewardChest();
    private PlayerSettingsData playerSettings = new PlayerSettingsData();
    private DimensionsData dimensionsData = new DimensionsData();
    private Map<Integer, IslandModel> islands = new HashMap<>();
    private int currentIslandId = 0;
    private StaffData staffData = new StaffData();
    private PlayerActivityData activityData = new PlayerActivityData();
    private SuperBlockData superBlockData = new SuperBlockData();
    private PlayerPendingData pendingData = new PlayerPendingData();
    private PermanentMultiplier multiplier = PermanentMultiplier.NONE;
    private Set<SkillType> skills = new HashSet<>();
    private Map<String, Long> notify = new HashMap<>(); // do not rename, used by ender
    private List<AscendReward> ascendRewards = new ArrayList<>();
    private Map<Rarity, Integer> geodes = new HashMap<>();
    private int cookies = 0;
    private long cookieLastGivenAt = 0;
    private long batNextSpawnAt = 0;
    private Trader trader = new Trader();
    private List<Location> treasureLocations = new ArrayList<>();
    private long lastThankedAt = 0;
    private Mineshaft mineshaft = new Mineshaft();
    private Map<String, QuestProgress> quests = new HashMap<>();
    private List<String> dailyQuests = new ArrayList<>();
    private int unlockedDailyQuests = 3;
    private QuestsData questsData = new QuestsData();
    private long lastAscendAt = 0;
    private int dailyStreak = 0;
    private Instant lastDailyStreakLogin = Instant.now().truncatedTo(ChronoUnit.DAYS);

    private transient Set<Integer> allowedEntities = new HashSet<>();
    private transient Set<com.comphenix.protocol.wrappers.BlockPosition> allowedBlockChanges = new HashSet<>();
    private transient IslandModel visitingIsland;
    private transient BigNumber uncollectedVaultsGold;
    private transient ResponsiveScoreboard scoreboard;
    private transient FriendsMenu friendsMenu;
    private transient AchievementsMenu achievementsMenu;
    private transient SettingsMenu settingsMenu;
    private transient MainMenu mainMenu;
    private transient DiscordMenu discordMenu;
    private transient UpgradesMenu upgradesMenu;
    private transient SkillsMenu skillsMenu;
    private transient TutorialVillager tutorialVillager;
    private transient boolean afkVaultsFilled;
    private transient Block lastClickedBlock;
    private transient AtomicInteger ignoreClicks = new AtomicInteger();
    private transient byte[] bodyRender = null;
    private transient boolean autoUpgradeEnabled;
    private transient long lastHologramUpdatedAt = 0;
    private transient List<Bat> goldBats = new ArrayList<>();
    private transient int arrows = 0;
    private transient Map<Block, DynamicMineBlock> dynamicMineBlocks = new HashMap<>(); // TODO this is not being saved currently
    private transient boolean openingGeode;
    private transient double achievementGoldMultiplier = 1;

    @Override
    public Map<String, String> getChatHoverInfo() {
        HashMap<String, String> map = new HashMap<>();
        map.put("Level", LevelsService.i().getLevel(exp) + "");
        map.put("Dimension", dimensionsData.getDimension().getName());

        return map;
    }

    @Override
    protected void onLoad() {
        StatisticsService.i().load(getUuid());
        PickaxeService.i().loadPlayerPickaxe(this);
        IslandsService.i().loadPlayerIslands(this);
        WorkersService.i().loadPlayerWorkers(this);
        PowerupService.i().loadPlayerPowerups(this);
        QuestsService.i().loadPlayerQuests(this);

        friendsMenu = new FriendsMenu(this);
        achievementsMenu = new AchievementsMenu(this);
        settingsMenu = new SettingsMenu(this);
        mainMenu = new MainMenu(this);
        discordMenu = new DiscordMenu(this);
        upgradesMenu = new UpgradesMenu(this);
        skillsMenu = new SkillsMenu(this);

        pickaxePowerup.update(this);
        trader.setPlayer(this);
        mineshaft.setPlayer(this);
        parkour.setPlayer(this);

        // clean up any null values in collections (and maps)
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                boolean isMap = Map.class.isAssignableFrom(field.getType());
                if (isMap || Collection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);

                    Object object = field.get(this);
                    if (isMap) {
                        object = ((Map) object).values();
                    }
                    ((Collection) object).removeIf(Objects::isNull);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onJoin(boolean networkWide) {
        if (getName().equals("JeffClick")) {
            setRank(Rank.DEV);
        }

        if (networkWide) {
            activityData.setLastLocation(null);
            activityData.setSameLocationSeconds(0);
            activityData.setAfk(false);
            activityData.setAfkTime(0);
        }

        PlayersService.i().hideFromAll(this);
        getPlayer().setGameMode(GameMode.ADVENTURE);

        // Process pending data (offline votes, etc)
        schedule(20, () -> PlayerPendingDataService.i().process(this));

        // Process any pending transaction
        schedule(40, () -> TransactionsService.i().checkTransactions(this));

        // Teleport to lobby or a player if on join data is present
        if (getOnJoinData().isTpToLobby()) {
            LobbyService.i().spawn(this);
            getOnJoinData().setTpToLobby(false);
        } else if (getOnJoinData().getTpToPlayer() != null) {
            Player player = Bukkit.getPlayerExact(getOnJoinData().getTpToPlayer());
            if (player != null) {
                PlayersService.i().hideFromAll(this);
                getPlayer().teleport(player);
            } else {
                sendMessage("Cannot find player " + getOnJoinData().getTpToPlayer(), MessageType.ERROR);
                tpToIsland(getCurrentIsland(), true);
            }

            if (isRankAtLeast(Rank.STAFF)) {
                getPlayer().setAllowFlight(true);
            }
            getOnJoinData().setTpToPlayer(null);
        } else if (getOnJoinData().getVisitPlayer() != null) {
            Player player = Bukkit.getPlayerExact(getOnJoinData().getVisitPlayer());
            if (player != null) {
                PlayersService.i()
                        .<GamePlayer>get(
                                player.getUniqueId(),
                                playerModel -> IslandsService.i().visitPlayer(this, playerModel)
                        )
                        .ifNull(() -> {
                            sendMessage("Cannot find player " + getOnJoinData().getVisitPlayer(), MessageType.ERROR);
                            tpToIsland(getCurrentIsland(), true);
                        });
            } else {
                sendMessage("Cannot find player " + getOnJoinData().getVisitPlayer(), MessageType.ERROR);
                tpToIsland(getCurrentIsland(), true);
            }

            getOnJoinData().setVisitPlayer(null);
        } else {
            // otherwise just tp to island
            tpToIsland(getCurrentIsland(), true);
        }

        // Send join message
        if (networkWide) {
            long duration = Duration.between(activityData.getLastOnlineAt(), Instant.now()).toMillis();
            if (duration > 15000) {
                // reset worker cookies if absent for more than 15 seconds
                workers.values().forEach(worker -> worker.setExcitedTicks(0));

                // send message
                sendImportantMessage(
                        "Welcome back to MineClick!",
                        "You were absent for " + Formatter.roundedTime(duration)
                );

                // Broadcast join
                if (isRankAtLeast(Rank.PAID)) {
                    boolean disabled = getGameSettings().getJoinMsgDisabled().get();
                    if (!disabled) {
                        ChatService.i().sendBroadcast(getRank().getChatPrefix() + getName() + ChatColor.GOLD + " joined", null, false);
                    }

                    TextComponent text = new TextComponent(ChatColor.GRAY + "[Click here to " + (disabled ? "enable" : "disable") + " join announcement]");
                    text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to " + (disabled ? "enable" : "disable") + " your\njoin announcement").color(net.md_5.bungee.api.ChatColor.DARK_AQUA).create()));
                    text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/joinmsg"));
                    getPlayer().spigot().sendMessage(text);
                }

                // Vaults
                if (goldRate.greaterThan(BigNumber.ZERO)) {
                    schedule(40, () -> Vault.spawn(this, getCurrentIsland().getConfig().getSpawn().toLocation().clone(), duration));
                }
            } else {
                sendImportantMessage("Welcome to MineClick!", null);
            }
        }

        // Scoreboard
        if (tutorial.isComplete() || tutorial.isShowScoreboard()) {
            createScoreboard();
            updateScoreboard();
        }

        //Compile full body render
        Runner.async(() -> {
            try {
                URL url = new URL("https://crafatar.com/avatars/" + getUuid() + "?size=80");
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(20000);
                InputStream is = null;
                try {
                    is = connection.getInputStream();
                } catch (IOException ioe) {
                    if (connection instanceof HttpURLConnection) {
                        HttpURLConnection httpConn = (HttpURLConnection) connection;
                        int statusCode = httpConn.getResponseCode();
                        if (statusCode != 200) {
                            is = httpConn.getErrorStream();
                        }
                    }
                }

                if (is != null) {
                    bodyRender = MapPalette.imageToBytes(ImageIO.read(is));
                }
            } catch (Exception e) {
                Game.i().getLogger().log(Level.SEVERE, "Error loading player's body render (" + getUuid() + ")", e);
            }
        });

        // Announcements
        schedule(200, () -> {
            if (!getFriendsData().getReceivedRequests().isEmpty()) {
                int received = getFriendsData().getReceivedRequests().size();
                sendImportantMessage("You have " + ChatColor.GREEN + received + ChatColor.YELLOW + " friend request" + (received > 1 ? "s" : ""), "See the Main Menu");
            }

            if (AchievementsService.i().hasUncollected(this)) {
                sendImportantMessage("You have uncollected achievements", "See the Main Menu");
            }
        });

        updateInventory();
        LevelsService.i().updateExpBar(this);
        DailyServices.i().updateDailyStreak(this);

        discordMenu.checkSettings();
    }

    public void createScoreboard() {
        if (scoreboard == null) {
            scoreboard = new ResponsiveScoreboard(this);
        }
    }

    @Override
    public void onQuit() {
        pickaxePowerup.removeBossBar();
        DynamicMineBlocksService.i().clear(this, false);

        if (uncollectedVaultsGold != null) {
            addGold(uncollectedVaultsGold);
            uncollectedVaultsGold = null;
        }

        if (scoreboard != null) {
            scoreboard.removeAll();
            scoreboard.delete();
        }

        activityData.setLastOnlineAt(Instant.now());
        workers.values().forEach(Worker::clear);
        islands.values().forEach(IslandModel::clear);

        // patch up the tutorial restarting exploit
        if (!tutorial.isComplete()) {
            pickaxe.setLevel(1);
            workers.clear();
            islands.values().forEach(islandModel -> islandModel.getBuildings().clear());
            gold = new BigNumber("0");
        }

        updateNotify();
    }

    private void updateNotify() {
        // notify on discord of the reward chest and vaults
        if (playerSettings.getDiscordReward().get() && !dailyRewardChest.isRefreshed()) {
            notify.put("discordReward", dailyRewardChest.getRefreshAt().toEpochMilli());
        }

        long vaultsFillAt = System.currentTimeMillis() + (getVaults() * 60 * 60 * 1000) - (activityData.getAfkTime() * 1000);
        if (playerSettings.getDiscordVaults().get() && vaultsFillAt > System.currentTimeMillis()) {
            notify.put("discordVaults", vaultsFillAt);
        }
    }

    @Override
    public void saveTpState(boolean serverShutdown) {
        if (!serverShutdown) {
            StatisticsService.i().flush(getUuid());
        } else if (visitingIsland == null) {
            getOnJoinData().setLastLocation(getPlayer().getLocation());
        }
        activityData.setLastOnlineAt(Instant.now());

        if (mineshaft.isStarted() && MineshaftService.i().isInMineshaft(this) && serverShutdown) {
            mineshaft.setServerShutdown(true);
        }
    }

    public void addGold(BigNumber amount) {
        gold = gold.add(amount);

        if (amount.greaterThan(BigNumber.ZERO)) {
            lifelongGold = lifelongGold.add(amount);
        }
    }

    public boolean chargeGold(BigNumber gold) {
        if (this.gold.smallerThan(gold))
            return false;

        addGold(new BigNumber(gold.negate()));
        return true;
    }

    public void addExp(long amount) {
        int oldLevel = LevelsService.i().getLevel(exp);

        exp += amount;
        StatisticsService.i().increment(getUuid(), StatisticType.EXP, amount);
        LevelsService.i().updateExpBar(this);

        int newLevel = LevelsService.i().getLevel(exp);
        if (newLevel > oldLevel) {
            levelUpSound();
            sendImportantMessage("Level UP!", "You are now level " + ChatColor.YELLOW + newLevel);
        }
    }

    public void addSchmepls(long amount) {
        schmepls += amount;
        updateScoreboard();
    }

    public boolean chargeSchmepls(long amount) {
        if (schmepls >= amount) {
            schmepls -= amount;
            updateScoreboard();
            return true;
        }

        return false;
    }

    public void onItemDrop(ItemStack itemStack) {
        if (pickaxe != null && itemStack.getType().equals(pickaxe.getConfiguration().getMaterial())) {
            ignoreClicks.incrementAndGet();
        } else {
            updateInventory();
        }
    }

    public void updateInventory() {
        if (isOffline())
            return;

        if (SpleefService.i().isInArena(getPlayer().getLocation())) {
            SpleefService.i().updateInventory(this);
            return;
        }

        Player player = getPlayer();
        player.getInventory().clear();
        pickaxe.updateItem();
        if (MineshaftService.i().isInMineshaft(this)) {
            MineshaftService.i().updateInventory(this);
            return;
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!effect.getType().equals(PotionEffectType.BLINDNESS)) {
                player.removePotionEffect(effect.getType());
            }
        }
        player.setHealth(20);

        updateCookiesItem();
        updateMenuItems();

        // Treasure map or parkour item
        if (!treasureLocations.isEmpty() && isOnOwnIsland()) {
            // TODO this is probably a memory leak issue, I don't think those maps get garbage collected at all
            Location treasureLocation = treasureLocations.get(0);
            ServerLevel worldServer = ((CraftWorld) treasureLocation.getWorld()).getHandle();
            net.minecraft.world.item.ItemStack stack = MapItem.create(worldServer, treasureLocation.getBlockX(), treasureLocation.getBlockZ(), (byte) 0, true, true);

            int mapId = stack.getTag().getInt("map");
            MapView mapView = Bukkit.getMap(mapId);
            if (mapView != null) {
                mapView.setCenterX(treasureLocation.getBlockX());
                mapView.setCenterZ(treasureLocation.getBlockZ());
            }

            MapItem.renderBiomePreviewMap(worldServer, stack);
            for (Location location : treasureLocations) {
                MapItemSavedData.addTargetDecoration(stack, new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), "+", MapDecoration.Type.byIcon((byte) 26));
            }

            ItemStack map = CraftItemStack.asBukkitCopy(stack);

            getPlayer().getInventory().setItemInOffHand(map);

            ItemStack shovel = ItemBuilder.builder().title(ChatColor.YELLOW + "Wooden Shovel" + ChatColor.GRAY + " - right-click to dig").material(Material.WOODEN_SHOVEL).build().toItem();
            getPlayer().getInventory().setItem(0, shovel);
        } else {
            updateParkourItem();
        }

        LobbyService.i().updateInventory(this);
        if (visitingIsland != null) {
            player.getInventory().setItem(0, IslandsService.i().getVisitingItem());
        }

        //Staff tool
        if (isRankAtLeast(Rank.STAFF) && !staffData.isHideTool()) {
            ItemBuilder.ItemBuilderBuilder builder = ItemBuilder.builder()
                    .material(Material.STICK)
                    .title(ChatColor.GREEN + "[S] " + ChatColor.YELLOW + "Staff Tool " + ChatColor.GRAY + "/st");

            staffData.getStaffTools().forEach((trigger, tool) ->
                    builder.lore(ChatColor.GRAY + trigger.getName() + ": " + ChatColor.YELLOW + tool.getName()));
            player.getInventory().setItem(2, builder.build().toItem());
        }

        // Bow and arrows
        boolean hasBow = false;
        if (SkillsService.i().has(this, SkillType.BAT_1) && arrows > 0 && goldBats.stream().anyMatch(Bat::isAlive)) {
            ItemBuilder.ItemBuilderBuilder rangeWeaponBuilder = ItemBuilder.builder()
                    .title(ChatColor.YELLOW + "Bat catcher" + ChatColor.GRAY + " - " + arrows + " arrows left");

            ItemStack rangeWeapon;
            // TODO: fix crossbow and change the BAT_3 skill
//            if (!SkillsService.i().has(this, SkillType.BAT_3)) {
            rangeWeapon = rangeWeaponBuilder.material(Material.BOW)
                    .lore(ChatColor.GRAY + "Use this bow to catch bats")
                    .build().toItem();
//            }
//            else {
//                rangeWeapon = rangeWeaponBuilder.material(Material.CROSSBOW)
//                        .lore(ChatColor.GRAY + "Use this crossbow to catch bats")
//                        .build().toItem();
//
//                rangeWeapon.addEnchantment(Enchantment.QUICK_CHARGE, 2);
//                rangeWeapon.addEnchantment(Enchantment.MULTISHOT, 1);
//            }

            player.getInventory().setItem(1, rangeWeapon);
            player.getInventory().setItem(9, new ItemStack(Material.ARROW, arrows));

            hasBow = true;
        }

        // Running shoes
        boolean shoesInInv = false;
        if (parkour.isShoesUnlocked()) {
            boolean removed = parkour.isShoesRemoved();
            if (removed) {
                shoesInInv = true;
            }

            if (!removed) {
                player.getInventory().setBoots(ParkourService.RUNNING_SHOES);
            } else {
                player.getInventory().setItem(hasBow ? 10 : 9, ParkourService.RUNNING_SHOES);
            }

            if (!removed) {
                getPlayer().setWalkSpeed(0.4F);
            } else {
                getPlayer().setWalkSpeed(0.2F);
            }
        }

        // Elytra
        if (parkour.isElytraUnlocked()) {
            boolean removed = parkour.isElytraRemoved();

            if (!removed) {
                player.getInventory().setChestplate(ParkourService.ELYTRA);
            } else {
                int pos = 9;
                if (hasBow) {
                    pos++;
                }
                if (shoesInInv) {
                    pos++;
                }
                player.getInventory().setItem(pos, ParkourService.ELYTRA);
            }
        }
    }

    /**
     * Get the player's current island or the one they are currently visiting
     *
     * @return Player's current island (not necessary the highest level unlocked island)
     */
    public IslandModel getCurrentIsland() {
        return getCurrentIsland(true);
    }

    /**
     * Get the player's current island or the one they are currently visiting
     *
     * @param checkVisiting Whether to return the visiting island (if any) or not
     * @return Player's current island (not necessary the highest level unlocked island)
     */
    public IslandModel getCurrentIsland(boolean checkVisiting) {
        if (checkVisiting && visitingIsland != null) {
            return visitingIsland;
        }

        return currentIslandId >= islands.size() ? islands.get(islands.size() - 1) : islands.get(currentIslandId);
    }

    /**
     * @return True if the player is on their own island and not visiting, in the lobby, mineshaft or anywhere else
     */
    public boolean isOnOwnIsland() {
        if (visitingIsland != null) return false;

        Player player = getPlayer();
        if (player == null) {
            return false;
        }
        Location loc = player.getLocation();
        Location spawn = getCurrentIsland(false).getConfig().getSpawn().toLocation();

        return loc.getX() <= spawn.getX() + 500 && loc.getX() >= spawn.getX() - 500
                && loc.getZ() <= spawn.getZ() + 500 && loc.getZ() >= spawn.getZ() - 500;
    }

    public void visitIsland(IslandModel island) {
        if (visitingIsland == island) {
            sendMessage("You are already visiting this player", MessageType.ERROR);
            return;
        }

        sendMessage("Visiting " + island.getPlayer().getName() + "'s island", MessageType.INFO);
        island.getPlayer().sendMessage(getRank().getPrefix() + getName() + ChatColor.YELLOW + " is visiting your island", MessageType.INFO);

        tpToIsland(island, true);
    }

    public void removeVisitingIsland() {
        if (visitingIsland == null) return;

        visitingIsland.removeVisitor(this);
        visitingIsland.getPlayer().getWorkers().values().forEach(w -> w.removeFor(this));
        visitingIsland = null;

        upgradesMenu.setVisiting(false);
    }

    public void tpToIsland(IslandModel island, boolean reset) {
        if (isOffline())
            return;

        DynamicMineBlocksService.i().clear(this, DynamicMineBlockType.MINESHAFT, false);
        DynamicMineBlocksService.i().clear(this, DynamicMineBlockType.LOBBY_CRIMSON, false);

        IslandModel currentIsland = getCurrentIsland();
        boolean visiting = !this.equals(island.getPlayer());

        // Update gamemode, time and stuff like that which is common whether you visit your own or a friends island
        Player player = getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setPlayerTime(island.getConfig().isNightTime() ? 18000 : 6000, false);

        if (isRankAtLeast(Rank.STAFF) || (isRankAtLeast(Rank.YOUTUBER) && playerSettings.getFlight().get())) {
            player.setAllowFlight(true);
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        getParkour().reset();
        recalculateGoldRate();

        // Tp to spawn (or the last location on server restarts)
        Location spawn = island.getConfig().getSpawn().toLocation();
        if (getOnJoinData().getLastLocation() != null && activityData.getLastOnlineAt().plus(10, ChronoUnit.SECONDS).isAfter(Instant.now())) {
            spawn = getOnJoinData().getLastLocation();
        }
        getOnJoinData().setLastLocation(null);
        player.teleport(spawn);

        upgradesMenu.setVisiting(visiting);
        // if teleported to the same island (jumped in void or clicked from the menu), do not respawn anything
        if (island == currentIsland && !reset)
            return;

        // clean up the last visited island
        removeVisitingIsland();

        // add the visitor if necessary or update the current island id
        if (visiting) {
            visitingIsland = island;
            visitingIsland.getVisitors().add(this);
        } else {
            currentIslandId = island.getId();
        }

        // clear the island we came from
        if (currentIsland.getPlayer().equals(this)) {
            trader.despawn();
            currentIsland.clear();
            workers.values().forEach(Worker::clear);

            // teleport your island visitors with you
            if (!visiting) {
                // wrapping in a new list to avoid concurrent modification exception as the collection will be modified in visitIsland
                new ArrayList<>(currentIsland.getVisitors()).forEach(p -> p.tpToIsland(island, false));
            }
        }

        // hide/show players
        PlayersService.i().hideFromAll(this, island.getAllPlayers());

        // blindness effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 1000000, 0, true, false));
        updateInventory();

        schedule(20, () -> {
            if (isOffline()) return;
            getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);

            if (visiting) {
                island.update();
                island.getPlayer().getWorkers().values().forEach(w -> w.showFor(this));
            } else {
                // spawn workers
                int j = 0;
                for (Worker worker : workers.values()) {
                    schedule(j * 2, () -> {
                        if (!isOnOwnIsland()) {
                            return;
                        }

                        worker.spawn(island);
                    });
                    j++;
                }
                island.update();

                if (!tutorial.isComplete()) {
                    if (tutorialVillager != null) {
                        tutorialVillager.discard();
                    }
                    tutorialVillager = new TutorialVillager(island.getConfig().getTutorialVillagerSpawn().toLocation(), this);
                }

                // ascend rewards
                if (!ascendRewards.isEmpty()) {
                    Location loc = island.getConfig().getSpawn().toLocation();
                    Vector dir = loc.getDirection().setY(0).normalize();
                    Vector up = new Vector(0, 1, 0);

                    int i = 0;
                    for (AscendReward ascendReward : ascendRewards) {
                        double angle = Math.PI / 4 * (i % 2 == 0 ? i / 2D : -Math.ceil(i / 2D));
                        Vector newDir = VectorUtil.rotateOnVector(up, dir.clone(), angle).multiply(3);
                        schedule(i * 20, () -> ascendReward.spawn(this, loc.clone().add(newDir)));
                        i++;
                    }
                }
            }
        });

        // This kinda sucks, but without knowing when those chunks get loaded this is the only way...
        schedule(20, island::resendSchematics);
        schedule(100, island::resendSchematics);
        schedule(200, island::resendSchematics);
    }

    public void addTreasureMap() {
        // get a random island to place the treasure on
        IslandModel island = new ArrayList<>(islands.values()).get(Game.getRandom().nextInt(islands.size()));

        if (Game.getRandom().nextBoolean()) {
            treasureLocations.add(island.getRandomNpcSpawn());
        } else {
            MineRegionConfig randomMineRegion = island.getRandomMineRegion();
            if (randomMineRegion != null) {
                Block randomBlock = randomMineRegion.getRandomBlock();
                if (randomBlock != null) {
                    treasureLocations.add(randomBlock.getLocation());
                }
            }
        }

        updateInventory();
    }

    @Override
    protected void tick(long ticks) {
        activityData.tick(this, ticks);
        pickaxePowerup.tick(ticks);

        workers.values().forEach(IncrementalModel::tick);
        pickaxe.tick();
        parkour.tick();
        mineshaft.tick();

        // NPCs and holograms
        NPCService.i().tick(this);
        HologramsService.i().tick(this);

        if (ticks % 20 == 0) {
            if (activityData.wasMoving(1)) {
                // campfire quest
                CampfireQuest campfire = (CampfireQuest) QuestsService.i().getQuest("campfire");
                if (campfire != null) {
                    campfire.checkCampfire(this);
                }

                // cookie thieves quest
                CookieThievesQuest cookieThieves = (CookieThievesQuest) QuestsService.i().getQuest("cookieThieves");
                if (cookieThieves != null) {
                    cookieThieves.checkZombies(this);
                }
            }

            if (activityData.wasMoving(10) && LobbyService.i().isInLobby(this)) {
                // Frozen elytra quest
                ElytraQuest elytraQuest = (ElytraQuest) QuestsService.i().getQuest("elytra");
                if (elytraQuest != null) {
                    elytraQuest.tickBlock(this);
                }

                // Bookshelves quest
                BookshelvesQuest bookshelvesQuest = (BookshelvesQuest) QuestsService.i().getQuest("bookshelves");
                if (bookshelvesQuest != null) {
                    bookshelvesQuest.tickBlocks(this);
                }

                // Daily rewards chest
                if (dailyRewardChest.isEmpty(this) && dailyRewardChest.isRefreshed()) {
                    dailyRewardChest.setClicks(0);
                }
                if (!dailyRewardChest.isEmpty(this)) {
                    ParticlesUtil.send(ParticleTypes.HAPPY_VILLAGER, LobbyService.i().getRewardsChest().clone().add(0.5, 0.5, 0.5), Triple.of(0.3F, 0.3F, 0.3F), 5, this);
                }
            }

            // Update dynamic blocks
            DynamicMineBlocksService.i().update(this);

            // treasure particles
            for (var treasureLocation : treasureLocations) {
                ParticlesUtil.send(ParticleTypes.LARGE_SMOKE, treasureLocation.clone().add(0.5, 0.5, 0.5), Triple.of(0.15F, 0.15F, 0.15F), 1, this);
            }

            // wandering trader
            if (ticks > 100) {
                trader.secondTick();
            }

            // automatically give cookies
            if (cookies > 0 && SkillsService.i().has(this, SkillType.COOKIE_5)) {
                for (WorkerType workerType : Arrays.stream(WorkerType.values()).sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                    Worker worker = workers.get(workerType);
                    if (worker != null && !worker.isExcited() && !worker.isNoAutoCookies()) {
                        cookies--;
                        worker.giveCookie();
                        updateCookiesItem();

                        if (cookies <= 0) break;
                    }
                }
            }

            // auto upgrade
            if (!activityData.isAfk() && autoUpgradeEnabled && visitingIsland == null) {
                // upgrade workers
                for (WorkerType workerType : Arrays.stream(WorkerType.values()).sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                    Worker worker = workers.get(workerType);
                    if (worker == null) {
                        int highest = workers.isEmpty() ? 0 : workers.size();
                        if (workerType.ordinal() <= highest) {
                            WorkerConfiguration configuration = WorkersService.i().getConfigurations().get(workerType);
                            if (chargeGold(configuration.getBaseCost())) {
                                WorkersService.i().unlockWorker(this, workerType);
                                worker = workers.get(workerType);
                                worker.setJustUpgradedTicks(5);
                                recalculateGoldRate();
                            }
                        }
                    }
                    if (worker == null) continue;

                    long max = worker.maxCanBuy(-1);
                    if (max > 0) {
                        worker.increaseLevel(max);
                        chargeGold(worker.cost(max));
                        worker.setJustUpgradedTicks(5);
                        recalculateGoldRate();

                        return;
                    }
                }

                // upgrade pickaxe
                long maxPicks = pickaxe.maxCanBuy(-1);
                if (maxPicks > 0 && chargeGold(pickaxe.cost(maxPicks))) {
                    pickaxe.increaseLevel(maxPicks);
                    pickaxe.setJustUpgradedTicks(5);
                    recalculateGoldRate();
                }
            }

            // auto-clicking detection
            if (activityData.isAutoClicking()) {
                activityData.setAutoClickerKicks(activityData.getAutoClickerKicks() + 1);

                getPlayer().kickPlayer(ChatColor.DARK_RED + "Auto-clicker detected!\n\n" +
                        ChatColor.RED + "Auto-clicking is not allowed and you can be permanently banned.");
                return;
            }

            // play time achievement
            if (!activityData.isAfk()) {
                long hours = activityData.getPlayTime() / 3600;
                AchievementsService.i().setProgress(this, "playtime", hours);
            }

            // check visiting island
            if (visitingIsland != null && (visitingIsland.getPlayer().isOffline()
                    || (!visitingIsland.getPlayer().isOnOwnIsland() && !MineshaftService.i().isInMineshaft(visitingIsland.getPlayer()))
                    || !getFriendsData().isFriendsWith(visitingIsland.getPlayer().getUuid()))
            ) {
                sendMessage(visitingIsland.getPlayer().getName() + " left their island");
                tpToIsland(getCurrentIsland(false), true);
            }

            // ester eggs
            if (!activityData.isAfk()) {
                for (EasterEgg egg : LobbyService.i().getEasterEggs().values()) {
                    if (!getLobbyData().getCollectedEasterEggs().contains(egg.id())
                            && egg.location().distanceSquared(getPlayer().getLocation()) <= 100) {
                        getPlayer().sendBlockChange(egg.location(), Bukkit.createBlockData(Material.DRAGON_EGG));
                    }
                }
            }

            // parkour checkpoint particles
            if (!activityData.isAfk() && parkour.isStarted() && parkour.getCheckpoint() != null) {
                Location loc = parkour.getCheckpoint().clone().add(0.5, 0.1, 0.5);
                double radius = 0.5;
                double step = 1 / Math.PI;
                for (double theta = 0; theta < Math.PI * 2; theta += step) {
                    ParticlesUtil.sendColor(loc.clone().add(radius * Math.cos(theta), 0, radius * Math.sin(theta)), new java.awt.Color(0, 100, 0), this);
                }

                for (double x = -0.4; x < 0.4; x += 0.08) {
                    ParticlesUtil.sendColor(loc.clone().add(x, 0, x), new java.awt.Color(0, 230, 0), this);
                    ParticlesUtil.sendColor(loc.clone().add(x, 0, -x), new java.awt.Color(0, 230, 0), this);
                }
            }

            // block breaking effect
            if (lastClickedBlock != null) {
                int cps = activityData.calculateAvgCPS();
                BlockPos position = new BlockPos(lastClickedBlock.getX(), lastClickedBlock.getY(), lastClickedBlock.getZ());
                int clicks = (int) (Math.min(10, ((cps / 15D) * 10)) - 1);
                sendPacket(new ClientboundBlockDestructionPacket(getPlayer().getEntityId(), position, clicks));

                if (cps == 0) {
                    lastClickedBlock = null;
                }
            }

            // mining blocks particles
            if (!activityData.isAfk() && ticks % 60 == 0 && isOnOwnIsland()) {
                getCurrentIsland(true).getMineRegions().forEach(region -> {
                    for (int i = 0; i < 5; i++) {
                        Block randomBlock = region.getRandomBlock();
                        if (randomBlock != null) {
                            ParticlesUtil.send(ParticleTypes.HAPPY_VILLAGER, randomBlock.getLocation().add(0.5, 0.5, 0.5), Triple.of(0.3F, 0.3F, 0.3F), 5, this);
                        }
                    }
                });
            }

            // afk message
            if (activityData.isAfk()) {
                parkour.reset();

                long time = activityData.getAfkTime();
                String exactTime = Formatter.duration(time * 1000);
                long vaults = getVaults();
                long validTime = (long) Math.min(time, vaults * (3.6e+3));
                int filledVaults = (int) Math.floor(validTime / 3.6e+3);

                String subtitle;
                if (filledVaults >= vaults) {
                    subtitle = ChatColor.RED + exactTime + ChatColor.GOLD + " All of your vaults are full!";
                    if (!afkVaultsFilled) {
                        recalculateGoldRate();
                    }
                    afkVaultsFilled = true;
                } else {
                    subtitle = ChatColor.GREEN + exactTime + ChatColor.GOLD + " Your vaults are being filled up!";
                }
                subtitle += ChatColor.GRAY + " (" + filledVaults + "/" + vaults + ")";

                MessageUtil.sendTitle(ChatColor.RED + "You seem to be away", subtitle, this);
            } else if (afkVaultsFilled) {
                recalculateGoldRate();
                afkVaultsFilled = false;
            }

            // Quest objectives progress
            if (!activityData.isAfk()) {
                QuestsService.i().checkQuestObjectives(this);
                QuestsService.i().checkDailyQuests(this);
            }
        }

        if (ticks % 100 == 0) {
            // update lobby scoreboard
            if (LeaderboardsService.i().isInRegion(this)) {
                LeaderboardsService.i().update(this);
            }

            // update pickaxe powerup
            pickaxePowerup.update(this);

            // spawn gold bats
            if (ticks > 100 && tutorial.isComplete() && !activityData.isAfk()
                    && !MineshaftService.i().isInMineshaft(this)
                    && !SpleefService.i().isInArena(getPlayer().getLocation())) {
                long currentTime = System.currentTimeMillis();
                if (currentTime > batNextSpawnAt && Game.getRandom().nextInt(10) == 0) {
                    // from 5 to 20 minutes; bat lives for 2
                    batNextSpawnAt = currentTime + (5 + Game.getRandom().nextInt(16)) * 60 * 1000;

                    // Spawn a bat swarm with 33% chance
                    if (SkillsService.i().has(this, SkillType.BAT_5) && Game.getRandom().nextInt(3) == 0) {
                        int amount = Game.getRandom().nextInt(4) + 3; // 3 to 6 bats

                        for (int i = 0; i < amount; i++) {
                            goldBats.add(GoldBat.spawn(this));
                        }

                        arrows = 5;
                    }
                    // Failed to spawn the bat swarm; spawn one bat.
                    else {
                        goldBats.add(GoldBat.spawn(this));

                        arrows = SkillsService.i().has(this, SkillType.BAT_3) ? 5 : 3;
                    }

                    // Spawn additional bat
                    if (SkillsService.i().has(this, SkillType.BAT_4) && Game.getRandom().nextInt(4) == 0) {
                        goldBats.add(GoldBat.spawn(this));
                    }

                    updateInventory();
                }
            }

            // check if a bat needs to die
            goldBats.removeIf(bat -> {
                if (!bat.isAlive() || getPlayer().getLocation().distanceSquared(bat.getBukkitEntity().getLocation()) > 50 * 50) {
                    bat.discard();
                    return true;
                }

                return false;
            });

            if (goldBats.isEmpty() && getArrows() > 0) {
                setArrows(0);
                updateInventory();
            }

            // announce uncollected achievements
            AchievementsService.i().checkAchievements(this);

            // update island parkour
            if (visitingIsland == null) {
                getCurrentIsland().updateParkour();
            }

            // recalculate gold
            recalculateGoldRate();

            // check discord notify
            if (activityData.isAfk() && playerSettings.getDiscordAfk().get()) {
                updateNotify();
            } else {
                notify.remove("discordReward");
                notify.remove("discordVaults");
            }
        }

        if (!afkVaultsFilled) {
            addGold(goldRate);
        }

        if (!mineshaft.isStarted() && (visitingIsland == null || !visitingIsland.getPlayer().getMineshaft().isStarted())) {
            MessageUtil.sendHotbar(gold.print(this, false, false) + ChatColor.GREEN + " Gold", this);
        }

        if (ticks != 0 && ticks % 1200 == 0) {
            // resend island schematics every minute
            if (isOnOwnIsland() || visitingIsland != null) {
                getCurrentIsland().resendSchematics();
            }

            // clean up allowed entities
            ServerLevel world = ((CraftWorld) getPlayer().getWorld()).getHandle();
            allowedEntities.removeIf(id -> {
                Entity entity = world.getEntity(id);
                return entity == null || !entity.isAlive();
            });
        }
    }

    public long getVaults() {
        long vaults = LevelsService.i().getLevel(exp) / 5 + 2;

        if (SkillsService.i().has(this, SkillType.MISC_2))
            vaults += 4;

        if (getRank().isAtLeast(Rank.PAID))
            vaults *= 2;

        return vaults;
    }

    public void updateParkourItem() {
        if (parkour.isStarted()) {
            int amount = parkour.getCheckpoints() - parkour.getCheckpointsUsed();
            if (amount <= 0) {
                getPlayer().getInventory().setItem(0, null);
            } else {
                ItemStack clone = ParkourService.ITEM.clone();
                clone.setAmount(amount);
                getPlayer().getInventory().setItem(0, clone);
            }
        }
    }

    public void updateMenuItems() {
        if (isOffline())
            return;

        if (tutorial.isComplete() || tutorial.isShowUpgradesMenu()) {
            getPlayer().getInventory().setItem(5, UpgradesMenu.MENU_ITEM);
        } else {
            getPlayer().getInventory().clear(5);
        }

        getPlayer().getInventory().setItem(8, MainMenu.MENU_ITEM);
    }

    public void processVotes(int count) {
        int multiplier = SkillsService.i().has(this, SkillType.MISC_1) ? 2 : 1;
        BigNumber goldMade = new BigNumber(getGoldRate().multiply(new BigNumber(72000 * multiplier)));
        long schmepls = 100 * multiplier;

        String awardMsg = ChatColor.GREEN + "+"
                + ChatColor.AQUA + schmepls
                + ChatColor.YELLOW + " schmepls"
                + ChatColor.GRAY + " and "
                + ChatColor.GREEN + "+" + goldMade.print(this) + ChatColor.YELLOW + " gold";
        sendImportantMessage("Thank you for voting!", awardMsg);

        addSchmepls(schmepls);
        addGold(goldMade);
        StatisticsService.i().increment(getUuid(), StatisticType.VOTES);
    }

    public void recalculateGoldRate() {
        achievementGoldMultiplier = 1.000 + 0.002 * AchievementsService.i().getProgress(this, "achievements");

        workers.values().forEach(Worker::recalculate);
        pickaxe.recalculate();

        BigNumber totalWorkersIncome = getTotalWorkersIncome();
        goldRate = totalWorkersIncome.multiply(new BigNumber("0.05"));

        //Check achievements
        AchievementsService.i().setProgress(this, "income", totalWorkersIncome.getExponent());

        if (workers.size() == WorkerType.values().length) {
            long minLevel = workers.values().stream()
                    .mapToLong(IncrementalModel::getLevel)
                    .min()
                    .orElse(0);
            AchievementsService.i().setProgress(this, "totalworkers", minLevel);
        }

        updateScoreboard();
    }

    public void updateScoreboard() {
        if (scoreboard == null) {
            return;
        }

        String staffChatStatus = getChatData().isStaffChat() ? ChatColor.RED + "Staff chat is on" : " ";
        scoreboard.setScore(1, staffChatStatus);

        if (!MineshaftService.i().isInMineshaft(this)) {
            String production = (afkVaultsFilled ? ChatColor.RED + "Vaults are filled" : getTotalWorkersIncome().print(this, false, true)) + ChatColor.GREEN + " gold/s";
            scoreboard.setScore(2, ChatColor.GRAY + "Production  ");
            scoreboard.setScore(3, String.format(" %s  ", production));
            scoreboard.setScore(4, "  ");
            String lifeGold = getLifelongGold().print(this, false, true);
            scoreboard.setScore(5, ChatColor.GRAY + "Produced  ");
            scoreboard.setScore(6, String.format(" %s  ", lifeGold));
            scoreboard.setScore(7, "   ");
            String schmeplsDisplay = ChatColor.AQUA + Formatter.format(schmepls);
            scoreboard.setScore(8, ChatColor.GRAY + "Schmepls  ");
            scoreboard.setScore(9, String.format(" %s  ", schmeplsDisplay));
            scoreboard.setScore(10, "    ");

            if (!parkour.isStarted() && getDimensionsData().getAscensionsTotal() > 0) {
                scoreboard.setScore(11, ChatColor.GRAY + "Ascension  ");

                BigNumber minGold = dimensionsData.getDimension().getMinGold();
                double minPercent = Math.min(100d, 100d * (getLifelongGold().divide(minGold).doubleValue()));
                double ascendPercent = AscensionServices.i().getAscendPercent(this);

                // if the player hasn't reached the max percentage yet, don't show the multiplier for the max reward
                if (minPercent < 100) {
                    ChatColor percentColor;

                    if (minPercent <= 30) {
                        percentColor = ChatColor.RED;
                    } else {
                        if (minPercent <= 80) {
                            percentColor = ChatColor.YELLOW;
                        } else {
                            percentColor = ChatColor.GREEN;
                        }
                    }
                    scoreboard.setScore(12, percentColor + " " + String.format("%.2f%%", minPercent) + ChatColor.GRAY);
                } else {
                    // if this is 1 then just show the number without any decimal places
                    // Also make it GOLD so the player knows it's maxed out, and it won't increase anymore
                    ChatColor multiplierColor = ascendPercent == 1 ? ChatColor.GOLD : ChatColor.YELLOW;
                    String ascendPercentage = String.format(ascendPercent == 1 ? "%.0f" : "%.2f", 1 + (ascendPercent * 10d));
                    String ascendText = String.format("%s 100%%%s ┃ %s%sx  ", ChatColor.GREEN, ChatColor.GRAY, multiplierColor, ascendPercentage);
                    scoreboard.setScore(12, ascendText);
                }
                scoreboard.setScore(13, "     ");
            }
        }


        int activeBoosters = BoostersService.i().getActiveBoosters().size();
        if (!playerSettings.getHideBoosters().get() && activeBoosters > 0) {
            scoreboard.setScore(14, ChatColor.GOLD + "Active Boosters: " + activeBoosters);
        } else {
            scoreboard.removeScore(14);
        }
    }

    public BigNumber getTotalWorkersIncome() {
        return workers.values().stream()
                .map(IncrementalModel::getIncome)
                .reduce(BigNumber::add)
                .orElse(BigNumber.ZERO);
    }

    public double computeSuperBlockPercent() {
        double chance = superBlockData.getChance() + (BoostersService.i().getActiveBoost(BoosterType.SUPER_BLOCK_BOOSTER) - 1);
        if (SkillsService.i().has(this, SkillType.SUPERBLOCK_4)) {
            chance *= 1.3;
        } else if (SkillsService.i().has(this, SkillType.SUPERBLOCK_1)) {
            chance *= 1.1;
        }
        if (SkillsService.i().has(this, SkillType.SUPERBLOCK_6)) {
            chance += 0.02 * islands.size();
        }
        return Math.min(chance, 0.75);
    }

    public BigNumber getSuperBlockReward(BigNumber toAdd) {
        double percent = computeSuperBlockPercent();
        if (percent > 0 && Game.getRandom().nextDouble() < percent) {
            toAdd = toAdd.multiply(new BigNumber(String.valueOf(Game.getRandom().nextInt(90) + 10)));
            if (SkillsService.i().has(this, SkillType.SUPERBLOCK_2)) {
                toAdd = toAdd.multiply(new BigNumber("1.2"));
            }
            if (SkillsService.i().has(this, SkillType.SUPERBLOCK_5)) {
                toAdd = toAdd.multiply(new BigNumber("1.5"));
            }

            return toAdd;
        } else {
            return null;
        }
    }

    public void onItemClick(ItemStack item) {
        if (activityData.isAfk()) {
            return;
        }

        if (ItemBuilder.isSameTitle(item, UpgradesMenu.MENU_ITEM)) {
            upgradesMenu.open(getPlayer());
        } else if (ItemBuilder.isSameTitle(item, MainMenu.MENU_ITEM)) {
            mainMenu.open(getPlayer());
        } else if (ItemBuilder.isSameTitle(item, LobbyTpMenu.MENU_ITEM)) {
            new LobbyTpMenu(this);
        } else if (ItemBuilder.isSameTitle(item, IslandsService.i().getVisitingItem())) {
            tpToIsland(getCurrentIsland(false), true);
        } else if (ItemBuilder.isSameTitle(item, GadgetsService.i().getMenuItem())) {
            new GadgetsMenu(this);
        } else if (ItemBuilder.isSameTitle(item, GadgetsService.i().getMenuItem())) {
            new GadgetsMenu(this);
        } else if (item.getType().equals(pickaxe.getConfiguration().getMaterial())) {
            pickaxePowerup.rightClick();
        } else if (!treasureLocations.isEmpty() && item.getType().equals(Material.WOODEN_SHOVEL)) {
            List<Block> blocks = getPlayer().getLastTwoTargetBlocks(null, 4);
            if (blocks.size() >= 2) {
                Location target = blocks.get(1).getLocation();
                treasureLocations.removeIf(treasureLocation -> {
                    if (treasureLocation.distanceSquared(target) <= 3) {
                        digOutTreasure(treasureLocation);
                        return true;
                    }
                    return false;
                });

                updateInventory();
            }
        }
    }

    private void digOutTreasure(Location location) {
        Location center = location.clone().add(0.5, 0.5, 0.5);
        Runner.sync(0, 5, state -> {
            double percent = Game.getRandom().nextDouble();

            playSound(Sound.BLOCK_SAND_BREAK, 0.5, 1);
            ParticlesUtil.send(ParticleTypes.CLOUD, center, Triple.of(0.5f, 0.5f, 0.5f), 10, this);

            if (state.getTicks() < 6) return;
            state.cancel();
            playSound(Sound.BLOCK_CHEST_OPEN, 0.5, 1);

            List<Triple<Integer, String, Boolean>> items = new ArrayList<>();

            // schmepls
            long schmepls = (long) Math.max(10, percent * 200);
            addSchmepls(schmepls);
            HologramsService.i().spawnFloatingUp(center.clone().add(new RandomVector().setY(Game.getRandom().nextDouble() * -2)), p -> ChatColor.GREEN + "+" + ChatColor.YELLOW + schmepls + ChatColor.AQUA + " schmepls", Collections.singleton(this));
            items.add(Triple.of((int) schmepls, "schmepl", true));

            // exp
            long exp = (long) Math.max(5, percent * 50);
            addExp(exp);
            HologramsService.i().spawnFloatingUp(center.clone().add(new RandomVector().setY(Game.getRandom().nextDouble() * -2)), p -> ChatColor.GREEN + "+" + ChatColor.YELLOW + exp + ChatColor.AQUA + " EXP", Collections.singleton(this));
            items.add(Triple.of((int) exp, "exp", false));

            // powerup parts
            int parts = (int) Math.max(0, percent * 6 - 2);
            if (parts > 0) {
                HologramsService.i().spawnFloatingUp(center.clone().add(new RandomVector().setY(Game.getRandom().nextDouble() * -2)), p -> ChatColor.GREEN + "+" + ChatColor.YELLOW + parts + ChatColor.AQUA + " powerup parts", Collections.singleton(this));
                PowerupService.i().addParts(this, parts);
                items.add(Triple.of(parts, "powerup part", true));
            }

            // 30% chance of geodes
            if (Game.getRandom().nextDouble() < 0.3) {
                int count = Game.getRandom().nextInt(2) + 1;
                Rarity rarity = Rarity.random();
                GeodesService.i().addGeode(this, rarity, count);
                HologramsService.i().spawnFloatingUp(center.clone().add(new RandomVector().setY(Game.getRandom().nextDouble() * -2)), p -> ChatColor.GREEN + "+" + ChatColor.YELLOW + count + ChatColor.AQUA + " geode" + (count == 1 ? "" : "s"), Collections.singleton(this));
                items.add(Triple.of(count, rarity.getGeodeName() + ChatColor.GOLD + " geode" + (count == 1 ? "" : "s"), false));
            }

            // send msg
            sendListMessage("Treasure chest contents:", items, true);
        });
    }

    /**
     * Send a message with a list of items
     *
     * @param title The title of the message
     * @param items List of items. Triple: count, name, true if can be plural
     * @param lines Whether to encapsulate with lines
     */
    public void sendListMessage(String title, List<Triple<Integer, String, Boolean>> items, boolean lines) {
        StringBuilder builder = new StringBuilder();
        if (lines) {
            builder.append(Strings.line()).append("\n");
        }
        builder.append(ChatColor.YELLOW).append(title);
        for (Triple<Integer, String, Boolean> item : items) {
            builder.append("\n ");
            if (item.first() != 0) {
                builder.append(ChatColor.GREEN).append("+").append(item.first()).append(" ");
            }
            builder.append(ChatColor.GOLD).append(item.second());
            if (item.first() != 1 && item.third()) {
                builder.append("s");
            }
        }
        if (lines) {
            builder.append("\n").append(Strings.line());
        }

        sendMessage(builder.toString());
    }

    public List<Block> getTargetBlocks() {
        if (isOffline()) return null;
        List<Block> blocks = new ArrayList<>();
        BlockIterator itr = new BlockIterator(getPlayer(), 4);

        while (itr.hasNext()) {
            Block block = itr.next();
            blocks.add(block);
            if (blocks.size() > 2) {
                blocks.remove(0);
            }

            Material material = block.getType();
            if (!material.isAir() && !material.equals(Material.RAIL) || DynamicMineBlocksService.i().contains(this, block)) {
                break;
            }
        }

        return blocks;
    }

    public void onSwing() {
        if (getPlayer().isSneaking() && isRankAtLeast(Rank.DEV)) {
            pickaxePowerup.setCharge(1);

            popSound();
            return;
        }

        if (ignoreClicks.get() > 0) {
            // take care of some weird bug when holding or right clicking too fast it would fire the event twice
            if (ignoreClicks.decrementAndGet() > 1) {
                ignoreClicks.set(1);
            }
            return;
        }
        if (SpleefService.i().isInArena(getPlayer().getLocation())) return;

        activityData.click();

        if (pickaxe == null || pickaxe.getAmount() <= 0 || pickaxe.getTempAmount() <= 0 || activityData.isAfk())
            return;

        List<Block> targetBlocks = getTargetBlocks();
        if (targetBlocks == null || targetBlocks.size() < 2)
            return;
        Block clickedBlock = targetBlocks.get(1);

        // check campfires
        Quest campfire = QuestsService.i().getQuest("campfire");
        if (campfire != null) {
            ((CampfireQuest) campfire).checkCampfireClick(this, clickedBlock);
        }

        Player p = getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!item.getType().equals(pickaxe.getConfiguration().getMaterial())) {
            return;
        }

        List<MineRegionConfig> regions = getCurrentIsland(true).getMineRegions(clickedBlock.getLocation());
        boolean rewardChestBlock = clickedBlock.getLocation().equals(LobbyService.i().getRewardsChest());
        if (rewardChestBlock && dailyRewardChest.isEmpty(this)) {
            noSound();
            return;
        }

        MineRegionConfig region = regions.stream().filter(r -> r.getBlockMaterial().equals(clickedBlock.getType())).findFirst().orElse(null);
        boolean accepted = false;
        Material carryItem = null;
        Material dynamicBlockMaterial = DynamicMineBlocksService.i().getMaterial(this, clickedBlock);
        if (dynamicBlockMaterial != null) {
            accepted = true;
            carryItem = dynamicBlockMaterial;
        } else if (region != null && region.getBlockMaterial().equals(clickedBlock.getType())) {
            accepted = true;
            carryItem = region.getItemMaterial();
        } else if (!getCurrentIsland(true).getConfig().getGlobalMineBlocks().isEmpty()) {
            Optional<MineBlock> optional = getCurrentIsland(true).getConfig().getGlobalMineBlocks().stream()
                    .filter(b -> b.getBlockMaterial().equals(clickedBlock.getType()))
                    .findAny();
            if (optional.isPresent()) {
                carryItem = optional.get().getItemMaterial();
                accepted = true;
            }
        }

        if (!rewardChestBlock && !accepted || item.getAmount() <= 0) {
            lastClickedBlock = null;
            return;
        }

        BlockFace face = targetBlocks.get(1).getFace(targetBlocks.get(0)).getOppositeFace();
        Location location = targetBlocks.get(0).getLocation(); //Erm wrong block but I already changed swing offsets so fuck it

        Set<GamePlayer> players = getCurrentIsland().getAllPlayers();
        pickaxe.updateItem(-1);
        Material finalCarryItem = carryItem;
        SwingAnimation.builder()
                .item(new ItemStack(pickaxe.getConfiguration().getMaterial()))
                .location(location)
                .spawnLocation(getPlayer().getLocation())
                .face(face)
                .degreeStep(pickaxe.getConfiguration().getSpeed())
                .swingsToLive(1)
                .onSwing(i -> {
                    if (isOffline())
                        return;

                    pickaxe.updateItem(1);
                    getPlayer().playSound(location, Sound.BLOCK_STONE_HIT, 1, 1);

                    if (rewardChestBlock) {
                        dailyRewardChest.handleClick(this);
                        return;
                    }

                    if (dynamicBlockMaterial == null) {
                        lastClickedBlock = clickedBlock;
                    }
                    activityData.setEverClicked(true);
                    StatisticsService.i().increment(getUuid(), StatisticType.CLICKS);
                    QuestsService.i().incrementProgress(this, "dailyClicks", 0, 1);

                    // check collector quest
                    Quest collector = QuestsService.i().getQuest("collector");
                    if (collector != null) {
                        ((CollectorQuest) collector).checkMinedBlock(this, clickedBlock.getType());
                    }

                    // handle dynamic mine block clicks
                    if (dynamicBlockMaterial != null) {
                        DynamicMineBlocksService.i().click(this, clickedBlock, 1);
                        pickaxePowerup.click(false);
                        return;
                    }

                    BigNumber toAdd = pickaxe.getIncome();
                    BigNumber superBlockReward = getSuperBlockReward(toAdd);
                    boolean superBlock = superBlockReward != null;
                    if (superBlock) {
                        toAdd = superBlockReward;

                        ParticlesUtil.send(ParticleTypes.LAVA, location.clone().add(0.5, 0.5, 0.5), Triple.of(.3F, .3F, .3F), 1, players);
                        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, location, 0.1F, 1);

                        superBlockData.setClicksInARow(superBlockData.getClicksInARow() + 1);
                        AchievementsService.i().setProgress(this, "superblock", superBlockData.getClicksInARow());
                        QuestsService.i().incrementProgress(this, "dailySuperBlock", 0, 1);
                    } else {
                        superBlockData.setClicksInARow(0);
                    }
                    pickaxePowerup.click(superBlock);

                    addGold(toAdd);

                    if (visitingIsland != null) {
                        GamePlayer visiting = visitingIsland.getPlayer();
                        visiting.addGold(visiting.getPickaxe().getIncome());

                        HologramsService.i().spawnBlockBreak(
                                clickedBlock.getLocation().add(0.5, 0.5, 0.5).add(new RandomVector(Game.getRandom().nextDouble() * 0.5)),
                                visiting.getPickaxe().getIncome(),
                                false,
                                Collections.singleton(visiting)
                        );
                    }

                    if (tutorialVillager != null) {
                        tutorialVillager.onMine();
                    }

                    HologramsService.i().spawnBlockBreak(
                            clickedBlock.getLocation().add(0.5, 0.5, 0.5).add(new RandomVector(Game.getRandom().nextDouble() * 0.8)),
                            toAdd,
                            superBlock,
                            Collections.singleton(this)
                    );

                    DroppedItem.spawn(finalCarryItem, location.clone().add(0.5, 0.5, 0.5), 40, players);

                    boolean hasCookieUpgrade = SkillsService.i().has(this, SkillType.COOKIE_4);
                    if (SkillsService.i().has(this, SkillType.COOKIE_1)
                            && System.currentTimeMillis() - cookieLastGivenAt > (hasCookieUpgrade ? 15000 : 30000)
                            && Game.getRandom().nextDouble() < (hasCookieUpgrade ? 0.05 : 0.025)) {
                        cookieLastGivenAt = System.currentTimeMillis();
                        popSound();
                        DroppedItem.spawn(Material.COOKIE, location.clone().add(0.5, 0.5, 0.5), 100, Collections.singleton(this), player -> {
                            if (cookies < 64) {
                                addCookies(1);

                                playSound(Sound.ENTITY_CHICKEN_EGG, 0.5, 0.1);
                                updateCookiesItem();
                                return true;
                            }

                            return false;
                        });
                    }
                })
                .build()
                .spawn(players);
    }

    public void addCookies(int amount) {
        cookies += amount;

        AchievementsService.i().incrementProgress(this, "cookies", amount);
        QuestsService.i().incrementProgress(this, "dailyCookies", 0, 1);
    }

    public void updateCookiesItem() {
        if (isOffline()) return;

        if (cookies <= 0) {
            getPlayer().getInventory().setItem(3, null);
        }

        ItemStack itemStack = ItemBuilder.builder().material(Material.COOKIE)
                .amount(Math.min(64, cookies))
                .title(ChatColor.GOLD + "Worker cookie" + ChatColor.GRAY + " - right-click worker")
                .lore(" ")
                .lore(ChatColor.GRAY + "Give this cookie to a worker")
                .lore(ChatColor.GRAY + "to make them work faster")
                .build().toItem();

        getPlayer().getInventory().setItem(3, itemStack);
    }

    public void hardReset() {
        Player p = getPlayer();
        if (p == null)
            return;

        StatisticsService.i().reset(getUuid());
        StatisticsService.i().flush(getUuid());
        onQuit();

        p.teleport(Game.i().getSpawn());
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 0.25, 1);

        schedule(10, () -> {
            try {
                GamePlayer cleanPlayer = new GamePlayer();
                for (Field field : GamePlayer.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    field.set(this, field.get(cleanPlayer));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            this.setDestroyed(true);
            getPlayer().kickPlayer(ChatColor.GREEN + "Your player data was reset\n" + ChatColor.YELLOW + "Please rejoin");
        });
    }

    public void sendBlockChange(Block block) {
        sendBlockChange(block.getLocation(), block.getBlockData());
    }

    /**
     * Use {@link Bukkit#createBlockData(Material)}
     */
    public void sendBlockChange(Location location, BlockData blockData) {
        if (isOffline())
            return;

        com.comphenix.protocol.wrappers.BlockPosition position = new com.comphenix.protocol.wrappers.BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        allowedBlockChanges.add(position);
        getPlayer().sendBlockChange(location, blockData);
        allowedBlockChanges.remove(position);
    }

    @Override
    public void updateFriends() {
        friendsMenu.reloadFriends();
    }
}
