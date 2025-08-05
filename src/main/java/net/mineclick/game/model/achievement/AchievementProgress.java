package net.mineclick.game.model.achievement;

import lombok.Data;
import net.mineclick.game.type.StatisticType;

@Data
public class AchievementProgress {
    private long progress; // make sure to call AchievementsService#getProgress() to get the progress
    private StatisticType statisticType; // can be null. Either this or the progress above will be used
    private int awardedLevel;
    private boolean checked;
}
