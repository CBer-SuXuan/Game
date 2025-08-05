package net.mineclick.game.model.pickaxe;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.mineclick.game.model.IncrementalModelConfiguration;
import org.bukkit.Material;

@Data
@EqualsAndHashCode(callSuper = true)
public class PickaxeConfiguration extends IncrementalModelConfiguration {
    private Material material;
    private double speed;
    private boolean glow;
    private int minLevel;
}

