package net.mineclick.game.util.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import net.mineclick.game.Game;
import org.bukkit.Sound;

import java.util.Set;

public class SoundPolice extends PacketAdapter {
    private final Set<Sound> EXCLUDED_SOUNDS = ImmutableSet.of(
            Sound.ENTITY_PLAYER_ATTACK_CRIT,
            Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
            Sound.ENTITY_PLAYER_ATTACK_NODAMAGE,
            Sound.ENTITY_PLAYER_ATTACK_STRONG,
            Sound.ENTITY_PLAYER_ATTACK_SWEEP,
            Sound.ENTITY_PLAYER_ATTACK_WEAK,
            Sound.ENTITY_ARMOR_STAND_PLACE,
            Sound.ITEM_ARMOR_EQUIP_GENERIC
    );

    public SoundPolice() {
        super(Game.i(), ListenerPriority.HIGHEST, PacketType.Play.Server.NAMED_SOUND_EFFECT);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (EXCLUDED_SOUNDS.contains(event.getPacket().getSoundEffects().read(0))) {
            event.setCancelled(true);
        }
    }
}
