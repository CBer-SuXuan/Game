package net.mineclick.game.type.quest.villager;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.service.DynamicMineBlocksService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.type.DynamicMineBlockType;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.mineclick.global.util.location.LocationParser;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Collections;
import java.util.List;

public class BookshelvesQuest extends VillagerQuest {
    private static final List<Location> LOCATIONS = List.of(
            LocationParser.parse("-12 108 -1079"),
            LocationParser.parse("-12 107 -1079"),
            LocationParser.parse("-17 107 -1080"),
            LocationParser.parse("-17 109 -1082"),
            LocationParser.parse("-17 108 -1082"),
            LocationParser.parse("-17 107 -1086"),
            LocationParser.parse("-12 108 -1088"),
            LocationParser.parse("-12 107 -1088"),
            LocationParser.parse("-8 108 -1084"),
            LocationParser.parse("-8 107 -1084"),
            LocationParser.parse("-9 107 -1082"),
            LocationParser.parse("-8 90 -1085"),
            LocationParser.parse("-7 90 -1085"),
            LocationParser.parse("-7 89 -1085"),
            LocationParser.parse("-9 89 -1085"),
            LocationParser.parse("-3 89 -1080"),
            LocationParser.parse("-3 88 -1080")
    );

    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective.Talk(getVillagerName(), true) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    sendVillagerMessage(player, "Here you are, I heard you can help me with something");
                    sendVillagerMessage(player, 80, "We've got a bit of a situation here...");
                    sendVillagerMessage(player, 140, "Silverfish have taken over some of our bookshelves and are ruining the books");
                    sendVillagerMessage(player, 240, "I think your pickaxe should be strong enough to dust them out");

                    player.schedule(320, () -> {
                        completeObjective(getQuestProgress(player));
                        player.schedule(60, () -> placeBlocks(player));
                    });
                }
            },
            new QuestObjective("Dust out " + LOCATIONS.size() + " bookshelves", LOCATIONS.size()) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    cooldown(player, 100);

                    sendVillagerMessage(player, "Use your pickaxe, it'll help you to get them out");
                    QuestProgress progress = getQuestProgress(player);
                    if (progress != null) {
                        sendVillagerMessage(player, 40, "There's still " + (LOCATIONS.size() - progress.getTaskProgress()) + " more bookshelves to dust out");
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

                    sendVillagerMessage(player, "Brilliant! Thank you for your help");
                    sendVillagerMessage(player, 60, "Hopefully they won't come back again");

                    player.schedule(100, () -> completeObjective(getQuestProgress(player)));
                }
            }
    );

    public void placeBlocks(GamePlayer player) {
        QuestProgress progress = getQuestProgress(player);
        if (progress != null && progress.getObjective() == 1) {
            for (Location location : LOCATIONS) {
                if (player.getQuestsData().getBookshelvesData().getDustedLocations().contains(location)) {
                    continue;
                }

                Block block = location.getBlock();
                if (DynamicMineBlocksService.i().get(player, block) == null) {
                    DynamicMineBlocksService.i().create(DynamicMineBlockType.BOOKSHELF, block, Material.BOOKSHELF, 20, Collections.singleton(player))
                            .setBreakConsumer((p, b) -> {
                                QuestsService.i().incrementProgress(player, "bookshelves", 1, 1);
                                player.getQuestsData().getBookshelvesData().getDustedLocations().add(location);
                                player.expSound();

                                for (int i = 0; i < Game.getRandom().nextInt(3) + 1; i++) {
                                    spawnSilverfish(player, block);
                                }
                            });
                }
            }
        }
    }

    private void spawnSilverfish(GamePlayer player, Block block) {
        Silverfish silverfish = new Silverfish(EntityType.SILVERFISH, ((CraftWorld) block.getWorld()).getHandle()) {
            @Override
            public void tick() {
                super.tick();

                if (tickCount >= 40 && Game.getRandom().nextInt(10) == 0) {
                    discard();

                    if (!player.isOffline()) {
                        ParticlesUtil.send(ParticleTypes.CLOUD, getBukkitEntity().getLocation(), Triple.of(0.2F, 0.2F, 0.2F), 5, Collections.singleton(player));
                        player.playSound(Sound.ENTITY_SILVERFISH_DEATH);
                    }
                }
            }

            @Override
            protected void registerGoals() {
                this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1, 1));
            }
        };
        silverfish.moveTo(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
        silverfish.setInvulnerable(true);

        player.getAllowedEntities().add(silverfish.getId());
        silverfish.level().addFreshEntity(silverfish, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    public void tickBlocks(GamePlayer player) {
        QuestProgress progress = getQuestProgress(player);
        if (progress != null && progress.getObjective() == 1) {
            for (Location location : LOCATIONS) {
                if (player.getQuestsData().getBookshelvesData().getDustedLocations().contains(location)) {
                    continue;
                }

                if (Game.getRandom().nextBoolean()) {
                    ParticlesUtil.sendBlock(location.clone().add(0.5, 0.5, 0.5), Material.STONE, Triple.of(0.5F, 0.5F, 0.5F), 0, 10, Collections.singleton(player));
                }
            }
        }
    }

    @Override
    public String getId() {
        return "bookshelves";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Dusty bookshelves";
    }

    @Override
    public int getExpReward(GamePlayer player) {
        return 80;
    }

    @Override
    public int getSchmeplsReward(GamePlayer player) {
        return 160;
    }

    @Override
    public String getVillagerName() {
        return "Herbert";
    }

    @Override
    public Pair<VillagerType, VillagerProfession> getVillagerType() {
        return Pair.of(VillagerType.JUNGLE, VillagerProfession.CARTOGRAPHER);
    }

    @Override
    public Location getVillagerLocation() {
        return LocationParser.parse("-12.5 107 -1075.5");
    }

    @Override
    public boolean isVisible(GamePlayer player) {
        return true;
    }
}
