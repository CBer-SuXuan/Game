package net.mineclick.game.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DynamicMineBlockType {
    MINESHAFT(true, true),
    LOBBY_CRIMSON(false, true),
    FROZEN_ELYTRA(false, false),
    BOOKSHELF(false, false),
    ;

    private final boolean saved; //TODO not used
    private final boolean powerupAllowed;
}
