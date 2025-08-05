package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.type.quest.Quest;
import net.mineclick.game.type.quest.QuestObjective;

import java.util.List;

public class DailySilverfishQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Get rid of " + count + " silverfish";
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(10, 5, 10, 40),
            new Stage(20, 7, 20, 80),
            new Stage(40, 9, 30, 120),
            new Stage(60, 11, 40, 160),
            new Stage(80, 13, 60, 200),
            new Stage(100, 15, 80, 400),
            new Stage(125, 17, 100, 600),
            new Stage(200, 19, 200, 800)
    );

    @Override
    public String getId() {
        return "dailySilverfish";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Silverfish daily quest";
    }

    @Override
    public boolean hasPrerequisite(GamePlayer player) {
        Quest quest = QuestsService.i().getQuest("campfire");
        return quest != null && quest.isComplete(player) && !player.getQuestsData().getCampfireData().isSilverfishDeal();
    }
}
