package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.SkillsService;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.game.type.skills.SkillType;

import java.util.List;

public class DailyTraderQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Buy " + count + " item" + (count == 1 ? "" : "s") + " from the Wandering Trader";
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(1, 5, 15, 25),
            new Stage(2, 7, 25, 45),
            new Stage(3, 9, 35, 65),
            new Stage(4, 11, 45, 85),
            new Stage(5, 13, 55, 105),
            new Stage(6, 15, 65, 125),
            new Stage(7, 17, 75, 145),
            new Stage(8, 19, 85, 165),
            new Stage(9, 21, 95, 185),
            new Stage(10, 23, 110, 220)
    );

    @Override
    public String getId() {
        return "dailyTrader";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Trader daily quest";
    }

    @Override
    public boolean hasPrerequisite(GamePlayer player) {
        return SkillsService.i().has(player, SkillType.TRADER_1);
    }
}
