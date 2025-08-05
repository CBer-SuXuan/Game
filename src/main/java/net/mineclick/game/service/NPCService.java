package net.mineclick.game.service;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.visual.PacketNPC;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.SingletonInit;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SingletonInit
public class NPCService {
    private static NPCService i;

    private final List<PacketNPC> npcList = new ArrayList<>();


    private NPCService() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Game.i(), ListenerPriority.HIGH, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                int id = event.getPacket().getIntegers().getValues().get(0);

                WrappedEnumEntityUseAction action = event.getPacket().getEnumEntityUseActions().getValues().get(0);
                if (action.getAction() != EnumWrappers.EntityUseAction.INTERACT) {
                    return;
                }

                PacketNPC npc = npcList.stream()
                        .filter(n -> n.getId() == id)
                        .findFirst().orElse(null);
                if (npc != null && npc.getLocation().distanceSquared(event.getPlayer().getLocation()) < 25) {
                    PlayersService.i().get(event.getPlayer().getUniqueId(), (GamePlayer player) -> Runner.sync(() -> npc.handleClick(player)));
                }
            }
        });
    }

    public static NPCService i() {
        return i == null ? i = new NPCService() : i;
    }

    /**
     * Spawns a new global, non-moving NPC
     *
     * @param location           The location
     * @param villagerType       The villager type
     * @param villagerProfession The villager profession
     * @param visibility         Whether the player can see/interact with this villager or not
     * @return The newly created villager
     */
    public PacketNPC spawn(Location location, VillagerType villagerType, VillagerProfession villagerProfession, Function<GamePlayer, Boolean> visibility) {
        PacketNPC npc = new PacketNPC(location, villagerType, villagerProfession, visibility);
        npcList.add(npc);

        return npc;
    }

    public void tick(GamePlayer player) {
        npcList.forEach(npc -> npc.tick(player));
    }
}
