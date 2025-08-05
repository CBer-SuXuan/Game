package net.mineclick.game.type.quest.daily;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.SkillsService;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.game.type.skills.SkillType;

import java.util.List;

public class DailyCookiesQuest extends DailyQuest {
    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective() {
                @Override
                public String getName(GamePlayer player) {
                    int count = getCurrentStage(player).getValue();
                    return "Collect " + count + " cookie" + (count == 1 ? "" : "s");
                }
            }
    );
    @Getter
    public List<Stage> stages = List.of(
            new Stage(1, 5, 15, 25),
            new Stage(5, 7, 25, 45),
            new Stage(10, 9, 35, 65),
            new Stage(20, 11, 45, 85),
            new Stage(30, 13, 55, 105),
            new Stage(40, 15, 65, 125),
            new Stage(50, 17, 75, 145),
            new Stage(60, 19, 85, 165),
            new Stage(80, 21, 95, 185),
            new Stage(100, 23, 110, 220)
    );

    @Override
    public String getId() {
        return "dailyCookies";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Cookies daily quest";
    }

    @Override
    public boolean hasPrerequisite(GamePlayer player) {
        return SkillsService.i().has(player, SkillType.COOKIE_1);
    }
}
