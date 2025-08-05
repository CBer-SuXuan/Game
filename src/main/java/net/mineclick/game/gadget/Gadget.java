package net.mineclick.game.gadget;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.LobbyService;
import net.mineclick.global.util.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import java.util.Set;

public abstract class Gadget implements Listener {
    public boolean canRun(GamePlayer player, Action action) {
        return true;
    }

    public abstract void run(GamePlayer player, Action action);

    public abstract ItemBuilder.ItemBuilderBuilder getBaseItem();

    public abstract String getImmutableName();

    public abstract String getName();

    public abstract String getDescription();

    public abstract int getCooldown();

    public boolean isSecret() {
        return false;
    }

    public boolean isPlayerUnavailable(GamePlayer player) {
        return player.getActivityData().isAfk() || !LobbyService.i().isInLobby(player);
    }

    public Set<GamePlayer> getPlayersInLobby() {
        return LobbyService.i().getPlayersInLobby();
    }

    public Set<GamePlayer> getPlayersInLobby(Location closeBy) {
        return LobbyService.i().getPlayersInLobby(closeBy);
    }
}
