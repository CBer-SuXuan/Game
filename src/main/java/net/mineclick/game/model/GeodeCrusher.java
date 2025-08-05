package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.game.service.GeodesService;
import net.mineclick.game.service.HologramsService;
import net.mineclick.game.service.LobbyService;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.geode.GeodeAnimation;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.game.util.visual.PacketHologram;
import net.mineclick.global.service.ServersService;
import net.mineclick.global.util.Runner;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@Data
public class GeodeCrusher {
    private final Location blockLocation;
    private final List<PacketHologram> holograms = new ArrayList<>();

    public GeodeCrusher(Location blockLocation) {
        this.blockLocation = blockLocation;

        // top hologram
        Function<GamePlayer, String> topText = p -> {
            if (p.isOpeningGeode()) {
                return null;
            }

            int count = GeodesService.i().getTotal(p);
            if (count > 0) {
                return ChatColor.YELLOW + "You have " + ChatColor.GREEN + count + ChatColor.YELLOW + " geode" + (count != 1 ? "s" : "") + " to open!";
            } else {
                return ChatColor.GRAY + "You don't have any geodes";
            }
        };
        holograms.add(HologramsService.i().spawn(blockLocation.clone().add(0.5, 1.5, 0.5), topText, true));

        // bottom hologram
        Function<GamePlayer, String> bottomText = p -> {
            if (p.isOpeningGeode()) {
                return null;
            }

            int count = GeodesService.i().getTotal(p);
            if (count > 0) {
                return ChatColor.GRAY + "Click to open a geode";
            } else {
                return ChatColor.GRAY + "Get geodes from ascending or at " + ChatColor.DARK_AQUA + "store.mineclick.net";
            }
        };
        holograms.add(HologramsService.i().spawn(blockLocation.clone().add(0.5, 1, 0.5), bottomText, false));
    }

    public void checkPlayersStanding(Set<GamePlayer> players) {
        Vector velocity = new Vector(-1.5, 0.5, 0);
        players.stream().filter(p -> {
            if (p.isOffline()) return false;

            Block block = p.getPlayer().getLocation().getBlock();
            return block.getX() == blockLocation.getBlockX() && block.getZ() == blockLocation.getBlockZ()
                    && block.getY() >= blockLocation.getBlockY() - 2 && block.getY() <= blockLocation.getBlockY() + 2;
        }).forEach(p -> {
            Player player = p.getPlayer();
            if (player.isFlying()) {
                player.setFlying(false);
            }
            player.setVelocity(velocity);
            p.playSound(Sound.ENTITY_PLAYER_HURT);
            p.playSound(Sound.UI_STONECUTTER_TAKE_RESULT, blockLocation, 0.5, 1);
        });
    }


    public void spawnOpenedHolograms(GamePlayer player, List<GeodeItem> items) {
        AtomicReference<String> hologramText = new AtomicReference<>("");

        // floating item
        Location itemLocation = blockLocation.clone().add(0.5, 1.1, 0.5);
        ArmorStandUtil itemStandBuilder;
        itemStandBuilder = ArmorStandUtil.builder()
                .viewers(Collections.singleton(player))
                .location(itemLocation)
                .small(true)
                .tickConsumer(new Consumer<>() {
                    boolean initialised;
                    int index = 0;

                    @Override
                    public void accept(ArmorStand stand) {
                        if (stand.tickCount % 40 == 0 || !initialised) {
                            initialised = true;
                            if (index >= items.size()) {
                                index = 0;
                            }

                            GeodeItem item = items.get(index);

                            hologramText.set(ChatColor.YELLOW + item.getName() + " " + item.getRarity().getName());

                            ItemStack itemStack = item.getItem() == null ? new ItemStack(Material.CHEST) : item.getItem().build().toItem();
                            stand.setItemSlot(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack));

                            index++;
                        }

                        int degree = stand.tickCount % 360;
                        double y = Math.sin(degree / 180D * Math.PI * 6) / 4;

                        stand.moveTo(itemLocation.getX(), itemLocation.getY() + y, itemLocation.getZ());
                        stand.setHeadPose(new Rotations(0, stand.headPose.getY() + 5, 0));
                    }
                })
                .build();
        itemStandBuilder.spawn();
        Runner.sync(items.size() > 1 ? 300 : 140, itemStandBuilder::removeAll);

        // bottom hologram
        HologramsService.i().spawn(blockLocation.clone().add(0.5, 1, 0.5), p -> hologramText.get(), Collections.singleton(player), false).setLifespan(items.size() > 1 ? 300 : 140);
    }

    /**
     * @param player The player that opened it
     * @return A set of players that can see a geode opening animation
     */
    public Set<GamePlayer> getPlayers(GamePlayer player) {
        Set<GamePlayer> players = LobbyService.i().getPlayersInLobby(getBlockLocation());
        players.removeIf(p -> p.isOffline() || p.isOpeningGeode() || p.getPlayer().getOpenInventory().getTopInventory().getType().equals(InventoryType.CHEST));
        players.add(player);

        return players;
    }

    public boolean open(GamePlayer player, Rarity rarity, boolean openAll) {
        if (ServersService.i().isShuttingDown()) return false;
        if (GeodesService.i().getCount(player, rarity) > 0) {
            player.setOpeningGeode(true);
            holograms.forEach(h -> h.refresh(player));

            GeodeAnimation animation = rarity.getAnimation().apply(this, player);
            animation.spawnGeodeStand(rarity);
            animation.setOnComplete(() -> {
                if (player.isOffline()) return;

                List<GeodeItem> geodeItems = GeodesService.i().openGeode(player, rarity, openAll);
                spawnOpenedHolograms(player, geodeItems);

                Runner.sync(geodeItems.size() > 1 ? 300 : 140, () -> {
                    player.setOpeningGeode(false);
                    holograms.forEach(h -> h.refresh(player));
                });
            });

            return true;
        }

        return false;
    }

    public void clear() {
        holograms.forEach(HologramsService.i()::remove);
    }
}
