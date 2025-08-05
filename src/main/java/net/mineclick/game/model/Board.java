package net.mineclick.game.model;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.SneakyThrows;
import net.mineclick.game.Game;
import net.mineclick.game.model.leaderboard.LeaderboardImage;
import net.mineclick.game.util.nms.NMS;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FileUtils;
import org.bukkit.Location;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public abstract class Board {
    public static final float FONT_SIZE = 32;
    private static final AtomicInteger entityIdCounter = new AtomicInteger(1000000);
    public static Font MC_FONT;
    public static Font MC_FONT_BOLD;
    private final int widthPanels;
    private final int heightPanels;
    @Getter
    private final Location location;
    private final Direction face;
    private final int entityIdStart;
    @Getter
    private LeaderboardImage image;

    public Board(int widthPanels, int heightPanels, String background, Location location, Direction face) {
        this.widthPanels = widthPanels;
        this.heightPanels = heightPanels;
        this.location = location;
        this.face = face;
        this.entityIdStart = entityIdCounter.getAndAdd(widthPanels * heightPanels);

        if (MC_FONT == null || MC_FONT_BOLD == null) {
            try {
                MC_FONT = loadFont("mc_regular");
                MC_FONT_BOLD = loadFont("mc_bold");
            } catch (FontFormatException | IOException e) {
                Game.i().getLogger().log(Level.SEVERE, "Error loading mc fonts", e);
            }
        }

        try {
            InputStream stream = Game.class.getResourceAsStream("/images/" + background);
            image = new LeaderboardImage(ImageIO.read(stream));
        } catch (Exception e) {
            Game.i().getLogger().log(Level.SEVERE, "Error loading resource images", e);
        }
    }

    private Font loadFont(String fontName) throws IOException, FontFormatException {
        InputStream stream = Game.class.getResourceAsStream("/fonts/" + fontName + ".otf");
        File tempFile = File.createTempFile(fontName, null);
        tempFile.deleteOnExit();

        FileUtils.copyInputStreamToFile(stream, tempFile);

        Font font = Font.createFont(Font.TRUETYPE_FONT, tempFile);
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);

        return font;
    }

    @SneakyThrows
    public void update(GamePlayer player) {
        if (player.isOffline())
            return;

        LeaderboardImage image = applyTo(player);
        if (image == null)
            image = this.image;

        int id = entityIdStart;
        for (int x = 0; x < widthPanels; x++) {
            for (int y = 0; y < heightPanels; y++) {
                Location loc = location.clone().add(face.getStepX() * x, y, face.getStepZ() * x);

                // Allow packets to be sent
                player.getAllowedEntities().add(id);

                // spawn entity
                Direction frameFace = face.getClockWise();
                float pitch;
                float yaw;
                if (frameFace.getAxis().isHorizontal()) {
                    pitch = 0.0F;
                    yaw = (float) (frameFace.get2DDataValue() * 90);
                } else {
                    pitch = (float) (-90 * frameFace.getAxisDirection().getStep());
                    yaw = 0.0F;
                }
                ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(id, UUID.randomUUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), pitch, yaw, EntityType.ITEM_FRAME, frameFace.get3DDataValue(), new Vec3(0, 0, 0), 0);
                player.sendPacket(spawnPacket);

                // Update the metadata
                ItemStack itemStack = new ItemStack(Items.FILLED_MAP);
                itemStack.getOrCreateTag().putInt("map", id);

                List<WrappedDataValue> wrappedDataValueList = Lists.newArrayList();
                wrappedDataValueList.add(new WrappedDataValue(8, NMS.ITEM, itemStack));

                PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
                packet.getIntegers().write(0, id);
                packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);

                ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), packet);

                // Send the image
                byte[] panel = image.getPanel(x * 128, (heightPanels - y - 1) * 128);
                MapItemSavedData.MapPatch mapPatch = new MapItemSavedData.MapPatch(0, 0, 128, 128, panel);

                ClientboundMapItemDataPacket packetMap = new ClientboundMapItemDataPacket(id, (byte) 0, false, null, mapPatch);
                player.sendPacket(packetMap);

                id++;
            }
        }
    }

    public abstract LeaderboardImage applyTo(GamePlayer player);
}
