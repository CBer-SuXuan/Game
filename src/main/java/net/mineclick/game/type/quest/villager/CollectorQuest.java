package net.mineclick.game.type.quest.villager;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.location.LocationParser;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.List;

public class CollectorQuest extends VillagerQuest {
    private final List<String> returnObjectiveMessages = List.of(
            "Great! That's what I needed",
            "Great job, now let's see if it works...",
            "What took you so long? Anyways, let's do this",
            "Oh here you are! Thanks, that's what I needed",
            "Long time no see. This looks good, let me see what I can do...",
            "You're missing one block... Just kidding!"
    );
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective.Talk(getVillagerName(), true) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    sendVillagerMessage(player, "Hello " + player.getName());
                    sendVillagerMessage(player, 40, "I'm quite impressed with how you cleaned those crimson blocks");
                    sendVillagerMessage(player, 140, "But I think your Pickaxe can use some work");
                    sendVillagerMessage(player, 220, "Hmm... Alright...");
                    sendVillagerMessage(player, 260, "Bring me 500 Iron Ore blocks and I'll see what I can do");

                    player.schedule(300, () -> {
                        completeObjective(getQuestProgress(player));
                    });
                }
            },
            createMineObjective(500, "Iron Ore", Material.IRON_ORE),

            createReturnObjective("600 Spruce Log", 1.25),
            createMineObjective(600, "Spruce Log", Material.SPRUCE_LOG),

            createReturnObjective("700 Pumpkin", 1.5),
            createMineObjective(700, "Pumpkin", Material.CARVED_PUMPKIN),

            createReturnObjective("1000 Quartz Ore", 2),
            createMineObjective(1000, "Quartz Ore", Material.NETHER_QUARTZ_ORE),

            createReturnObjective("1200 Chiseled Quartz", 2.5),
            createMineObjective(1200, "Chiseled Quartz", Material.CHISELED_QUARTZ_BLOCK),

            createReturnObjective("1400 Quartz", 3),
            createMineObjective(1400, "Quartz", Material.QUARTZ_BLOCK),

            createReturnObjective("1500 Slime", 3.5),
            createMineObjective(1500, "Slime", Material.SLIME_BLOCK),

            createReturnObjective("2500 Slime", 4),
            createMineObjective(2500, "Slime", Material.SLIME_BLOCK),

            createReturnObjective("1500 Crimson", 4.5),
            createMineObjective(1500, "Crimson", Material.CRIMSON_HYPHAE),

            createReturnObjective(null, 5)
    );


    @Override
    public String getId() {
        return "collector";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Collector";
    }

    @Override
    public int getExpReward(GamePlayer player) {
        return 3000;
    }

    @Override
    public int getSchmeplsReward(GamePlayer player) {
        return 7000;
    }

    @Override
    public String getVillagerName() {
        return "Garrett";
    }

    @Override
    public Pair<VillagerType, VillagerProfession> getVillagerType() {
        return Pair.of(VillagerType.SAVANNA, VillagerProfession.MASON);
    }

    @Override
    public Location getVillagerLocation() {
        return LocationParser.parse("-29.5 102 -1015.5 -90");
    }

    @Override
    public boolean isVisible(GamePlayer player) {
        return QuestsService.i().completedQuest(player, "crimson");
    }

    private QuestObjective createReturnObjective(String nextObjective, double multiplier) {
        return new ReturnObjective(getVillagerName(), multiplier) {
            @Override
            public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                if (talkedBefore) {
                    return;
                }

                sendVillagerMessage(player, returnObjectiveMessages.get(Game.getRandom().nextInt(returnObjectiveMessages.size())));
                sendVillagerMessage(player, 80, "Hang on...");
                player.schedule(80, () -> player.playSound(Sound.BLOCK_ANVIL_USE, 0.5, 0.5));
                sendVillagerMessage(player, 120, "Ta-da! " + ChatColor.YELLOW + "Your " + ChatColor.GREEN + "Pickaxe " + ChatColor.YELLOW + "is now " + ChatColor.GREEN + Formatter.format(multiplier) + "x" + ChatColor.YELLOW + " more productive");

                if (nextObjective != null) {
                    sendVillagerMessage(player, 200, "This time, I'll need " + nextObjective + " blocks to further upgrade your Pickaxe");
                } else {
                    sendVillagerMessage(player, 200, "This is all I can do for you my friend. Unfortunately I cannot upgrade your Pickaxe any further");
                    sendVillagerMessage(player, 280, "Have fun with it!");
                }

                player.schedule(280, () -> {
                    completeObjective(getQuestProgress(player));
                    player.getPickaxe().recalculate();
                });
            }

            @Override
            public boolean hideAfterNextObjective() {
                return true;
            }
        };
    }

    @Override
    public void onVillagerClickAfterComplete(GamePlayer player) {
        sendVillagerMessage(player, "Sorry, but there's nothing else I can do for you");
    }

    private QuestObjective createMineObjective(int amount, String blockName, Material material) {
        return new MineObjective("Mine " + amount + " " + blockName + " blocks", amount, material) {
            @Override
            public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                if (!talkedBefore) {
                    sendVillagerMessage(player, "Bring me " + amount + " " + blockName + " blocks first");
                    return;
                }

                QuestProgress progress = getQuestProgress(player);
                if (progress != null) {
                    sendVillagerMessage(player, "You still need to mine " + (getValue(player) - progress.getTaskProgress()) + " more " + blockName + " blocks");
                }
            }

            @Override
            public boolean sendCompleteObjective() {
                return true;
            }
        };
    }

    public void checkMinedBlock(GamePlayer player, Material material) {
        QuestProgress progress = getQuestProgress(player);
        if (progress == null) {
            return;
        }

        QuestObjective objective = getObjective(progress.getObjective());
        if (objective instanceof MineObjective && ((MineObjective) objective).getMaterial().equals(material)) {
            QuestsService.i().incrementProgress(player, getId(), progress.getObjective(), 1);
        }
    }

    public double getMultiplier(GamePlayer player) {
        QuestProgress progress = getQuestProgress(player);
        if (progress == null) return 1;

        double multiplier = 1;
        for (int i = 0; i < progress.getObjective() && i < objectives.size(); i++) {
            QuestObjective objective = objectives.get(i);
            if (objective instanceof ReturnObjective) {
                multiplier *= ((ReturnObjective) objective).getMultiplier();
            }
        }

        return multiplier;
    }

    private static class MineObjective extends QuestObjective {
        @Getter
        private final Material material;

        public MineObjective(String name, int amount, Material material) {
            super(name, amount);
            this.material = material;
        }
    }

    private static class ReturnObjective extends QuestObjective.Talk {
        @Getter
        private final double multiplier;

        public ReturnObjective(String villagerName, double multiplier) {
            super(villagerName, false);
            this.multiplier = multiplier;
        }
    }
}
