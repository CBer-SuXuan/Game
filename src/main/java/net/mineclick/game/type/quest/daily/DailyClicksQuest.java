package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.quest.QuestObjective;

import java.util.List;

public class DailyClicksQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Mine " + count + " blocks";
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(50, 5, 10, 20),
            new Stage(100, 7, 20, 40),
            new Stage(250, 9, 30, 60),
            new Stage(500, 11, 40, 80),
            new Stage(1000, 13, 60, 100),
            new Stage(2000, 15, 80, 200),
            new Stage(4000, 17, 100, 400),
            new Stage(6000, 19, 200, 600),
            new Stage(8000, 21, 300, 800),
            new Stage(10000, 23, 500, 1000)
    );

    @Override
    public String getId() {
        return "dailyClicks";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Click blocks daily quest";
    }
}
