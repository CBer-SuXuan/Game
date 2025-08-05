package net.mineclick.game.model.worker.goal;

import lombok.Getter;
import net.mineclick.game.model.worker.EntityWorker;

@Getter
public abstract class WorkerGoal {
    private final EntityWorker worker;
    private int goalTicks = 0;

    public WorkerGoal(EntityWorker worker) {
        this.worker = worker;
    }

    public abstract void start();

    public final void tick() {
        tick(goalTicks++);
    }

    protected abstract void tick(int ticks);

    public abstract boolean hasEnded();

    public void reset() {
        goalTicks = 0;
    }
}
