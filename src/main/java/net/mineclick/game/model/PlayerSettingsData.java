package net.mineclick.game.model;

import lombok.Data;

import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class PlayerSettingsData {
    private AtomicBoolean hideBoosters = new AtomicBoolean(false);
    private AtomicBoolean noIslandVisits = new AtomicBoolean(false);
    private AtomicBoolean flight = new AtomicBoolean(false);
    private AtomicBoolean noAutoCheckpoints = new AtomicBoolean(false);

    // discord
    private AtomicBoolean discordBooster = new AtomicBoolean(false);
    private AtomicBoolean discordVaults = new AtomicBoolean(false);
    private AtomicBoolean discordFriends = new AtomicBoolean(false);
    private AtomicBoolean discordReward = new AtomicBoolean(false);
    private AtomicBoolean discordAfk = new AtomicBoolean(false);
}
