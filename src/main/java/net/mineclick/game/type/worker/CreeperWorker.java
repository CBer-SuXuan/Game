package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CreeperWorker extends EntityWorker {
    public CreeperWorker(Level world) {
        super(EntityType.CREEPER, world);

        setFollowingItemEnabled(true);
    }

    @Override
    public void loadGoals() {

    }
}
