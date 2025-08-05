package net.mineclick.game.model.leaderboard;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.game.model.Board;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.Statistic;
import net.mineclick.game.service.StatisticsService;
import net.mineclick.game.type.LeaderboardType;
import net.minecraft.core.Direction;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;

@Getter
public class Leaderboard extends Board {
    private final static DecimalFormat format = new DecimalFormat("#,###");
    private final static Color GOLD = new Color(218, 165, 32);
    private final static Color GRAY = new Color(192, 192, 192);
    private final static Color GREEN = new Color(127, 204, 25);
    private final static Color BLUE = new Color(79, 188, 183);

    private final LeaderboardType type;
    @Setter
    private Map<String, Statistic> statistics; // player name -> statistic

    public Leaderboard(LeaderboardType type, Location loc, Direction face) {
        super(type.getSize().getWidth(), type.getSize().getHeight(), "leaderboards/" + type.getImage(), loc, face);
        this.type = type;

        getImage().drawString(type.getName(), BLUE, 470, type.getSize().getLineStart() - 50);
    }

    @Override
    public LeaderboardImage applyTo(GamePlayer player) {
        LeaderboardImage image = getImage().copy();
        if (statistics == null || statistics.isEmpty()) {
            return image;
        }

        Statistic personalStat = statistics.getOrDefault(player.getName(), StatisticsService.i().get(player.getUuid(), type.getStatisticType()));
        boolean replaceLast = personalStat.getRank() > type.getSize().getLines(); // Whether to replace the last line with personal stat
        for (Map.Entry<String, Statistic> entry : statistics.entrySet()) {
            String name = entry.getKey();
            Statistic statistic = entry.getValue();
            int line = (int) statistic.getRank();
            if (statistic.getRank() == type.getSize().getLines() && replaceLast) {
                name = player.getName();
                statistic = personalStat;
                line = type.getSize().getLines();
            }

            Color color = statistic == personalStat ? GREEN : statistic.getRank() < 3 ? GOLD : GRAY;
            drawLine(image, name, statistic, line, color);
        }

        return image;
    }

    private void drawLine(LeaderboardImage image, String name, Statistic statistic, int line, Color color) {
        String scoreStr = format.format(statistic.getScore());
        int commas = StringUtils.countMatches(scoreStr, ",");

        int rank = Math.min(999, (int) statistic.getRank());
        int rankPadding = 44 - String.valueOf(rank).length() * 10;
        int scorePadding = 554 - String.valueOf(((long) statistic.getScore())).length() * 10 - (commas * 4);

        int y = type.getSize().getLineStart() + (line) * 54;

        if (scorePadding < 464) {
            scorePadding = 464;
        }

        image.drawString(String.valueOf(rank), color, rankPadding, y);
        image.drawString(name, color, 90, y);
        image.drawString(scoreStr, color, scorePadding, y);
    }
}
