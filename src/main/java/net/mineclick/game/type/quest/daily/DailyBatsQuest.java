package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.quest.QuestObjective;

import java.util.List;

public class DailyBatsQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Catch " + count + " golden bat" + (count == 1 ? "" : "s");
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(1, 5, 20, 10),
            new Stage(2, 7, 30, 25),
            new Stage(3, 9, 40, 50),
            new Stage(4, 11, 50, 100),
            new Stage(5, 13, 60, 200)
    );

    @Override
    public String getId() {
        return "dailyBats";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Golden bat daily quest";
    }
}
