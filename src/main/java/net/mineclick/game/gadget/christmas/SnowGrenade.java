package net.mineclick.game.gadget.christmas;

import com.comphenix.protocol.wrappers.BlockPosition;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.game.util.packet.BlockPolice;
import net.mineclick.global.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class SnowGrenade extends Gadget {
    private static final String SKIN = Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWRmZDc3MjRjNjlhMDI0ZGNmYzYwYjE2ZTAwMzM0YWI1NzM4ZjRhOTJiYWZiOGZiYzc2Y2YxNTMyMmVhMDI5MyJ9fX0=");

    @Override
    public void run(GamePlayer player, Action action) {
        player.playSound(Sound.ITEM_FIRECHARGE_USE);

        Player bukkitPlayer = player.getPlayer();
        Location location = bukkitPlayer.getEyeLocation()
                .add(bukkitPlayer.getLocation().getDirection().multiply(2))
                .add(0, -1.8, 0);
        ItemStack stack = getBaseItem().title(Game.getRandom().nextInt() + "").build().toItem();

        Vector vector = bukkitPlayer.getLocation().getDirection();
        vector.multiply(1.5);
        ArmorStandUtil builder = ArmorStandUtil.builder()
                .onPlayerCollision((stand, p) -> {
                    if (!p.equals(player)) {
                        stand.discard();
                    }
                })
                .onBlockCollision((stand, block) -> stand.discard())
                .head(stack)
                .global(true)
                .location(location)
                .hasGravity(true)
                .velocity(vector)
                .build();
        ArmorStand stand = builder.spawn();

        Runner.sync(0, 1, state -> {
            if (state.getTicks() > 60 || !stand.isAlive()) {
                Location loc = stand.getBukkitEntity().getLocation().add(0, 1.8, 0);
                Set<Location> blocks = new HashSet<>();
                for (int x = -3; x <= 3; x++) {
                    for (int y = -3; y <= 3; y++) {
                        for (int z = -3; z <= 3; z++) {
                            if (x * x + y * y + z * z <= 9) {
                                Location l = loc.clone().add(x, y, z);
                                Material type = l.getBlock().getType();
                                if (type.isSolid() && !type.equals(Material.BARRIER)) {
                                    BlockPosition blockPosition = new BlockPosition(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                                    if (!BlockPolice.exclude.contains(blockPosition)) {
                                        BlockPolice.exclude.add(blockPosition);
                                        blocks.add(l);
                                    }
                                }
                            }
                        }
                    }
                }

                for (Location block : blocks) {
                    BlockData blockData = Bukkit.createBlockData(Material.SNOW_BLOCK);
                    getPlayersInLobby().forEach(p -> p.getPlayer().sendBlockChange(block, blockData));
                }
                Runner.sync(200, () -> {
                    for (Location l : blocks) {
                        getPlayersInLobby().forEach(p -> p.getPlayer().sendBlockChange(l, l.getBlock().getBlockData()));

                        BlockPolice.exclude.remove(new BlockPosition(l.getBlockX(), l.getBlockY(), l.getBlockZ()));
                    }
                });

                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1);
                ParticlesUtil.send(ParticleTypes.ITEM_SNOWBALL, loc, Triple.of(1f, 1f, 1f), 25, getPlayersInLobby(loc));
                ParticlesUtil.send(ParticleTypes.CLOUD, loc, Triple.of(1f, 1f, 1f), 30, getPlayersInLobby(loc));

                stand.discard();
                state.cancel();
                return;
            }

            ParticlesUtil.send(ParticleTypes.CLOUD, stand.getBukkitEntity().getLocation().add(0, 1.8, 0), Triple.of(0.1f, 0.1f, 0.1f), 2, getPlayersInLobby());
        });
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().skull(SKIN);
    }

    @Override
    public String getImmutableName() {
        return "snowGrenade";
    }

    @Override
    public String getName() {
        return "Snow Grenade";
    }

    @Override
    public String getDescription() {
        return "Throw explosive snow grenade";
    }

    @Override
    public int getCooldown() {
        return 15;
    }
}
