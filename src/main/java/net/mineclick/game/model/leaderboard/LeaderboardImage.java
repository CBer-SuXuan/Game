package net.mineclick.game.model.leaderboard;

import lombok.Getter;
import net.mineclick.game.model.Board;
import org.bukkit.map.MapPalette;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class LeaderboardImage {
    private final byte[] buffer;
    @Getter
    private final int width;
    @Getter
    private final int height;

    @SuppressWarnings("deprecation")
    public LeaderboardImage(Image image) {
        this(MapPalette.imageToBytes(image), image.getWidth(null), image.getHeight(null));
    }

    private LeaderboardImage(byte[] buffer, int width, int height) {
        this.buffer = buffer;
        this.width = width;
        this.height = height;
    }

    public LeaderboardImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, color.getRGB());
            }
        }

        this.buffer = MapPalette.imageToBytes(image);
        this.width = width;
        this.height = height;
    }

    public LeaderboardImage copy() {
        return new LeaderboardImage(Arrays.copyOf(buffer, buffer.length), width, height);
    }

    public void drawString(String string, Color color, int x, int y) {
        drawString(string, Board.FONT_SIZE, color, x, y);
    }

    public void drawString(String string, float fontSize, Color color, int x, int y) {
        byte c = MapPalette.matchColor(color);

        BufferedImage image = new BufferedImage(width, height, 2);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.setFont(Board.MC_FONT.deriveFont(fontSize));
        g.drawString(string, x, y);

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        int black = Color.BLACK.getRGB();
        for (int i = 0; i < pixels.length; ++i) {
            if (pixels[i] == black) {
                buffer[i] = c;
            }
        }

        g.dispose();
    }

    public void drawImage(byte[] image, int oX, int oY, int width, int length) {
        int index = 0;
        for (int y = oY; y < oY + length; y++) {
            for (int x = oX; x < oX + width; x++) {
                int loc = y * this.width + x;
                if (loc >= buffer.length)
                    return;

                byte color = image[index++];
                if (color != 0) {
                    buffer[loc] = color;
                }
            }
        }
    }

    public byte[] getPanel(int oX, int oY) {
        byte[] result = new byte[128 * 128];

        int index = 0;
        for (int y = oY; y < oY + 128; y++) {
            for (int x = oX; x < oX + 128; x++) {
                int loc = y * width + x;
                if (loc >= buffer.length)
                    return result;

                result[index++] = buffer[loc];
            }
        }

        return result;
    }
}
