package net.mineclick.game.type;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.StaffData;
import net.mineclick.global.model.PlayerId;
import net.mineclick.global.service.PlayerListService;
import net.mineclick.global.service.TpService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.MessageType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum StaffTool {
    TP_NEXT((owner) -> {
        List<PlayerId> online = PlayerListService.i().getOnlinePlayers().stream()
                .filter(p -> !p.getRank().isAtLeast(Rank.STAFF))
                .collect(Collectors.toList());
        online.remove(owner);
        if (online.isEmpty()) {
            owner.sendMessage("No one online but you :(", MessageType.ERROR);
            return;
        }

        StaffData staffData = owner.getStaffData();
        List<PlayerId> potentialPlayers = new ArrayList<>(online);
        potentialPlayers.removeIf(pl -> staffData.getVisited().contains(pl.getUuid()));
        if (potentialPlayers.isEmpty()) {
            staffData.getVisited().clear();
            potentialPlayers.addAll(online);
            owner.sendMessage("Visited all players. Starting a new loop...");
        }

        PlayerId playerId = potentialPlayers.get(0);
        staffData.getVisited().add(playerId.getUuid());
        staffData.setLastVisited(playerId.getUuid());

        if (TpService.i().tpToPlayer(owner, playerId)) {
            owner.sendMessage("Teleporting to " + playerId.getName() + "...");
        } else {
            owner.sendMessage("Can't teleport to " + playerId.getName() + ". Excluded them from the tp list", MessageType.ERROR);
        }
    }, "Teleport next", "Teleport to next player online"),
    TP_SAME((owner) -> {
        if (owner.getStaffData().getLastVisited() != null) {
            PlayerId player = PlayerListService.i().getOnlinePlayers().stream()
                    .filter(p -> p.getUuid().equals(owner.getStaffData().getLastVisited()))
                    .findFirst().orElse(null);
            if (player != null) {
                if (TpService.i().tpToPlayer(owner, player)) {
                    owner.sendMessage("Teleporting to " + player.getName() + "...");
                } else {
                    owner.sendMessage("Can't teleport to " + player.getName(), MessageType.ERROR);
                }
                return;
            }
        }

        owner.sendMessage("No previous player", MessageType.ERROR);
    }, "Teleport same", "Teleport to the same player"),
    TEST_CLICK((owner) -> {
        Player targetPl = getTarget(owner.getPlayer());
        if (targetPl == null) {
            owner.getPlayer().sendMessage(ChatColor.RED + "No players within 10 meters");
        } else {
            Location location = targetPl.getLocation();
            location.setPitch(-90);
            targetPl.teleport(location);
            owner.getPlayer().sendMessage(ChatColor.YELLOW + "Moved their head up, are they still clicking? If so make a report");
        }
    }, "Test clicking", "Move target player's head up"),
    JUMP((owner) -> {
        Block targetBlock = owner.getPlayer().getTargetBlock(null, 50);

        Vector v = owner.getPlayer().getLocation().toVector().subtract(targetBlock.getLocation().toVector()).normalize();
        owner.getPlayer().teleport(targetBlock.getLocation().add(0.5, 0.5, 0.5).add(v).setDirection(owner.getPlayer().getLocation().getDirection()));
        owner.playSound(Sound.ENTITY_ENDERMAN_TELEPORT);
    }, "Jump", "Jump to target block (50m)"),
    CUSTOM((owner) -> {
        String command = owner.getStaffData().getCustomToolCommand();
        if (command != null) {
            if (command.contains("{player}")) {
                Player player = getTarget(owner.getPlayer());
                if (player == null) {
                    owner.getPlayer().sendMessage(ChatColor.RED + "No players within 10 meters");
                } else {
                    command = command.replace("{player}", player.getName());
                }
            }

            if (command.contains("{location}")) {
                Block targetBlock = owner.getPlayer().getTargetBlock(null, 50);
                Location l = targetBlock.getLocation();
                command = command.replace("{location}", l.getBlockX() + " " + l.getBlockZ() + " " + l.getBlockZ());
            }

            owner.getPlayer().sendMessage(ChatColor.GRAY + "Executing: " + command);
            owner.getPlayer().performCommand(command);
        }
    }, "Custom command", "Set a custom command");

    public final Consumer<GamePlayer> execute;
    private final String name;
    private final String description;

    private static Player getTarget(Player owner) {
        Preconditions.checkNotNull(owner.getLocation().getWorld());

        Location loc = owner.getLocation();
        Vector dir = loc.getDirection().multiply(0.1);
        for (int i = 0; i < 100; i++) {
            loc.add(dir);
            Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, 0.25, 1.5, 0.25)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .filter(p -> !p.equals(owner))
                    .collect(Collectors.toSet());
            if (!nearby.isEmpty()) {
                return (Player) nearby.iterator().next();
            }
        }

        return null;
    }
}
