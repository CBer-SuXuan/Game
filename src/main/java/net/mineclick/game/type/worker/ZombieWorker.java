package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ZombieWorker extends EntityWorker {
    public ZombieWorker(Level world) {
        super(EntityType.ZOMBIE, world);
    }

    @Override
    public void loadGoals() {
    }
}
