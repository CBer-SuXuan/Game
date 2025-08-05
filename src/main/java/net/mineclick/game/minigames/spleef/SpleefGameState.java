package net.mineclick.game.minigames.spleef;

import net.mineclick.game.minigames.GameState;

public interface SpleefGameState extends GameState {
    @Override
    default SpleefService service() {
        return SpleefService.i();
    }
}
