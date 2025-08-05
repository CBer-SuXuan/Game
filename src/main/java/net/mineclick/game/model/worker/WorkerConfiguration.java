package net.mineclick.game.model.worker;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.mineclick.game.model.IncrementalModelConfiguration;
import net.mineclick.game.service.BoostersService;
import net.mineclick.game.type.BoosterType;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.Material;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkerConfiguration extends IncrementalModelConfiguration {
    private String skin = Material.ZOMBIE_HEAD.name();

    public void setHeadItem(ItemUI itemUI) {
        Material material = Material.getMaterial(skin);
        if (material != null) {
            itemUI.setMaterial(material);
            return;
        }

        itemUI.setSkin(skin);
    }

    @Override
    public BigNumber getBaseCost() {
        return super.getBaseCost().multiply(new BigNumber(BoostersService.i().getActiveBoost(BoosterType.CHEAP_WORKER_BOOSTER)));
    }
}
