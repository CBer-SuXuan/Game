package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class BlazeWorker extends EntityWorker {
    public BlazeWorker(Level world) {
        super(EntityType.BLAZE, world);

        setFollowingItemEnabled(true);
    }

    @Override
    public void loadGoals() {

    }
}
