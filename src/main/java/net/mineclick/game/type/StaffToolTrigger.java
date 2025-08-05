package net.mineclick.game.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum StaffToolTrigger {
    LEFT(e -> !e.getPlayer().isSneaking() && (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)), "Left click"),
    RIGHT(e -> !e.getPlayer().isSneaking() && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)), "Right click"),
    SHIFT_LEFT(e -> e.getPlayer().isSneaking() && (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)), "Shift + Left click"),
    SHIFT_RIGHT(e -> e.getPlayer().isSneaking() && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)), "Shift + Right click"),
    DROP(e -> false, "Item drop (Q key)");

    public final Function<PlayerInteractEvent, Boolean> isApplied;
    private final String name;
}
