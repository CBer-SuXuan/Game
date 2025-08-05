package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GolemWorker extends EntityWorker {
    public GolemWorker(Level world) {
        super(EntityType.IRON_GOLEM, world);

        setFollowingItemEnabled(true);
    }

    @Override
    public void loadGoals() {

    }
}
