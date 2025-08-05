package net.mineclick.game.type.quest.villager;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.NPCService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.type.quest.Quest;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.game.util.visual.PacketNPC;
import net.mineclick.global.util.Pair;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class VillagerQuest extends Quest {
    @Getter
    private final Map<GamePlayer, Long> playersCooldown = new HashMap<>();

    public abstract String getVillagerName();

    public abstract Pair<VillagerType, VillagerProfession> getVillagerType();

    public abstract Location getVillagerLocation();

    public abstract boolean isVisible(GamePlayer player);

    public abstract List<QuestObjective> getObjectives();

    public void onVillagerClickAfterComplete(GamePlayer player) {
    }

    public void sendVillagerMessage(GamePlayer player, String message) {
        sendVillagerMessage(player, 0, message);
    }

    public void sendVillagerMessage(GamePlayer player, int delay, String message) {
        String msg = ChatColor.DARK_GREEN + getVillagerName() + ": " + ChatColor.WHITE + message;
        if (delay > 0) {
            player.schedule(delay, () -> player.sendMessage(msg));
        } else {
            player.sendMessage(msg);
        }
    }

    public void spawnVillager() {
        PacketNPC npc = NPCService.i().spawn(getVillagerLocation(), getVillagerType().key(), getVillagerType().value(), this::isVisible);
        npc.addHologram(p -> (isNewQuest(p) || isNewObjective(p) ? ChatColor.GOLD.toString() + ChatColor.BOLD + "[!] " : "") + ChatColor.RESET + (isComplete(p) ? ChatColor.GRAY : ChatColor.GREEN) + getVillagerName(), false);
        npc.setHasParticles(p -> isNewQuest(p) || isNewObjective(p));
        npc.setClickConsumer(p -> {
            p.playSound(Sound.ENTITY_VILLAGER_AMBIENT, 0.5, Game.getRandom().nextDouble() + 0.5);
            QuestsService.i().handleVillagerClick(p, this);
        });
    }

    public void cooldown(GamePlayer player, int ticks) {
        playersCooldown.put(player, System.currentTimeMillis() + ticks * 50L);
    }

    public boolean isOnCooldown(GamePlayer player) {
        playersCooldown.values().removeIf(time -> time < System.currentTimeMillis());

        return playersCooldown.containsKey(player);
    }
}
