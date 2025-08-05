package net.mineclick.game.service;

import com.google.common.reflect.ClassPath;
import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SingletonInit
public class GadgetsService implements Listener {
    private static GadgetsService i;

    @Getter
    private final List<Gadget> gadgets = new ArrayList<>();
    private final Map<Pair<GamePlayer, Gadget>, AtomicInteger> cooldown = new HashMap<>();
    @Getter
    private final ItemStack menuItem = ItemBuilder.builder()
            .material(Material.ENDER_CHEST)
            .title(ChatColor.YELLOW + "Gadgets Menu " + ChatColor.GRAY + ChatColor.ITALIC + "right-click")
            .build().toItem();


    private GadgetsService() {
        try {
            ClassPath classPath = ClassPath.from(Game.class.getClassLoader());
            List<? extends Class<?>> list = classPath.getTopLevelClassesRecursive("net.mineclick.game")
                    .stream()
                    .map(ClassPath.ClassInfo::load)
                    .filter(c -> !c.equals(Gadget.class))
                    .filter(Gadget.class::isAssignableFrom)
                    .collect(Collectors.toList());
            for (Class<?> aClass : list) {
                Gadget gadget = (Gadget) aClass.getDeclaredConstructor().newInstance();
                Bukkit.getPluginManager().registerEvents(gadget, Game.i());
                gadgets.add(gadget);
            }
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }

        Bukkit.getPluginManager().registerEvents(this, Game.i());

        Runner.sync(20, 20, state -> cooldown.values().removeIf(i -> i.addAndGet(-1) <= 0));
    }

    public static GadgetsService i() {
        return i == null ? i = new GadgetsService() : i;
    }

    @EventHandler
    public void on(PlayerInteractEvent e) {
        if (e.getItem() != null) {
            PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
                Gadget gadget = getGadget(player);
                if (gadget != null && ItemBuilder.isSameTitle(e.getItem(), buildItem(gadget, false))) {
                    Pair<GamePlayer, Gadget> pair = Pair.of(player, gadget);
                    AtomicInteger timeLeft = cooldown.get(pair);
                    if (timeLeft != null && !player.getRank().isAtLeast(Rank.DEV)) {
                        player.sendMessage("You need to wait " + timeLeft.get() + " second" + (timeLeft.get() == 1 ? "" : "s"), MessageType.ERROR);
                    } else if (gadget.canRun(player, e.getAction())) {
                        gadget.run(player, e.getAction());
                        cooldown.put(pair, new AtomicInteger(gadget.getCooldown()));
                    }

                    e.setCancelled(true);
                }
            });
        }
    }

    public Gadget getGadget(GamePlayer player) {
        return gadgets.stream().filter(g ->
                g.getImmutableName().equals(player.getLobbyData().getCurrentGadget())).findFirst().orElse(null);
    }

    public void addSecretGadget(GamePlayer player, String gadgetId, boolean setCurrent) {
        player.getLobbyData().getUnlockedGadgets().add(gadgetId);
        if (setCurrent) {
            player.getLobbyData().setCurrentGadget(gadgetId);
            player.updateInventory();
        }
    }

    public ItemStack buildItem(Gadget gadget, boolean menu) {
        return gadget.getBaseItem().title(ChatColor.GREEN + gadget.getName() + (menu ? "" : ChatColor.GRAY + " click")).build().toItem();
    }
}
