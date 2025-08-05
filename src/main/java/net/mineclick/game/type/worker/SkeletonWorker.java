package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SkeletonWorker extends EntityWorker {
    public SkeletonWorker(Level world) {
        super(EntityType.SKELETON, world);
    }

    @Override
    public void loadGoals() {
    }
}
