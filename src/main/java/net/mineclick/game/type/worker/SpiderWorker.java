package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SpiderWorker extends EntityWorker {
    public SpiderWorker(Level world) {
        super(EntityType.SPIDER, world);

        setFollowingItemEnabled(true);
    }

    @Override
    public void loadGoals() {

    }
}
