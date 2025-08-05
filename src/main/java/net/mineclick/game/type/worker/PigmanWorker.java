package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class PigmanWorker extends EntityWorker {
    public PigmanWorker(Level world) {
        super(EntityType.ZOMBIFIED_PIGLIN, world);
    }

    @Override
    public void loadGoals() {

    }
}
