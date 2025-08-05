package net.mineclick.game.type.geode;

import net.mineclick.game.Game;
import net.mineclick.game.gadget.christmas.MagicWand;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.RandomCollection;
import net.mineclick.global.util.Triple;
import net.mineclick.global.util.location.RandomVector;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VeryRare extends GeodeAnimation {
    private static final Color FLARE_COLOR = new Color(226, 172, 37);
    private final Set<Block> changedBlocks = new HashSet<>();
    private final List<MagicWand.Flare> flares = new ArrayList<>();

    public VeryRare(GeodeCrusher geodeCrusher, GamePlayer player) {
        super(geodeCrusher, player);

        placeBlocks();
    }

    @Override
    public int getPeriod() {
        return 100;
    }

    @Override
    public void tick(long ticks) {
        if (Game.getRandom().nextInt(5) == 0 && flares.size() < 7) {
            getPlayer().playSound(Sound.BLOCK_LAVA_POP, getGeodeCrusher().getBlockLocation(), 1, 1);
            flares.add(new MagicWand.Flare(getGeodeCrusher().getBlockLocation().clone().add(0.5, 0.5, 0.5), new RandomVector().setY(1).normalize().multiply(0.2), FLARE_COLOR));
        }
        flares.removeIf(flare -> {
            ParticlesUtil.sendColor(flare.tick(), flare.getColor(), getPlayers());
            return flare.isDone();
        });

        ParticlesUtil.send(ParticleTypes.WHITE_ASH, getGeodeCrusher().getBlockLocation().clone().add(0.5, 2, 0.5), Triple.of(3F, 3F, 3F), 0.1F, 5, getPlayers());

        if (ticks == 100) {
            Location location = getGeodeCrusher().getBlockLocation().clone().add(0.5, 1, 0.5);
            getPlayer().playSound(Sound.BLOCK_FIRE_EXTINGUISH, location, 1, 1);
            ParticlesUtil.send(ParticleTypes.LARGE_SMOKE, location, Triple.of(1F, 0.5F, 1F), 10, getPlayer());
        }
    }

    @Override
    public void onReset() {
        getPlayers().forEach(this::onPlayerRemove);
    }

    @Override
    protected void onPlayerRemove(GamePlayer player) {
        changedBlocks.forEach(player::sendBlockChange);
    }

    private void placeBlocks() {
        RandomCollection<Block> blocks = getBlocksAround(5);
        if (blocks.values().size() > 0) {
            for (int i = 0; i < blocks.values().size(); i++) {
                Block block = blocks.next();
                Location location = block.getLocation();
                BlockData blockData = Bukkit.createBlockData(block.getType().isSolid() ? Material.NETHERRACK : Material.LAVA);

                getPlayers().forEach(player -> player.sendBlockChange(location, blockData));
                changedBlocks.add(block);
            }
        }

        // place campfire
        getPlayers().forEach(player -> player.sendBlockChange(getGeodeCrusher().getBlockLocation(), Bukkit.createBlockData(Material.SOUL_CAMPFIRE)));
        changedBlocks.add(getGeodeCrusher().getBlockLocation().getBlock());
    }
}
