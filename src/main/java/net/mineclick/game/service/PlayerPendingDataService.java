package net.mineclick.game.service;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.PlayerPendingData;
import net.mineclick.global.util.SingletonInit;

@SingletonInit
public class PlayerPendingDataService {
    private static PlayerPendingDataService i;

    private PlayerPendingDataService() {
    }

    public static PlayerPendingDataService i() {
        return i == null ? i = new PlayerPendingDataService() : i;
    }

    /**
     * Process this players pending data if any (votes, purchases, etc)
     *
     * @param player The player
     */
    public void process(GamePlayer player) {
        PlayerPendingData pendingData = player.getPendingData();
        if (pendingData.getVotes() > 0) {
            player.processVotes(pendingData.getVotes());
            pendingData.setVotes(0);
        }
    }
}
