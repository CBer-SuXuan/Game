package net.mineclick.game.util.visual;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Runner;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DroppedItem {
    public static void spawn(Material material, Location location, int lifeTicks, Set<GamePlayer> playerSet) {
        ItemStack item = ItemBuilder.builder().material(material).title(UUID.randomUUID().toString()).build().toItem();
        spawn(item, location, lifeTicks, playerSet, null);
    }

    public static void spawn(Material material, Location location, int lifeTicks, Set<GamePlayer> playerSet, Function<GamePlayer, Boolean> onPickup) {
        ItemStack item = ItemBuilder.builder().material(material).title(UUID.randomUUID().toString()).build().toItem();
        spawn(item, location, lifeTicks, playerSet, (location1, player) -> onPickup.apply(player));
    }

    public static void spawn(Material material, Location location, int lifeTicks, Set<GamePlayer> playerSet, BiFunction<Location, GamePlayer, Boolean> onPickup) {
        ItemStack item = ItemBuilder.builder().material(material).title(UUID.randomUUID().toString()).build().toItem();
        spawn(item, location, lifeTicks, playerSet, onPickup);
    }

    public static void spawn(ItemStack item, Location location, int lifeTicks, Set<GamePlayer> playerSet, BiFunction<Location, GamePlayer, Boolean> onPickup) {
        spawn(item, location, lifeTicks, playerSet, onPickup, null);
    }

    public static void spawn(ItemStack item, Location location, int lifeTicks, Set<GamePlayer> playerSet, BiFunction<Location, GamePlayer, Boolean> onPickup, Consumer<ItemEntity> onTick) {
        ItemEntity entityItem = new ItemEntity(((CraftWorld) location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), CraftItemStack.asNMSCopy(item)) {
            @Override
            public void remove(RemovalReason removalReason) {
                super.remove(removalReason);

                if (playerSet != null) {
                    for (GamePlayer player : playerSet) {
                        player.getAllowedEntities().remove(getId());
                    }
                } else {
                    EntityPolice.getGloballyExcluded().remove(getId());
                }
            }

            @Override
            public void tick() {
                super.tick();

                if (onTick != null) {
                    onTick.accept(this);
                }
            }

            @Override
            public void playerTouch(Player entityhuman) {
                if (onPickup == null || pickupDelay > 0) return;
                if (playerSet != null && playerSet.stream().noneMatch(p -> p.getUuid().equals(entityhuman.getUUID()))) {
                    return;
                }

                PlayersService.i().<GamePlayer>get(entityhuman.getUUID(), player -> {
                    if (onPickup.apply(getBukkitEntity().getLocation(), player)) {
                        discard();
                    }
                });
            }
        };
        entityItem.pickupDelay = 20;

        if (playerSet != null) {
            for (GamePlayer player : playerSet) {
                player.getAllowedEntities().add(entityItem.getId());
            }
        } else {
            EntityPolice.getGloballyExcluded().add(entityItem.getId());
        }
        Runner.sync(() -> entityItem.level().addFreshEntity(entityItem, CreatureSpawnEvent.SpawnReason.CUSTOM));
        Runner.sync(lifeTicks, entityItem::kill);
    }
}
