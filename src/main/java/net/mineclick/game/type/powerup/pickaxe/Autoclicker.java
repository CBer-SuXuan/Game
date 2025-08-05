package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.pickaxe.PickaxeConfiguration;
import net.mineclick.game.service.PickaxeService;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.visual.SwingAnimation;
import net.mineclick.global.util.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Set;

public class Autoclicker extends Powerup {
    private final Material pickMaterial;
    private int picksCount;

    public Autoclicker(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);

        int level = getLevel();
        ArrayList<PickaxeConfiguration> list = new ArrayList<>(PickaxeService.i().getConfigurations().values());
        if (level > list.size()) {
            pickMaterial = list.get(list.size() - 1).getMaterial();
        } else {
            pickMaterial = list.get(level - 1).getMaterial();
        }
    }

    @Override
    public double getPeriod() {
        return 5;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 3 == 0 && picksCount <= 8) {
            picksCount++;
            Pair<Block, BlockFace> pair = getRandomBlockRelative();
            if (pair == null) return;

            Set<GamePlayer> players = getPlayers();
            Location location = pair.key().getLocation();
            SwingAnimation.builder()
                    .item(new ItemStack(pickMaterial))
                    .degreeStep(30)
                    .swingsToLive(3)
                    .face(pair.value().getOppositeFace())
                    .location(location)
                    .onSwing(count -> {
                        if (count == 3) {
                            picksCount--;
                        }

                        getPlayer().playSound(Sound.BLOCK_STONE_HIT, location, 1, 1);
                        dropItem(pair.key().getLocation(), 1);
                    })
                    .build()
                    .spawn(players);
        }
    }
}
