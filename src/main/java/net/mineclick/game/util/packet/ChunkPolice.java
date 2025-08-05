package net.mineclick.game.util.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.LobbyService;
import net.mineclick.game.service.MineshaftService;
import net.mineclick.global.config.IslandConfig;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.Runner;
import org.bukkit.Location;

public class ChunkPolice extends PacketAdapter {
    public ChunkPolice() {
        super(Game.i(), ListenerPriority.HIGHEST, PacketType.Play.Server.MAP_CHUNK);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.isPlayerTemporary())
            return;

        PlayersService.i().<GamePlayer>get(event.getPlayer().getUniqueId(), player -> {
            if (player == null || player.isOffline() || player.getRank().isAtLeast(Rank.STAFF)) {
                return;
            }

            Location pLoc = event.getPlayer().getLocation();
            Location loc = Game.i().getSpawn();
            if (LobbyService.i().isInLobby(player)) {
                loc = LobbyService.i().getSpawn();
            } else if (MineshaftService.i().isInMineshaft(player)) {
                loc = MineshaftService.i().getSpawn();
            } else {
                IslandConfig config = player.getCurrentIsland(true).getConfig();
                if (config.getSpawn() != null) {
                    loc = config.getSpawn().toLocation();
                }
            }

            int minX = loc.getBlockX() - 500;
            int maxX = loc.getBlockX() + 500;
            int minZ = loc.getBlockZ() - 500;
            int maxZ = loc.getBlockZ() + 500;

            if (pLoc.getBlockX() <= minX || pLoc.getBlockX() >= maxX || pLoc.getBlockZ() <= minZ || pLoc.getBlockZ() >= maxZ) {
                event.setCancelled(true);

                Location finalLoc = loc;
                Runner.sync(() -> event.getPlayer().teleport(finalLoc));
            }
        });
    }
}
