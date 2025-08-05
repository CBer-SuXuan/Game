package net.mineclick.game;

import com.comphenix.protocol.ProtocolLibrary;
import lombok.Getter;
import net.mineclick.core.messenger.Messenger;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.packet.BlockPolice;
import net.mineclick.game.util.packet.ChunkPolice;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.game.util.packet.SoundPolice;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.SingletonInitializer;
import net.mineclick.global.util.location.LocationParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

@Getter
public class Game extends JavaPlugin {
    @Getter
    private static final Random random = new Random();
    private static Game i;
    private Location spawn;

    public static Game i() {
        return i;
    }

    @Override
    public void onEnable() {
        i = this;

        Messenger.getI().addClassLoaded(this.getClassLoader());
        SingletonInitializer.loadAll(this.getClassLoader());

        PlayersService.i().setPlayerClass(GamePlayer.class);

        saveResource("config.yml", true);
        reloadConfig();
        spawn = LocationParser.parse(getConfig().getString("spawn"));

        ProtocolLibrary.getProtocolManager().addPacketListener(new EntityPolice());
        ProtocolLibrary.getProtocolManager().addPacketListener(new BlockPolice());
        ProtocolLibrary.getProtocolManager().addPacketListener(new SoundPolice());
        ProtocolLibrary.getProtocolManager().addPacketListener(new ChunkPolice());
    }

    public World getWorld() {
        return Bukkit.getWorlds().get(0);
    }
}
