package net.mineclick.game.type.geode;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import org.bukkit.Material;
import org.bukkit.Sound;

public class Common extends GeodeAnimation {
    public Common(GeodeCrusher geodeCrusher, GamePlayer player) {
        super(geodeCrusher, player);
    }

    @Override
    public int getPeriod() {
        return 20;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 10 == 0) {
            if (ticks == 20) {
                getPlayer().playSound(Sound.UI_STONECUTTER_TAKE_RESULT, getGeodeCrusher().getBlockLocation(), 1, 1);
            } else {
                getPlayer().playSound(Sound.BLOCK_STONE_BREAK, getGeodeCrusher().getBlockLocation(), 1, 1);
            }
            ParticlesUtil.sendBlock(getGeodeCrusher().getBlockLocation().clone().add(0.5, 1.2, 0.5), Material.STONE, Triple.of(0.25F, 0.25F, 0.25F), 1, ticks == 20 ? 40 : 10, getPlayers());
        }
    }
}
