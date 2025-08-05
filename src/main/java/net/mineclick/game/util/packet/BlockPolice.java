package net.mineclick.game.util.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.service.PlayersService;

import java.util.HashSet;
import java.util.Set;

public class BlockPolice extends PacketAdapter {
    public static Set<BlockPosition> exclude = new HashSet<>();

    public BlockPolice() {
        super(Game.i(), ListenerPriority.HIGHEST, PacketType.Play.Server.BLOCK_CHANGE);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        BlockPosition blockPosition = event.getPacket().getBlockPositionModifier().getValues().get(0);
        PlayersService.i().<GamePlayer>get(event.getPlayer().getUniqueId(), playerModel -> {
            if (!playerModel.getAllowedBlockChanges().remove(blockPosition) && !exclude.contains(blockPosition)) {
                event.setCancelled(true);
            }
        });
    }
}
