package net.mineclick.game.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.IncrementalModel;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.service.PowerupService;
import net.mineclick.game.service.WorkersService;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.RandomCollection;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum TraderItemType {
    WORKERS("Random worker's levels", Material.ZOMBIE_HEAD, 0.6, percent -> 1 + (int) (percent * 20),
            (amount, player) -> {
                BigNumber cost;
                if (player.getWorkers().isEmpty()) {
                    cost = WorkersService.i().getConfigurations().get(WorkerType.ZOMBIE).getBaseCost().multiply(new BigNumber(amount));
                } else {
                    Worker highestWorker = player.getWorkers().values().stream()
                            .max(Comparator.comparing(IncrementalModel::getLevel)).orElse(null);
                    if (highestWorker != null) {
                        cost = highestWorker.cost(amount);
                    } else {
                        cost = new BigNumber("10000");
                    }
                }
                cost.multiply(new BigNumber(1.0 - Game.getRandom().nextDouble() * 0.25));

                double hours = amount / 20.0 * 10;
                BigNumber incomeCost = player.getTotalWorkersIncome().multiply(new BigNumber(60 * 60 * hours));
                if (incomeCost.greaterThan(BigNumber.ZERO) && incomeCost.smallerThan(cost)) {
                    cost = incomeCost;
                }

                return cost;
            },
            (amount, player) -> {
                if (player.getWorkers().keySet().isEmpty()) {
                    WorkersService.i().unlockWorker(player, WorkerType.ZOMBIE);
                }
                ArrayList<WorkerType> types = new ArrayList<>(player.getWorkers().keySet());

                WorkerType type = types.get(Game.getRandom().nextInt(types.size()));
                player.getWorkers().get(type).increaseLevel(amount);

                player.sendMessage(ChatColor.YELLOW + "Purchased " + amount + " level(s) of " + type.name().toLowerCase());
            }),
    PICKAXE("Pickaxe levels", Material.WOODEN_PICKAXE, 0.4, percent -> 1 + (int) (percent * 40),
            (amount, player) -> {
                double hours = amount / 40.0 * 10;
                BigNumber cost = player.getTotalWorkersIncome().multiply(new BigNumber(60 * 60 * hours));
                BigNumber costByPickaxe = player.getPickaxe().cost(amount).multiply(new BigNumber(1.0 - Game.getRandom().nextDouble() * 0.25));
                if (costByPickaxe.smallerThan(cost) || cost.equals(BigNumber.ZERO)) {
                    cost = costByPickaxe;
                }

                return cost;
            },
            (amount, player) -> {
                player.getPickaxe().increaseLevel(amount);
                player.sendMessage(ChatColor.YELLOW + "Purchased " + amount + " level(s) of pickaxe");
            }),
    COOKIES("Worker cookies", Material.COOKIE, 0.3, percent -> 5 + (int) (percent * 16),
            (amount, player) -> {
                double hours = amount / 20.0 * 4;
                return player.getTotalWorkersIncome().multiply(new BigNumber(60 * 60 * hours));
            },
            (amount, player) -> {
                player.addCookies(amount);
                player.updateCookiesItem();
                player.sendMessage(ChatColor.YELLOW + "Purchased " + amount + " cookie(s)");
            }),
    POWERUP_PARTS("Powerup parts", Material.ENCHANTING_TABLE, 0.1, percent -> 1 + (int) (percent * 3),
            (amount, player) -> {
                double hours = amount / 4.0 * 10;
                return player.getTotalWorkersIncome().multiply(new BigNumber(60 * 60 * hours));
            },
            (amount, player) -> {
                PowerupService.i().addParts(player, amount);
                player.sendMessage(ChatColor.YELLOW + "Purchased " + amount + " powerup part(s)");
            }),
    TREASURE_MAP("Treasure map", Material.FILLED_MAP, 0.05, percent -> 1,
            (amount, player) -> {
                double hours = Game.getRandom().nextDouble() * 3 + 2;
                return player.getTotalWorkersIncome().multiply(new BigNumber(60 * 60 * hours));
            },
            (amount, player) -> player.addTreasureMap()),
    ;

    private static RandomCollection<TraderItemType> list;

    private final String name;
    private final Material material;
    private final double chance;
    private final Function<Double, Integer> amount;
    private final BiFunction<Integer, GamePlayer, BigNumber> price;
    private final BiConsumer<Integer, GamePlayer> onPurchase;

    public static List<TraderItemType> getRandom(int count) {
        if (list == null) {
            list = new RandomCollection<>();
            for (TraderItemType value : values()) {
                list.add(value.chance, value);
            }
        }

        List<TraderItemType> collect = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            collect.add(list.next());
        }

        return collect;
    }
}
