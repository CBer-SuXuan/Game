package net.mineclick.game.messenger;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.core.messenger.Message;
import net.mineclick.core.messenger.MessageName;
import net.mineclick.game.model.leaderboard.LeaderboardData;

import java.util.Set;

@Getter
@Setter
@MessageName("leaderboards")
public class LeaderboardsHandler extends Message {
    private Set<LeaderboardData> leaderboards;
}
