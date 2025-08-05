package net.mineclick.game.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LeaderboardType {
    PERSONAL(null, "Personal.png", Size.LARGE, "STATISTICS"),
    EXP(StatisticType.EXP, "Leaderboard.png", Size.LARGE, "    EXP"),
    CLICKS(StatisticType.CLICKS, "TopPlayers.png", Size.SMALL, "  CLICKS"),
    ASCENDS(StatisticType.ASCENDS, "TopPlayers.png", Size.SMALL, " ASCENDS");

    private final StatisticType statisticType;
    private final String image;
    private final Size size;
    private final String name;

    @Getter
    @AllArgsConstructor
    public enum Size {
        LARGE(5, 6, 9, 312),
        SMALL(5, 5, 7, 294),
        TINY(4, 5, 7, 294);

        private final int width;
        private final int height;
        private final int lines;
        private final int lineStart;
    }
}
