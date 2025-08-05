package net.mineclick.game.type.powerup.orb;

import net.mineclick.game.Game;
import net.mineclick.game.gadget.christmas.Santa;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.ItemBuilder;
import org.bukkit.Sound;

public class Present extends OrbPowerup {
    public Present(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public boolean onShoot(Orb orb) {
        ItemBuilder.ItemBuilderBuilder builder = ItemBuilder.builder();
        builder.skull(Santa.PRESENT_SKINS.get(Game.getRandom().nextInt(Santa.PRESENT_SKINS.size())));
        getPlayer().playSound(Sound.ENTITY_CHICKEN_EGG, orb.getHeadLocation(), 0.2, 1);

        for (int i = 0; i < Game.getRandom().nextInt(3) + 1; i++) {
            DroppedItem.spawn(builder.title(Game.getRandom().nextInt(1000) + "").build().toItem(), orb.getHeadLocation(), 50, getPlayers(), null);
        }

        orb.discard();
        return true;
    }
}
