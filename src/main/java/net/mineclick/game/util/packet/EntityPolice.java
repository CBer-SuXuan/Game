package net.mineclick.game.util.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.service.PlayersService;

import java.util.HashSet;
import java.util.Set;

public class EntityPolice extends PacketAdapter {
    @Getter
    static final Set<Integer> globallyExcluded = new HashSet<>();

    public EntityPolice() {
        super(Game.i(), ListenerPriority.HIGHEST, PacketType.Play.Server.SPAWN_ENTITY);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        int id = event.getPacket().getIntegers().read(0);

        if (globallyExcluded.contains(id)) {
            return;
        }

//        if (event.getPacketType().equals(PacketType.Play.Server.SPAWN_ENTITY)) {
//            for (WrappedWatchableObject item : event.getPacket().getWatchableCollectionModifier().getValues().get(0)) {
//                System.out.println(item.toString());
//            }
//            System.out.println("===");
//        }

//        if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_METADATA)) {
//            for (WrappedDataValue item : event.getPacket().getDataValueCollectionModifier().getValues().get(0)) {
//                System.out.println(item.getIndex() + " " + item.getValue() + " " + item.getSerializer().getType());
//            }
//        }
//        System.out.println("===");

        PlayersService.i().<GamePlayer>get(event.getPlayer().getUniqueId(), playerModel -> {
            if (!playerModel.getAllowedEntities().contains(id)) {
                event.setCancelled(true);
            }
        }).ifNull(() -> event.setCancelled(true));
    }
}
