package net.mineclick.game.util;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.visual.NPC;
import net.mineclick.global.config.field.MineRegionConfig;
import net.mineclick.global.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// TODO: migrate from the deprecated NPC class
public class TutorialVillager extends NPC {
    private final GamePlayer player;
    private Runnable getsCloseRunnable;
    private Location walkLocation;
    private Iterator<Location> pathIterator;
    private String goal;
    private int notWalkingTicks = 0;
    private int notFollowingTicks = 0;
    private Runnable walkCallback;

    private boolean checkForMine;
    private boolean checkForPickaxeUpgrade;
    private boolean checkForWorkerUpgrade;
    @Getter
    private boolean checkForIslandUpgrade;

    public TutorialVillager(Location location, GamePlayer player) {
        super(location, VillagerType.SAVANNA, VillagerProfession.CARTOGRAPHER, Collections.singleton(player));

        this.player = player;

        setCustomNameVisible(true);
        setCustomName(CraftChatMessage.fromStringOrNull(ChatColor.GREEN + "Tutorial guide"));

        msg(() -> schedule(40, () -> {
                    MineRegionConfig region = player.getCurrentIsland().getRandomMineRegion();
                    if (region == null || player.isOffline()) {
                        return;
                    }

                    Location loc = player.getPlayer().getLocation();
                    loc.setDirection(new Vector(getX(), 0, getZ()).subtract(loc.toVector()));
                    loc.setPitch(0);
                    player.getPlayer().teleport(loc);

                    msg(() -> setGoal("Follow the guide"), Pair.of("Follow me!", 0));

                    walkTo(new ArrayList<>(region.getPathList()), () -> {
                        Location bLoc = region.getRandomAccessibleBlock(this);
                        if (bLoc != null) {
                            walkTo(Collections.singletonList(bLoc), this::gotToMines);
                        } else {
                            gotToMines();
                        }
                    });
                }),
                Pair.of("Hey there! I see you are new here", 0), Pair.of("Let me show you around!", 60)
        );
    }

