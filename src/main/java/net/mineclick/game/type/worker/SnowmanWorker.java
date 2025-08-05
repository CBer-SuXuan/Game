package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SnowmanWorker extends EntityWorker {
    public SnowmanWorker(Level world) {
        super(EntityType.SNOW_GOLEM, world);

        setFollowingItemEnabled(true);
    }

    @Override
    public void loadGoals() {

    }
}
