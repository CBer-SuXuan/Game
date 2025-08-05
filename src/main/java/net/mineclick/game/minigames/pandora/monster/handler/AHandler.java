package net.mineclick.game.minigames.pandora.monster.handler;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.ai.Robot;
import org.bukkit.entity.Monster;

@RequiredArgsConstructor
public abstract class AHandler {
    private final Robot monster;

    abstract void tick(Monster monster);
}
