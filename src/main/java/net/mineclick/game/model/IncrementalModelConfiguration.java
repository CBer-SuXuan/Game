package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.global.util.BigNumber;

@Data
public class IncrementalModelConfiguration {
    private String id;
    private String name;
    private BigNumber baseCost; // base (level 1) cost of the model
    private double costMultiplier; // defines how the cost is incremented (must be > 1.0)
    private BigNumber baseIncome; // base  (level 1) income of the model
}
