package net.mineclick.game.type.quest.villager;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.location.LocationParser;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Location;

import java.util.List;

public class ParkourLobbyQuest extends VillagerQuest {
    @Getter
    private final List<QuestObjective> objectives = List.of(
            new QuestObjective.Talk(getVillagerName(), true) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    sendVillagerMessage(player, "Oh hi, how did you find my secret hiding spot?");
                    sendVillagerMessage(player, 60, "I come here often to just... you know... hide from everyone");
                    sendVillagerMessage(player, 160, "Anyways, I've got something I want to ged rid of, but I can't decide who I should give it to...");
                    sendVillagerMessage(player, 260, "Oh! I've got an idea!");
                    sendVillagerMessage(player, 300, "Prove me that you're good at parkour and it's yours!");
                    sendVillagerMessage(player, 380, "What is this item you ask? Well, there's only one way to find out haha");
                    sendVillagerMessage(player, 480, "Complete 5 parkour courses of your choice to prove you're worthy");
                    sendVillagerMessage(player, 560, "Come back once you've done that and I'll give you something special");

                    player.schedule(640, () -> completeObjective(getQuestProgress(player)));
                }
            },
            new QuestObjective("Finish 5 parkour courses", 5) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    QuestProgress progress = getQuestProgress(player);
                    if (progress == null) {
                        return;
                    }

                    int taskProgress = progress.getTaskProgress();
                    if (taskProgress == 0) {
                        sendVillagerMessage(player, "What are you waiting for? Go and complete 5 parkour courses!");
                    } else {
                        cooldown(player, 60);
                        sendVillagerMessage(player, "Not bad, you've completed " + taskProgress + " parkour courses");
                        sendVillagerMessage(player, 60, getValue(player) - taskProgress + " more to go!");
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

                    sendVillagerMessage(player, "All done? Amazing!");
                    sendVillagerMessage(player, 60, "Here are 2 of my own checkpoint markers");
                    sendVillagerMessage(player, 140, "Don't worry, I won't need them. I think I'll just stay here for a while haha");
                    sendVillagerMessage(player, 220, "By the way, I have something else that I can give you");
                    sendVillagerMessage(player, 300, "But you need to show me you're really worth this...");
                    sendVillagerMessage(player, 360, "This time, complete 15 parkour courses");
                    sendVillagerMessage(player, 420, "I'll be waiting here haha");

                    player.schedule(460, () -> {
                        player.getParkour().setCheckpoints(player.getParkour().getCheckpoints() + 2);
                        completeObjective(getQuestProgress(player));
                    });
                }
            },
            new QuestObjective("Finish 15 parkour courses", 15) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    QuestProgress progress = getQuestProgress(player);
                    if (progress == null) {
                        return;
                    }

                    int taskProgress = progress.getTaskProgress();
                    if (taskProgress == 0) {
                        sendVillagerMessage(player, "What? Too much? Haha, I'm sure you can do it!");
                    } else {
                        cooldown(player, 60);
                        sendVillagerMessage(player, "Not bad, you've completed " + taskProgress + " parkour courses");
                        sendVillagerMessage(player, 60, getValue(player) - taskProgress + " more to go!");
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

                    sendVillagerMessage(player, "Wow you did it!");
                    sendVillagerMessage(player, 20, "I'm impressed!");
                    sendVillagerMessage(player, 80, "Now where was it...");
                    sendVillagerMessage(player, 140, "Ah yes! Here they are, my old running shoes!");
                    sendVillagerMessage(player, 220, "Enjoy! I'm sure you'll find more use for them haha");

                    player.schedule(300, () -> {
                        player.getParkour().setShoesUnlocked(true);
                        player.updateInventory();
                        completeObjective(getQuestProgress(player));
                    });
                }
            }
    );

    @Override
    public String getId() {
        return "parkourLobby";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Parkour challenge";
    }

    @Override
    public String getVillagerName() {
        return "Bunny";
    }

    @Override
    public Pair<VillagerType, VillagerProfession> getVillagerType() {
        return Pair.of(VillagerType.SNOW, VillagerProfession.SHEPHERD);
    }

    @Override
    public Location getVillagerLocation() {
        return LocationParser.parse("30.5 91 -748.5 -140");
    }

    @Override
    public boolean isVisible(GamePlayer player) {
        return true;
    }

    @Override
    public int getExpReward(GamePlayer player) {
        return 250;
    }

    @Override
    public int getSchmeplsReward(GamePlayer player) {
        return 400;
    }
}
