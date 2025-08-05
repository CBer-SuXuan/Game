package net.mineclick.game.model;

import lombok.Data;

@Data
public class TutorialData {
    private boolean complete;
    private boolean vaultsComplete;

    private transient boolean showScoreboard;
    private transient boolean showUpgradesMenu;
    private transient boolean showUpgradesMenuWorkers;
    private transient boolean showUpgradesMenuIslands;
}
