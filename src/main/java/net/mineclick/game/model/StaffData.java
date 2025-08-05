package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.game.type.StaffTool;
import net.mineclick.game.type.StaffToolTrigger;

import java.util.*;

@Data
public class StaffData {
    private Map<StaffToolTrigger, StaffTool> staffTools = new HashMap<>();
    private String customToolCommand;
    private Set<UUID> visited = new HashSet<>();
    private UUID lastVisited;
    private boolean hideTool;
}
