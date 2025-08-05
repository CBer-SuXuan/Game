package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.quest.QuestObjective;

import java.util.List;

public class DailySuperBlockQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Mine " + count + " Super blocks";
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(5, 5, 10, 20),
            new Stage(8, 7, 20, 40),
            new Stage(10, 9, 30, 60),
            new Stage(20, 11, 40, 80),
            new Stage(40, 13, 60, 100),
            new Stage(60, 15, 80, 200),
            new Stage(125, 17, 100, 400),
            new Stage(250, 19, 200, 600)
    );

    @Override
    public String getId() {
        return "dailySuperBlock";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Super blocks daily quest";
    }
}
