package net.mineclick.game.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class LobbyData {
    private String currentGadget;
    private Set<String> unlockedGadgets = new HashSet<>();
    private Set<String> collectedEasterEggs = new HashSet<>();
}
