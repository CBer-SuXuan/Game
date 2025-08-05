package net.mineclick.game.type.quest.daily;

import lombok.Value;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.service.LevelsService;
import net.mineclick.game.type.quest.Quest;

import java.util.List;

public abstract class DailyQuest extends Quest {

    public abstract List<Stage> getStages();

    public Stage getCurrentStage(GamePlayer player) {
        List<Stage> stages = getStages();
        QuestProgress progress = getQuestProgress(player);
        Stage stage = stages.iterator().next();
        int level = LevelsService.i().getLevel(player.getExp());

        if (progress != null && progress.getCompletedCount() != 0) {
            int i = 0;
            for (Stage entry : stages) {
                if (entry.getMinLevel() > level || i > progress.getCompletedCount()) {
                    break;
                }
                stage = entry;

                i++;
            }
        }

        return stage;
    }

    public int getCompletedCount(GamePlayer player) {
        QuestProgress questProgress = getQuestProgress(player);
        return questProgress == null ? 0 : questProgress.getCompletedCount();
    }

    public int getRandomObjectiveIndex() {
        return Game.getRandom().nextInt(getObjectives().size());
    }

    @Override
    public int getExpReward(GamePlayer player) {
        return getCurrentStage(player).getExpReward();
    }

    @Override
    public int getSchmeplsReward(GamePlayer player) {
        return getCurrentStage(player).getSchmeplsReward();
    }

    @Value
    public static class Stage {
        int value;
        int minLevel;
        int expReward;
        int schmeplsReward;
    }
}
