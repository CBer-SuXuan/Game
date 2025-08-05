package net.mineclick.game.type.worker;

import net.mineclick.game.model.worker.EntityWorker;
import net.mineclick.game.model.worker.goal.MineGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;

import javax.annotation.Nullable;
import java.util.Optional;

public class EndermanWorker extends EntityWorker {
    private static final EntityDataAccessor<Optional<BlockState>> DATA_CARRY_STATE = SynchedEntityData.defineId(EndermanWorker.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);
    private static final EntityDataAccessor<Boolean> DATA_CREEPY = SynchedEntityData.defineId(EndermanWorker.class, EntityDataSerializers.BOOLEAN);

    public EndermanWorker(Level world) {
        super(EntityType.ENDERMAN, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_CARRY_STATE, Optional.empty());
        this.entityData.define(DATA_CREEPY, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbttagcompound) {
        super.addAdditionalSaveData(nbttagcompound);
        this.entityData.get(DATA_CARRY_STATE).ifPresent(iblockdata -> nbttagcompound.put("carriedBlockState", NbtUtils.writeBlockState(iblockdata)));
    }

    public void setCarriedBlock(@Nullable BlockState iblockdata) {
        this.entityData.set(DATA_CARRY_STATE, Optional.ofNullable(iblockdata));
    }

    private void setCrazy(boolean crazy) {
        this.entityData.set(DATA_CREEPY, crazy);
    }

    @Override
    public void loadGoals() {
    }

    @Override
    public MineGoal createMineGoal() {
        return new MineGoal(this) {
            private int ticks = 0;

            @Override
            protected void walkingTowards(Location loc) {
                if (ticks++ == 100) {
                    ticks = 0;

                    getNavigation().stop();
                    Block block = loc.getBlock();
                    while (!block.getType().equals(Material.AIR) && block.getLocation().getBlockY() < 250) {
                        block = block.getRelative(BlockFace.UP);
                    }
                    loc = block.getLocation();
                    moveTo(loc.getX(), loc.getY(), loc.getZ());
                }
            }

            @Override
            protected void mine() {
                Material material = getMineRegion().getBlockMaterial();
                BlockState block = CraftMagicNumbers.getBlock(material).defaultBlockState();
                setCarriedBlock(block);
            }

            @Override
            protected double getWalkSpeed() {
                return getWorker().getWorker().isExcited() ? 1.5 : 1;
            }

            @Override
            public void reset() {
                super.reset();
                setCarriedBlock(null);
                ticks = 0;
            }
        };
    }

}
