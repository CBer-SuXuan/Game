package net.mineclick.game.type.quest.villager;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.service.GadgetsService;
import net.mineclick.game.service.LevelsService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.service.StatisticsService;
import net.mineclick.game.type.StatisticType;
import net.mineclick.game.type.quest.Quest;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.location.LocationParser;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Location;

import java.util.List;

public class CrimsonQuest extends VillagerQuest {
    @Getter
    private final List<QuestObjective> objectives = List.of(
            new QuestObjective.Talk(getVillagerName(), true) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    sendVillagerMessage(player, "Hey listen, we've got a bit of a problem here...");
                    sendVillagerMessage(player, 40, "The lobby is slowly being corrupted by crimson!");
                    sendVillagerMessage(player, 80, "I need your help stopping it before it gets out of control");
                    sendVillagerMessage(player, 140, "Find crimson blocks all over the lobby and break them with your pickaxe to get rid of it");
                    sendVillagerMessage(player, 240, "Break 50 blocks and I'll give you a reward, deal?");
                    sendVillagerMessage(player, 300, "Oh and take this grappling hook, it'll help you get around the lobby");

                    player.schedule(340, () -> {
                        player.popSound();
                        GadgetsService.i().addSecretGadget(player, "grapplingHook", true);
                        completeObjective(getQuestProgress(player));
                    });
                }
            },
            new QuestObjective("Break 50 crimson blocks in the lobby", 50) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    cooldown(player, 80);

                    sendVillagerMessage(player, "Doesn't seem like you've got 50 crimson blocks there, bud");
                    QuestProgress progress = getQuestProgress(player);
                    if (progress != null) {
                        sendVillagerMessage(player, 80, "You still need to break " + (getValue(player) - progress.getTaskProgress()) + " more blocks");
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

                    sendVillagerMessage(player, "You did great kid!");
                    sendVillagerMessage(player, 20, "Tho it seems it's still spreading...");
                    sendVillagerMessage(player, 60, "I tell you what, continue breaking those crimson blocks and I'll give you a reward for every 100 blocks you've broken");
                    sendVillagerMessage(player, 160, "How does that sound?");
                    sendVillagerMessage(player, 220, "You're a quiet one, aren't you... Well anyways, I'll see you around");

                    player.schedule(260, () -> completeObjective(getQuestProgress(player)));
                }
            }
    );
    private Location villagerLocation;

    @Override
    public String getId() {
        return "crimson";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Lobby cleanup";
    }

    @Override
    public String getVillagerName() {
        return "Reinald";
    }

    @Override
    public Pair<VillagerType, VillagerProfession> getVillagerType() {
        return Pair.of(VillagerType.SAVANNA, VillagerProfession.CARTOGRAPHER);
    }

    @Override
    public Location getVillagerLocation() {
        if (villagerLocation == null) {
            villagerLocation = LocationParser.parse("10.5 100 -1023.5 -90");
        }
        return villagerLocation;
    }

    @Override
    public boolean isVisible(GamePlayer player) {
        return true;
    }

    @Override
    public boolean hasPrerequisite(GamePlayer player) {
        cooldown(player, 100);

        if (LevelsService.i().getLevel(player.getExp()) < 3) {
            sendVillagerMessage(player, "Hey kid, I need a hand with something, but I'm afraid you're not strong enough to help me...");
            sendVillagerMessage(player, 100, "Come back when you reach level 3");

            return false;
        }

        return true;
    }

    @Override
    public int getExpReward(GamePlayer player) {
        return 100;
    }

    @Override
    public int getSchmeplsReward(GamePlayer player) {
        return 250;
    }

    @Override
    public void onVillagerClickAfterComplete(GamePlayer player) {
        cooldown(player, 240);

        sendVillagerMessage(player, "You cleared " + Formatter.format(StatisticsService.i().get(player.getUuid(), StatisticType.BROKEN_CRIMSON).getScore()) + " crimson blocks so far");
        sendVillagerMessage(player, 60, "For every 100 blocks cleared I'll give you 15 schmepls and 10 EXP");

        if (!QuestsService.i().unlockedQuest(player, "collector")) {
            VillagerQuest quest = (VillagerQuest) QuestsService.i().getQuest("collector");
            if (quest != null) {
                sendVillagerMessage(player, 140, "By the way, talk to my friend " + quest.getVillagerName() + ", he might have something for you");
                sendVillagerMessage(player, 240, "He's in that house behind me");
            }
        } else if (Game.getRandom().nextDouble() <= 0.3) {
            Quest quest = QuestsService.i().getQuest("parkourLobby");
            if (quest != null && !quest.isUnlocked(player)) {
                sendVillagerMessage(player, 140, "Psst! There's a good hiding spot that I know of...");
                sendVillagerMessage(player, 220, "It's behind the southern Villager statue. I hope no one else knows about it...");
            }
        }
    }
}
