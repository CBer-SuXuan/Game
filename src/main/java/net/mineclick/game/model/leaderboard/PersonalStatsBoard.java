package net.mineclick.game.model.leaderboard;

import net.mineclick.game.Game;
import net.mineclick.game.model.Board;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.LevelsService;
import net.mineclick.game.service.StatisticsService;
import net.mineclick.game.type.LeaderboardType;
import net.mineclick.game.type.StatisticType;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.Strings;
import net.minecraft.core.Direction;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.map.MapPalette;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.logging.Level;

public class PersonalStatsBoard extends Board {
    private final static DecimalFormat format = new DecimalFormat("#,###");
    private final static Color GOLD = new Color(218, 165, 32);
    private final static Color GRAY = new Color(192, 192, 192);
    private final static Color GREEN = new Color(127, 204, 25);
    private final static Color BLUE = new Color(79, 188, 183);

    private byte[] heart = null;

    public PersonalStatsBoard(LeaderboardType type, Location loc, Direction face) {
        super(type.getSize().getWidth(), type.getSize().getHeight(), "leaderboards/" + type.getImage(), loc, face);

        try {
            InputStream stream = Game.class.getResourceAsStream("/images/leaderboards/Heart.png");
            heart = MapPalette.imageToBytes(ImageIO.read(stream));
        } catch (Exception e) {
            Game.i().getLogger().log(Level.SEVERE, "Error loading resource heart image", e);
        }
    }

    @Override
    public LeaderboardImage applyTo(GamePlayer player) {
        LeaderboardImage image = getImage().copy();

        // head
        if (player.getBodyRender() != null) {
            image.drawImage(player.getBodyRender(), 60, 60, 80, 80);
        }

        // name
        int length = 0;
        for (char c : player.getName().toCharArray()) {
            length += Strings.DefaultFontInfo.getDefaultFontInfo(c).getLength() * 5;
        }
        image.drawString(player.getName(), 40f, GOLD, (640 - length) / 2, 190);

        // premium heart
        if (player.getRank().isAtLeast(Rank.PAID) && heart != null) {
            image.drawImage(heart, 500, 60, 80, 80);
        }

        // stats
        drawLine(image, "Level", LevelsService.i().getLevel(player.getExp()), 0);
        drawLine(image, "Schmepls", player.getSchmepls(), 1);
        drawLine(image, "Gold", printGold(player, player.getGold()), 2);
        drawLine(image, "Total gold made", printGold(player, player.getLifelongGold()), 3);
        drawLine(image, "Current dimension", player.getDimensionsData().getDimension().getName(), 4);
        drawLine(image, "Ascends", player.getDimensionsData().getAscensionsTotal(), 5);
        drawLine(image, "Clicks", StatisticsService.i().get(player.getUuid(), StatisticType.CLICKS).getScore(), 6);
        drawLine(image, "Server votes", StatisticsService.i().get(player.getUuid(), StatisticType.VOTES).getScore(), 7);
        drawLine(image, "Time played", Formatter.duration(player.getActivityData().getPlayTime() * 1000), 8);

        return image;
    }

    private String printGold(GamePlayer player, BigNumber gold) {
        String print = gold.print(player);
        String[] split = print.split("§6");
        if (split.length == 2) {
            return ChatColor.stripColor(split[0]) + "|" + split[1];
        } else {
            return ChatColor.stripColor(print);
        }
    }

    private void drawLine(LeaderboardImage image, String name, double value, int line) {
        drawLine(image, name, format.format(value), line);
    }

    private void drawLine(LeaderboardImage image, String name, String value, int line) {
        value = ChatColor.stripColor(value);
        String partValue = null;

        int y = 256 + (line) * 60;
        int scorePadding = 600;
        char[] charArray = value.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == '|') {
                partValue = value.substring(0, i);
                value = value.replace("|", "");
            } else {
                int length = Strings.DefaultFontInfo.getDefaultFontInfo(c).getLength();
                scorePadding -= length * 4;
            }
        }
        if (scorePadding < 410) {
            scorePadding = 410;
        }

        image.drawString(name, GRAY, 50, y);
        if (partValue != null) {
            image.drawString(value, GOLD, scorePadding, y);
            image.drawString(partValue, GREEN, scorePadding, y);
        } else {
            image.drawString(value, GREEN, scorePadding, y);
        }
    }
}
