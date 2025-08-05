package net.mineclick.game.service;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.SuperBlockData;
import net.mineclick.game.model.pickaxe.Pickaxe;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.model.worker.WorkerConfiguration;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.ChatColor;

import static net.mineclick.global.config.field.UpgradeConfig.UpgradeType.*;

@SingletonInit
public class BuildingUpgradesService {
    private static BuildingUpgradesService i;

    private BuildingUpgradesService() {
        load();
    }

    public static BuildingUpgradesService i() {
        return i == null ? i = new BuildingUpgradesService() : i;
    }

    private void load() {
        WORKER_BPS.setConsumer((player, s) -> {
            String[] split = s.split(":");
            WorkerType type = WorkerType.valueOf(split[0]);
            double percent = Double.parseDouble(split[1]);

            Worker worker = ((GamePlayer) player).getWorkers().get(type);
            if (worker != null) {
                worker.setMultiplier(worker.getMultiplier() + percent);
                worker.recalculate();
                ((GamePlayer) player).getWorkers().values().forEach(Worker::recalculateIncomePercent);

                ((GamePlayer) player).recalculateGoldRate();
                player.getPlayer().sendMessage(ChatColor.GREEN + worker.getConfiguration().getName() + ChatColor.YELLOW + " is now " + ChatColor.GREEN + Formatter.format(worker.getMultiplier()) + "x " + ChatColor.YELLOW + "more productive");
            }
        }).setArgCheck(s -> {
            String[] split = s.split(":");
            if (split.length != 2)
                return false;

            try {
                WorkerType.valueOf(split[0]);
                Double.parseDouble(split[1]);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }).setPrerequisite((playerData, s) -> {
            WorkerType type = WorkerType.valueOf(s.split(":")[0]);
            return ((GamePlayer) playerData).getWorkers().containsKey(type) ? null : "Unlock this worker first";
        }).setDescriptor((player, s) -> {
            String[] split = s.split(":");
            WorkerType type = WorkerType.valueOf(split[0]);
            Worker worker = ((GamePlayer) player).getWorkers().get(type);
            double multiplier = worker != null ? worker.getMultiplier() : 1;
            multiplier += Double.parseDouble(split[1]);

            WorkerConfiguration config = WorkersService.i().getConfigurations().get(type);
            return "Make " + config.getName() + " " + Formatter.format(multiplier) + "x more productive";
        });

        PICKAXE_AMOUNT.setConsumer((player, s) -> {
            Pickaxe pickaxe = ((GamePlayer) player).getPickaxe();
            if (pickaxe != null) {
                pickaxe.setAmount(pickaxe.getAmount() + 1);
                pickaxe.updateItem();
                player.sendMessage("You now have " + pickaxe.getAmount() + " pickaxes", MessageType.INFO);
            }
        }).setDescriptor((p, s) -> "Get one more Pickaxe");

        GOLD.setConsumer((player, s) -> {
            GamePlayer gamePlayer = (GamePlayer) player;
            BigNumber amount = new BigNumber(s);
            if (SkillsService.i().has(gamePlayer, SkillType.PARKOUR_1)) {
                amount = amount.multiply(new BigNumber("2"));
            }
            gamePlayer.addGold(amount);

            player.sendMessage("Received " + amount.print(player) + ChatColor.GOLD + " gold", MessageType.INFO);
        }).setArgCheck(s -> {
            try {
                new BigNumber(s);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        SUPER_BLOCK.setConsumer((player, s) -> {
            double percent = Double.parseDouble(s);
            SuperBlockData superBlockData = ((GamePlayer) player).getSuperBlockData();
            superBlockData.setChance(superBlockData.getChance() + percent);

            player.sendMessage(ChatColor.GREEN + "+" + Formatter.format(percent * 100) + "%" + ChatColor.YELLOW + " chance of mining a Super Block");
        }).setArgCheck(s -> {
            try {
                Double.parseDouble(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }).setDescriptor((p, s) -> "Increase the chance of a Super Block by " + Formatter.format(Double.parseDouble(s) * 100) + "%");
    }
}
