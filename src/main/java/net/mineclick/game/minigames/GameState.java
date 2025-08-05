package net.mineclick.game.minigames;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.util.Runner;
import org.bukkit.event.Listener;

import java.util.function.Consumer;

public interface GameState extends Consumer<Runner.State>, Listener {
    MiniGameService service();

    @Override
    default void accept(Runner.State state) {
        if (this.equals(service().getCurrentState())) {
            tick(state.getTicks());
        } else {
            state.cancel();
        }
    }

    void tick(long ticks);

    void updateInventory(GamePlayer player);
}
