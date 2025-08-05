package net.mineclick.game.menu;

import net.mineclick.core.util.BigNumber;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class UpgradesMenuTest {
    @Test
    public void maxCanBuy() throws Exception {
        BigNumber gold = new BigNumber("100");
        BigNumber cost = new BigNumber("10");
        double rate = 1.05;
        int limit = -1;

        for (int i = 0; i < 10; i++) {
            System.out.println(maxCanBuy(gold, cost, rate, i, limit));
        }
    }

    private long maxCanBuy(BigNumber gold, BigNumber baseCost, double growthRate, long owned, int limit) {
        BigDecimal c = gold.divide(baseCost, RoundingMode.HALF_EVEN);
        double d = (growthRate - 1) / Math.pow(growthRate, owned);
        BigDecimal toLog = c.multiply(new BigDecimal(d));

        double max = Math.floor(Math.log(toLog.doubleValue() + 1) / Math.log(growthRate));
        return (long) (limit < 0 ? max : Math.min(limit, max));
    }

    @Test
    public void cost() throws Exception {
        BigNumber baseCost = new BigNumber("10");
        double growthRate = 1.05;
        long owned = 0;

        for (int n = 1; n < 10; n++) {
            double r = Math.pow(growthRate, owned) * (Math.pow(growthRate, n) - 1) / (growthRate - 1);
            System.out.println(baseCost.multiply(new BigNumber(String.valueOf(r))));
        }
    }
}