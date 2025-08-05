package net.mineclick.game.type.powerup.perks;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.global.util.Formatter;

@RequiredArgsConstructor
public class WorkerPerk extends PowerupPerk {
    private final WorkerType workerType;
    private final double perkPerLevel;

    public double getPerk(GamePlayer player) {
        Worker worker = player.getWorkers().get(workerType);
        if (worker == null || worker.getLevel() < 50) return 0;

        return (int) (worker.getLevel() / 50) * perkPerLevel;
    }

    @Override
    public String getDescription(GamePlayer player) {
        double perk = getPerk(player);
        return "+" + Formatter.format(perkPerLevel * 100) + "% power for every 50 levels\nof " + workerType.name().toLowerCase() + " (" + Formatter.format(perk * 100) + "%)";
    }
}
