package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.quest.QuestObjective;

import java.util.List;

public class DailySpleefQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Participate in " + count + " spleef games";
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(2, 5, 10, 20),
            new Stage(4, 7, 20, 40),
            new Stage(6, 9, 30, 60),
            new Stage(8, 11, 40, 80),
            new Stage(10, 13, 60, 100)
    );

    @Override
    public String getId() {
        return "dailySpleef";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Spleef daily quest";
    }
}
