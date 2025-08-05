package net.mineclick.game.model;

import net.mineclick.game.model.leaderboard.LeaderboardImage;
import net.mineclick.game.service.LobbyService;
import net.minecraft.core.Direction;
import org.bukkit.Location;

public class Sign extends Board {
    public Sign(int width, int height, String image, Location location, Direction face) {
        super(width, height, "signs/" + image, location, face);
    }

    public void update() {
        LobbyService.i().getPlayersInLobby(getLocation()).forEach(this::update);
    }

    @Override
    public LeaderboardImage applyTo(GamePlayer player) {
        return null;
    }
}
