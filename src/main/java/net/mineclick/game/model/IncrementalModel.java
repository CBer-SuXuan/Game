package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.global.util.BigNumber;

@Data
public abstract class IncrementalModel {
    private String id;
    private long level = 1;
    private double multiplier = 1;
    private BigNumber income = BigNumber.ZERO;

    transient private GamePlayer player;
    transient private int justUpgradedTicks = 0;

    public void increaseLevel(long toAdd) {
        level += toAdd;
        recalculate();
    }

    /**
     * Recalculate certain model values
     */
    public void recalculate() {
        income = new BigNumber(getConfiguration().getBaseIncome().multiply(new BigNumber(level * getTotalMultiplier())));
    }

    /**
     * Get the total income multiplier taking into account
     * the dynamic multiplier, island gold multiplier
     * and player's personal multiplier
     *
     * @return The total multiplier
     */
    public double getTotalMultiplier() {
        double islandMultiplier = player.getIslands().isEmpty()
                ? 1
                : player.getIslands().values().parallelStream()
                .mapToDouble(i -> i.getConfig().getMultiplier())
                .sum();

        return multiplier * player.getMultiplier().getMultiplier() * islandMultiplier * player.getAchievementGoldMultiplier();
    }

    /**
     * Get the maximum number of items that
     * can be bought with the gold provided
     *
     * @param limit The buy limit (or <= 0 if no limit)
     * @return The number of items that can be bought
     */
    public long maxCanBuy(int limit) {
        double costBase = getConfiguration().getCostMultiplier();
        BigNumber gold = player.getGold();
        BigNumber costOfOne = cost(1);
        if (costOfOne.greaterThan(player.getGold())) {
            return 0;
        }

        double max = Math.log(1 + gold.multiply(new BigNumber(costBase - 1)).divide(costOfOne).doubleValue()) / Math.log(costBase);
        return (long) (limit <= 0 ? Math.min(BigNumber.POW_MAX, max) : Math.min(limit, max));
    }

    /**
     * Get the cost to buy the specified number of items
     *
     * @param toBuy The number of items to buy
     * @return The cost
     */
    public BigNumber cost(long toBuy) {
        return cost(toBuy, level);
    }

    /**
     * Get the cost to buy the specified number of items
     *
     * @param toBuy The number of items to buy
     * @param level From what level to calculate the cost
     * @return The cost
     */
    public BigNumber cost(long toBuy, long level) {
        if (toBuy > BigNumber.POW_MAX) {
            toBuy = BigNumber.POW_MAX;
        }

        double m = getConfiguration().getCostMultiplier();

        BigNumber r = new BigNumber(m).pow((int) level)
                .multiply(new BigNumber(m).pow((int) toBuy).subtract(BigNumber.ONE))
                .divide(new BigNumber(m).subtract(BigNumber.ONE));
        return new BigNumber(getConfiguration().getBaseCost().multiply(r));
    }

    public BigNumber getSelfWorth(BigNumber currentIncome) {
        BigNumber sum = currentIncome.add(income);
        BigNumber cost = cost(1);
        BigNumber costPerRate = cost.divide(currentIncome);
        return new BigNumber(costPerRate.add(cost.divide(sum)));
    }

    public void tick() {
        if (justUpgradedTicks > 0) {
            justUpgradedTicks--;
        }
    }

    public abstract IncrementalModelConfiguration getConfiguration();
}