    @SafeVarargs //Shh, just don't be an idiot and pass the proper types
    public static void msg(GamePlayer player, Runnable callback, Pair<String, Integer>... msgs) {
        new BukkitRunnable() {
            int index = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (index >= msgs.length || player.isOffline()) {
                    if (callback != null) {
                        callback.run();
                    }
                    cancel();
                    return;
                }

                if (ticks++ >= msgs[index].value()) {
                    ticks = 0;
                    player.sendMessage(ChatColor.DARK_GREEN + "Guide: " + ChatColor.WHITE + msgs[index++].key());
                }
            }
        }.runTaskTimer(Game.i(), 0, 1);
    }

    @SafeVarargs
    private void msg(Runnable callback, Pair<String, Integer>... msgs) {
        msg(player, callback, msgs);
    }

    private void setGoal(String obj) {
        if (obj == null) {
            goal = null;
            return;
        }

        goal = ChatColor.AQUA + obj;
    }

    private void schedule(int ticks, Runnable run) {
        Runner.sync(ticks, run);
    }

    private void walkTo(List<Location> path, Runnable walkCallback) {
        this.walkCallback = walkCallback;
        pathIterator = path.iterator();
        walkLocation = pathIterator.next();
    }

    @Override
    public void baseTick() {
        if (player.isOffline()) {
            discard();
            return;
        }

        super.baseTick();

        if (tickCount % 20 == 0 && goal != null) {
            MessageUtil.sendTitle("", goal, player);
        }

        if (getsCloseRunnable != null && tickCount % 10 == 0) {
            Location loc = player.getPlayer().getLocation();
            if (distanceToSqr(loc.getX(), loc.getY(), loc.getZ()) <= 10) {
                getsCloseRunnable.run();
                getsCloseRunnable = null;
            }
        }

        if (walkLocation != null && pathIterator != null) {
            setShouldMove(true);

            Location ploc = player.getPlayer().getLocation();
            if (distanceToSqr(ploc.getBlockX(), ploc.getY(), ploc.getZ()) > 100) {
                if (notFollowingTicks++ == 100) {
                    Location loc = player.getPlayer().getLocation();
                    loc.setDirection(new Vector(getX(), 0, getZ()).subtract(loc.toVector()));
                    loc.setPitch(0);
                    player.getPlayer().teleport(loc);

                    msg(() -> {
                    }, Pair.of("C'mon! I'm right here", 0));
                }

                if (tickCount % 20 == 0) {
                    lookTowards(player);
                }
            } else {
                notFollowingTicks = 0;

                if (getNavigation().moveTo(walkLocation.getX(), walkLocation.getY(), walkLocation.getZ(), 0.6)) {
                    notWalkingTicks = 0;
                } else {
                    if (distanceToSqr(walkLocation.getX(), walkLocation.getY(), walkLocation.getZ()) <= 2) {
                        if (pathIterator.hasNext()) {
                            walkLocation = pathIterator.next();
                        } else {
                            walkLocation = null;
                            pathIterator = null;

                            walkCallback.run();
                        }
                    } else if (notWalkingTicks++ == 100) {
                        getNavigation().stop();

                        msg(() -> {
                                    Location loc = walkLocation;
                                    while (pathIterator.hasNext()) {
                                        loc = pathIterator.next();
                                    }

                                    moveTo(loc.getX(), loc.getY(), loc.getZ());
                                    player.getPlayer().teleport(loc);
                                },
                                Pair.of("Erm this is weird... I can't walk up to there!", 0), Pair.of("Whatever, let's take a shortcut", 40)
                        );
                    }

                }
            }

            setLooking(false);
        } else {
            setLooking(true);
            setShouldMove(false);
        }
    }

    private void gotToMines() {
        setGoal(null);

        schedule(40, () -> getsCloseRunnable = () -> msg(() -> {
                            setGoal("Mine some blocks");
                            checkForMine = true;
                        },
                        Pair.of("Green particles show blocks that you can mine", 0), Pair.of("So go ahead and mine some out!", 80))
        );
    }

    public void onMine() {
        if (checkForMine) {
            checkForMine = false;
            setGoal(null);

            msg(() -> {
                        setGoal("Upgrade Pickaxe");

                        checkForPickaxeUpgrade = true;
                        player.getTutorial().setShowUpgradesMenu(true);
                        player.updateInventory();
                    },
                    Pair.of("For every mined block you get " + player.getPickaxe().getIncome().print(player, false, true, ChatColor.YELLOW, ChatColor.YELLOW, ChatColor.WHITE, false) + " gold", 0),
                    Pair.of("You can upgrade your Pickaxe to get more gold", 100),
                    Pair.of("Open the Upgrades menu from the toolbar and upgrade your Pickaxe", 100)
            );
        }
    }

    public void onPickaxeUpgrade() {
        if (checkForPickaxeUpgrade) {
            checkForPickaxeUpgrade = false;
            setGoal(null);
            player.getPlayer().closeInventory();

            msg(() -> {
                        player.addGold(new BigNumber("500"));
                        setGoal("Unlock a Worker");
                        player.getTutorial().setShowUpgradesMenuWorkers(true);
                        checkForWorkerUpgrade = true;
                    },
                    Pair.of("Awesome! Now you get " + player.getPickaxe().getIncome().print(player, false, true, ChatColor.YELLOW, ChatColor.YELLOW, ChatColor.WHITE, false) + " gold/click", 0),
                    Pair.of("This is great, but there's a way to automate your income", 100),
                    Pair.of("You can hire Workers that will do the job for you", 120),
                    Pair.of("Open the same menu and unlock your first Worker", 120),
                    Pair.of("Here's some free gold for you!", 40)
            );
        }
    }

    public void onWorkerUpgrade() {
        if (checkForWorkerUpgrade) {
            checkForWorkerUpgrade = false;
            setGoal(null);
            player.getPlayer().closeInventory();

            msg(() -> {
                        player.getTutorial().setShowScoreboard(true);
                        player.createScoreboard();
                        player.recalculateGoldRate();

                        msg(() -> {
                                    setGoal("Buy an Island Upgrade (Villager)");
                                    checkForIslandUpgrade = true;

                                    player.getCurrentIsland(false).update();
                                },
                                Pair.of("Next thing I want to show you are Island Upgrades", 120),
                                Pair.of("You can now find villagers around the island selling Island Upgrades", 120),
                                Pair.of("Your first upgrade is free! Go find an upgrade villager", 160)
                        );
                    },
                    Pair.of("Congrats! You're this much closer to becoming a millionaire!", 0),
                    Pair.of("You can now see your income per second on the sidebar", 140)
            );
        }
    }

    public boolean onIslandUpgrade() {
        if (checkForIslandUpgrade) {
            checkForIslandUpgrade = false;
            setGoal(null);
            player.getTutorial().setShowUpgradesMenuIslands(true);

            msg(() -> schedule(160, () -> {
                        player.getTutorial().setComplete(true);
                        schedule(2, player::updateInventory);

                        ParticlesUtil.send(ParticleTypes.LAVA, getBukkitEntity().getLocation(), Triple.of(0.1F, 0.1F, 0.1F), 15, player);
                        discard();

                        msg(() -> {
                                },
                                Pair.of("Well this is it! Welcome to MineClick", 0),
                                Pair.of("Join our Discord server if you have any questions: /discord", 0)
                        );
                    }),
                    Pair.of("Fantastic! You unlocked your first Island Upgrade!", 0),
                    Pair.of("Soon you'll be able to unlock new Islands and multiply your income!", 60),
                    Pair.of("You can unlock Islands from the same Upgrades menu", 160)
            );

            return true;
        }
        return false;
    }
}

