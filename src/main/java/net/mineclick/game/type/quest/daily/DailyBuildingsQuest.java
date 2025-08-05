package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.quest.QuestObjective;

import java.util.List;

public class DailyBuildingsQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Unlock/upgrade " + count + " island buildings";
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(2, 5, 20, 40),
            new Stage(3, 7, 30, 60),
            new Stage(4, 9, 40, 80),
            new Stage(5, 11, 50, 100),
            new Stage(6, 13, 60, 120),
            new Stage(7, 15, 70, 140),
            new Stage(8, 17, 80, 160),
            new Stage(9, 21, 90, 180),
            new Stage(10, 23, 100, 200)
    );

    @Override
    public String getId() {
        return "dailyBuildings";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Buildings daily quest";
    }
}
