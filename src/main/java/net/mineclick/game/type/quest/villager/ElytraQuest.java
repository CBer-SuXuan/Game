package net.mineclick.game.type.quest.villager;

import lombok.Getter;
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
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.List;

public class ElytraQuest extends VillagerQuest {
    public static final Location FROZEN_ELYTRA_LOCATION = LocationParser.parse("42 137 -1006");

    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective.Talk(getVillagerName(), true) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    sendVillagerMessage(player, "Fancy seeing you here, " + player.getName());
                    sendVillagerMessage(player, 60, "I'm in a bit of a pickle here...");
                    sendVillagerMessage(player, 120, "I like climbing up trees and flying down with my elytra");
                    sendVillagerMessage(player, 220, "But this time I forgot my elytra and now I can't get down!");
                    sendVillagerMessage(player, 320, "Please help me find it. I don't remember where I left it, but I know it was cold there");
                    sendVillagerMessage(player, 420, "I would really appreciate your help!");

                    player.schedule(500, () -> {
                        completeObjective(getQuestProgress(player));

                        player.schedule(60, () -> placeElytraBlock(player));
                    });
                }
            },
            new QuestObjective("Find " + getVillagerName() + "'s elytra", 1) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    cooldown(player, 100);

                    sendVillagerMessage(player, "Please help me find it");
                    sendVillagerMessage(player, 40, "I don't remember where I left it, but I know it was cold there");
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

                    sendVillagerMessage(player, "Huh so you've actually found it?");
                    sendVillagerMessage(player, 60, "Turns out I had another one in my second pocket! Silly me");
                    sendVillagerMessage(player, 160, "I suppose you can keep this one");
                    sendVillagerMessage(player, 220, "Cheers!");

                    player.schedule(240, () -> completeObjective(getQuestProgress(player)));
                }
            }
    );

    public void placeElytraBlock(GamePlayer player) {
        QuestProgress progress = getQuestProgress(player);
        if (progress != null && progress.getObjective() == 1) {
            Block block = FROZEN_ELYTRA_LOCATION.getBlock();
            if (DynamicMineBlocksService.i().get(player, block) == null) {
                DynamicMineBlocksService.i().create(DynamicMineBlockType.FROZEN_ELYTRA, block, Material.BLUE_ICE, 150, Collections.singleton(player))
                        .setBreakConsumer((p, b) -> {
                            QuestsService.i().incrementProgress(player, "elytra", 1, 1);

                            player.getParkour().setElytraUnlocked(true);
                            player.updateInventory();
                        });
            }
        }
    }

    public void tickBlock(GamePlayer player) {
        QuestProgress progress = getQuestProgress(player);
        if (progress != null && progress.getObjective() == 1) {
            ParticlesUtil.send(ParticleTypes.FIREWORK, FROZEN_ELYTRA_LOCATION.clone().add(0.5, 0.5, 0.5), Triple.of(0.3F, 0.3F, 0.3F), 0, 1, Collections.singleton(player));
        }
    }

    @Override
    public String getId() {
        return "elytra";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Elytra";
    }

    @Override
    public int getExpReward(GamePlayer player) {
        return 80;
    }

    @Override
    public int getSchmeplsReward(GamePlayer player) {
        return 180;
    }

    @Override
    public String getVillagerName() {
        return "Jenna";
    }

    @Override
    public Pair<VillagerType, VillagerProfession> getVillagerType() {
        return Pair.of(VillagerType.SWAMP, VillagerProfession.LEATHERWORKER);
    }

    @Override
    public Location getVillagerLocation() {
        return LocationParser.parse("50.5 123 -1181.5 60");
    }

    @Override
    public boolean isVisible(GamePlayer player) {
        return !QuestsService.i().completedQuest(player, getId());
    }
}
