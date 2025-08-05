package net.mineclick.game.type.worker;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.model.worker.EntityWorker;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.global.config.field.IslandUnlockRequired;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;

import java.lang.reflect.InvocationTargetException;

public enum WorkerType {
    //Enum ordinal corresponds to the index in workers menu
    ZOMBIE(ZombieWorker.class),
    SKELETON(SkeletonWorker.class),
    SPIDER(SpiderWorker.class),
    CREEPER(CreeperWorker.class),
    PIGMAN(PigmanWorker.class),
    SLIME(SlimeWorker.class),
    SNOWMAN(SnowmanWorker.class),
    ENDERMAN(EndermanWorker.class),
    BLAZE(BlazeWorker.class),
    GOLEM(GolemWorker.class);

    @Getter
    private final Class<? extends EntityWorker> workerClass;

    WorkerType(Class<? extends EntityWorker> workerClass) {
        this.workerClass = workerClass;
    }

    public static WorkerType ofMobType(IslandUnlockRequired.MobType mobType) {
        for (WorkerType type : values()) {
            if (type.name().equals(mobType.name()))
                return type;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityWorker> T spawn(IslandModel island, Worker worker) {
        try {
            return ((T) workerClass.getDeclaredConstructor(Level.class).newInstance(((CraftWorld) Game.i().getWorld()).getHandle()).spawn(island, worker));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }
}
