package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.type.quest.Quest;
import net.mineclick.game.type.quest.QuestObjective;

import java.util.List;

public class DailyCrimsonQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Break " + count + " crimson blocks";
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(5, 5, 10, 40),
            new Stage(8, 7, 20, 80),
            new Stage(10, 9, 30, 120),
            new Stage(20, 11, 40, 160),
            new Stage(40, 13, 60, 200),
            new Stage(60, 15, 80, 400),
            new Stage(125, 17, 100, 600),
            new Stage(250, 19, 200, 800)
    );

    @Override
    public String getId() {
        return "dailyCrimson";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Crimson cleanup daily quest";
    }

    @Override
    public boolean hasPrerequisite(GamePlayer player) {
        Quest quest = QuestsService.i().getQuest("crimson");
        return quest != null && quest.isUnlocked(player);
    }
}
