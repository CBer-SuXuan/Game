package net.mineclick.game.model.achievement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class AchievementNode {
    private final String id;
    private final List<Achievement> achievements = new ArrayList<>();

    public Achievement getByLevel(int level) {
        if (level <= 0) return achievements.get(0);
        if (level > achievements.size()) return achievements.get(achievements.size() - 1);

        return achievements.get(level - 1);
    }

    public Achievement getByProgress(double score, int lowestLevel) {
        if (lowestLevel >= achievements.size() || lowestLevel < 0) return null;

        Achievement nextLevel = achievements.get(lowestLevel);
        return score >= nextLevel.getScore() ? nextLevel : null;
    }
}

