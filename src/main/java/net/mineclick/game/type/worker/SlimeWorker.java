package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SlimeWorker extends EntityWorker {
    private static final EntityDataAccessor<Integer> ID_SIZE = SynchedEntityData.defineId(SlimeWorker.class, EntityDataSerializers.INT);

    public SlimeWorker(Level world) {
        super(EntityType.SLIME, world);

        this.entityData.set(ID_SIZE, 3);
        setFollowingItemEnabled(true);
    }

    @Override
    public void loadGoals() {

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ID_SIZE, 3);
    }
}
