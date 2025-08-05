package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.quest.QuestObjective;

import java.util.List;

public class DailyAscendQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Ascend " + count + " time" + (count == 1 ? "" : "s");
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(1, 5, 10, 20),
            new Stage(2, 7, 20, 40),
            new Stage(3, 9, 30, 80),
            new Stage(4, 11, 40, 100),
            new Stage(5, 13, 50, 200)
    );

    @Override
    public String getId() {
        return "dailyAscend";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Ascension daily quest";
    }
}
