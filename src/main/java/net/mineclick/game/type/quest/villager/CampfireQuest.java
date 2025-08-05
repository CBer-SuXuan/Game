package net.mineclick.game.type.quest.villager;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.CampfireData;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.model.Statistic;
import net.mineclick.game.service.GeodesService;
import net.mineclick.game.service.LobbyService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.service.StatisticsService;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.StatisticType;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.*;
import net.mineclick.global.util.location.LocationParser;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CampfireQuest extends VillagerQuest {
    public final static Set<Location> CAMPFIRES = Set.of(
            LocationParser.parse("-35 92 -955"),
            LocationParser.parse("102 99 -945"),
            LocationParser.parse("85 96 -1039"),
            LocationParser.parse("127 95 -1047"),
            LocationParser.parse("96 94 -1102"),
            LocationParser.parse("25 96 -1125"),
            LocationParser.parse("-54 102 -1104"),
            LocationParser.parse("-41 90 -1052"),
            LocationParser.parse("-93 93 -1050")
    );
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective.Talk(getVillagerName(), true) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    sendVillagerMessage(player, "Don't you just like sitting by the fire and enjoying its warmth?");
                    sendVillagerMessage(player, 100, "So nice and comfy. Too bad we can't have any more campfires...");

                    player.schedule(200, () -> completeObjective(getQuestProgress(player)));
                }
            },
            new QuestObjective("Light up campfires in the lobby", CAMPFIRES.size()) {
                @Override
                public boolean sendCompleteObjective() {
                    return true;
                }
            },
            new QuestObjective.Talk(getVillagerName(), false) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    sendVillagerMessage(player, "Oh no! What have you done?");
                    sendVillagerMessage(player, 60, "All those campfires are attracting lots of silverfish!");
                    sendVillagerMessage(player, 160, "I should've told you, we can only have one campfire here");
                    sendVillagerMessage(player, 260, "We need to get rid of them before they destroy everything!");
                    sendVillagerMessage(player, 340, "Use your Pickaxe, it'll help you");
                    sendVillagerMessage(player, 420, "Quickly! You need to put out all those campfires");

                    player.schedule(480, () -> completeObjective(getQuestProgress(player)));
                }
            },
            new QuestObjective("Put out campfires in the lobby", CAMPFIRES.size()) {
                @Override
                public String getName(GamePlayer player) {
                    return player.getQuestsData().getCampfireData().isSilverfishDeal() ? "Make a deal with silverfish" : "Put out campfires in the lobby";
                }

                @Override
                public int getValue(GamePlayer player) {
                    return player.getQuestsData().getCampfireData().isSilverfishDeal() ? 1 : CAMPFIRES.size();
                }

                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    cooldown(player, 80);

                    sendVillagerMessage(player, "What are you doing here?");
                    QuestProgress progress = getQuestProgress(player);
                    if (progress != null) {
                        sendVillagerMessage(player, 40, "There's still " + (getValue(player) - progress.getTaskProgress()) + " more campfires to put out");
                    }
                }

                @Override
                public boolean sendCompleteObjective() {
                    return true;
                }
            },
            new QuestObjective.Talk(getVillagerName(), false) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    if (!player.getQuestsData().getCampfireData().isSilverfishDeal()) {
                        sendVillagerMessage(player, "Phew! Thanks for getting silverfish under control");
                        sendVillagerMessage(player, 100, "However, I'm sure it wasn't all of them...");
                        sendVillagerMessage(player, 180, "I'll pay you if you continue getting rid of them");
                        player.schedule(240, () -> completeObjective(getQuestProgress(player)));
                    } else {
                        sendVillagerMessage(player, "You did what!?");
                        sendVillagerMessage(player, 40, "Are you sure you can trust them?");
                        player.schedule(100, () -> completeObjective(getQuestProgress(player)));
                    }
                }
            }
    );

    @Override
    public String getId() {
        return "campfire";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Campfire";
    }

    @Override
    public int getExpReward(GamePlayer player) {
        return player.getQuestsData().getCampfireData().isSilverfishDeal() ? 100 : 350;
    }

    @Override
    public int getSchmeplsReward(GamePlayer player) {
        return player.getQuestsData().getCampfireData().isSilverfishDeal() ? 300 : 980;
    }

    @Override
    public String getVillagerName() {
        return "Jack";
    }

    @Override
    public Pair<VillagerType, VillagerProfession> getVillagerType() {
        return Pair.of(VillagerType.DESERT, VillagerProfession.BUTCHER);
    }

    @Override
    public Location getVillagerLocation() {
        return LocationParser.parse("-54.5 99 -1000.5 -25");
    }

    @Override
    public boolean isVisible(GamePlayer player) {
        return true;
    }

    @Override
    public void onVillagerClickAfterComplete(GamePlayer player) {
        cooldown(player, 140);

        if (!player.getQuestsData().getCampfireData().isSilverfishDeal()) {
            long count = (long) StatisticsService.i().get(player.getUuid(), StatisticType.SILVERFISH).getScore();

            if (count == 0) {
                sendVillagerMessage(player, "Nice and comfy...");
                sendVillagerMessage(player, 40, "I tell you what, I'll pay you 30 EXP and 60 schmepls for every 50 silverfish you get rid of");
                sendVillagerMessage(player, 140, "I'm sure you'll find a lot of them by the campfires");
            } else {
                sendVillagerMessage(player, 40, "You've got " + count + " silverfish so far");
                sendVillagerMessage(player, 100, "Keep going and I'll pay you 30 EXP and 60 schmepls for every 50 silverfish you get rid of");
            }
        } else {
            sendVillagerMessage(player, "I wouldn't trust those silverfish if I was you...");
        }
    }

    public void checkCampfire(GamePlayer player) {
        if (LobbyService.i().isInLobby(player)) {
            for (Location campfire : player.getQuestsData().getCampfireData().getLitCampfires()) {
                sendBlockUpdate(player, campfire, true);
            }
        }
    }

    public void checkCampfireClick(GamePlayer player, Block block) {
        Location location = block.getLocation();

        if (CAMPFIRES.contains(location)) {
            QuestProgress questProgress = getQuestProgress(player);
            if (questProgress == null) {
                return;
            }
            CampfireData campfireData = player.getQuestsData().getCampfireData();

            // spawn silverfish after completing the quest
            if (questProgress.isComplete()) {
                if (campfireData.isSilverfishDeal()) {
                    long now = System.currentTimeMillis();
                    long next = campfireData.getNextSilverfishDelivery().toEpochMilli();
                    if (now > next) {
                        Rarity rarity = GeodesService.i().addGeode(player);
                        player.sendMessage(ChatColor.GREEN + "Silverfish have found something for you: " + rarity.getGeodeName() + " geode");

                        player.playSound(Sound.ENTITY_SILVERFISH_AMBIENT);
                        campfireData.setNextSilverfishDelivery(Instant.now().plus(4, ChronoUnit.HOURS));
                    } else {
                        player.sendMessage("There's no silverfish around right now", MessageType.ERROR);
                        player.sendMessage("Come back in " + Formatter.duration(next - now), MessageType.ERROR);

                        player.noSound();
                    }
                } else {
                    int toSpawn = 10 - campfireData.getSilverfish(location).size();
                    if (toSpawn > 0) {
                        spawnSilverfish(player, location.getBlock(), toSpawn);
                    }
                }

                return;
            }

            int objective = questProgress.getObjective();
            if (objective == 1) {
                if (campfireData.getLitCampfires().add(location)) {
                    QuestsService.i().incrementProgress(player, getId(), 1, 1);
                    player.playSound(Sound.ITEM_FLINTANDSTEEL_USE);
                }
                sendBlockUpdate(player, location, true);
            } else if (objective == 3) {
                if (campfireData.getLitCampfires().contains(location)) {
                    if (!campfireData.getFightingCampfires().containsKey(location)) {
                        campfireData.getFightingCampfires().put(location, 5);

                        spawnSilverfish(player, location.getBlock(), 5);
                    } else if (campfireData.getFightingCampfires().get(location) <= 0) {
                        if (campfireData.getLitCampfires().size() == CAMPFIRES.size()) {
                            if (!campfireData.isSilverfishTalking()) {
                                silverfishTalk(player, location);
                                campfireData.setSilverfishTalking(true);
                            }

                            if (campfireData.isSilverfishFinishedTalking()) {
                                openSilverfishMenu(player, location);
                            }
                        } else {
                            campfireData.getLitCampfires().remove(location);
                            QuestsService.i().incrementProgress(player, getId(), 3, 1);

                            player.playSound(Sound.BLOCK_FIRE_EXTINGUISH);
                            sendBlockUpdate(player, location, false);
                        }
                    } else {
                        int countLeft = campfireData.getFightingCampfires().get(location);
                        int toSpawn = countLeft - campfireData.getSilverfish(location).size();
                        if (toSpawn > 0) {
                            spawnSilverfish(player, location.getBlock(), toSpawn);
                        }

                        player.sendMessage("Get rid of " + countLeft + " more silverfish to put out this campfire", MessageType.ERROR);
                    }
                }
            }
        }
    }

    private void silverfishTalk(GamePlayer player, Location campfire) {
        player.playSound(Sound.ENTITY_SILVERFISH_AMBIENT);
        player.sendMessage(ChatColor.GRAY + "Silverfish: " + ChatColor.WHITE + "Wait! Please hear me out");
        player.schedule(80, () -> {
            player.playSound(Sound.ENTITY_SILVERFISH_AMBIENT);
            player.sendMessage(ChatColor.GRAY + "Silverfish: " + ChatColor.WHITE + "We're not causing any harm, we're just seeking warmth by the campfire");
        });
        player.schedule(180, () -> {
            player.playSound(Sound.ENTITY_SILVERFISH_AMBIENT);
            player.sendMessage(ChatColor.GRAY + "Silverfish: " + ChatColor.WHITE + "Please let us stay and we'll return the favour");
        });
        player.schedule(260, () -> {
            player.getQuestsData().getCampfireData().setSilverfishFinishedTalking(true);
            openSilverfishMenu(player, campfire);
        });
    }

    private void openSilverfishMenu(GamePlayer player, Location campfire) {
        MenuUtil.openConfirmationMenu(player, aBoolean -> {
            player.getQuestsData().getCampfireData().setSilverfishDeal(aBoolean);
            player.getQuestsData().getCampfireData().setSilverfishTalking(false);
            if (aBoolean) {
                player.expSound();

                player.sendMessage(ChatColor.GRAY + "Silverfish: " + ChatColor.WHITE + "Thank you! We will never forget this...");
                player.schedule(60, () -> {
                    player.playSound(Sound.ENTITY_SILVERFISH_AMBIENT);
                    player.sendMessage(ChatColor.GRAY + "Silverfish: " + ChatColor.WHITE + "Come to the campfire once in a while, we might have something for you");
                });
            } else {
                player.playSound(Sound.ENTITY_SILVERFISH_DEATH, 1, 0.3);

                player.getQuestsData().getCampfireData().getLitCampfires().remove(campfire);
                sendBlockUpdate(player, campfire, false);
            }

            QuestsService.i().incrementProgress(player, getId(), 3, 1);
        }, ChatColor.RED + "Do you want to", ChatColor.RED + "make a deal with silverfish?");
    }

    private void spawnSilverfish(GamePlayer player, Block campfire, int count) {
        List<Block> blocks = new ArrayList<>();
        recursiveBlockSearch(Material.GRASS_BLOCK, campfire, blocks);
        for (int i = 0; i < count; i++) {
            Location location = blocks.isEmpty() ? campfire.getLocation() : blocks.get(Game.getRandom().nextInt(blocks.size())).getLocation();
            location.add(0.5, 1, 0.5);

            Silverfish silverfish = new Silverfish(EntityType.SILVERFISH, ((CraftWorld) location.getWorld()).getHandle()) {
                @Override
                public void tick() {
                    if (player.isOffline() || (tickCount % 60 == 0 && getBukkitEntity().getLocation().distanceSquared(player.getPlayer().getLocation()) > 100)) {
                        discard();
                        return;
                    }
                    if (player.getQuestsData().getCampfireData().isSilverfishTalking()) {
                        return;
                    }

                    super.tick();
                }

                @Override
                public boolean hurt(DamageSource damagesource, float f) {
                    if (player.getQuestsData().getCampfireData().isSilverfishTalking() || player.getQuestsData().getCampfireData().isSilverfishDeal()) {
                        return false;
                    }

                    if (Objects.equals(damagesource.getMsgId(), "player")) {
                        discard();

                        ParticlesUtil.send(ParticleTypes.CLOUD, getBukkitEntity().getLocation(), Triple.of(0.2F, 0.2F, 0.2F), 0, 5, Collections.singleton(player));
                        player.expSound();

                        QuestProgress progress = getQuestProgress(player);
                        if (progress != null && progress.isComplete()) {
                            StatisticsService.i().increment(player.getUuid(), StatisticType.SILVERFISH);
                            Statistic statistic = StatisticsService.i().get(player.getUuid(), StatisticType.SILVERFISH);
                            if (statistic != null && statistic.getScore() % 50 == 0) {
                                player.sendMessage(ChatColor.GREEN + "You got rid of " + Formatter.format(statistic.getScore()) + " silverfish!");
                                player.sendMessage(ChatColor.GREEN + "+" + ChatColor.YELLOW + "60" + ChatColor.AQUA + " schmepls");
                                player.sendMessage(ChatColor.GREEN + "+" + ChatColor.YELLOW + "30" + ChatColor.AQUA + " EXP");
                                player.addSchmepls(60);
                                player.addExp(30);
                                player.levelUpSound();
                            }

                            // Check daily quest
                            QuestsService.i().incrementProgress(player, "dailySilverfish", 0, 1);
                        } else {
                            player.getQuestsData().getCampfireData().getFightingCampfires().computeIfPresent(campfire.getLocation(), (loc, count) -> Math.max(0, count - 1));
                        }
                    }
                    return false;
                }

                @Override
                protected void registerGoals() {
                    this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1, 10));
                }
            };
            silverfish.moveTo(location.getX(), location.getY(), location.getZ());
            player.getAllowedEntities().add(silverfish.getId());
            silverfish.level().addFreshEntity(silverfish, CreatureSpawnEvent.SpawnReason.CUSTOM);
            ParticlesUtil.sendBlock(location, Material.STONE, Triple.of(0.2F, 0.2F, 0.2F), 0, 10, Collections.singleton(player));

            player.getQuestsData().getCampfireData().getSilverfish().add(silverfish);
        }
    }

    private void recursiveBlockSearch(Material material, Block block, List<Block> blocks) {
        if (blocks.size() >= 25 || blocks.contains(block)) return;

        if (block.getType().equals(material)) {
            blocks.add(block);
        }
        for (BlockFace mainFace : new BlockFace[]{BlockFace.SELF, BlockFace.DOWN, BlockFace.UP}) {
            Block center = block.getRelative(mainFace);

            for (int i = 0; i < 4; i++) {
                Block relative = center.getRelative(BlockFace.values()[Game.getRandom().nextInt(4)]);
                if (relative.getType().equals(material)) {
                    recursiveBlockSearch(material, relative, blocks);
                }
            }
        }
    }

    private void sendBlockUpdate(GamePlayer player, Location location, boolean lit) {
        player.sendBlockChange(location, Bukkit.createBlockData(Material.CAMPFIRE, "[lit=" + lit + "]"));
    }
}
