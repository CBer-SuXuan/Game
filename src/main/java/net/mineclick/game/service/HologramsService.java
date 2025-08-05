package net.mineclick.game.service;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.visual.PacketHologram;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@SingletonInit
public class HologramsService {
    private static HologramsService i;

    private final List<PacketHologram> hologramList = new CopyOnWriteArrayList<>();

    private HologramsService() {
        Runner.sync(1, 1, state -> hologramList.forEach(PacketHologram::floatTick));
    }

    public static HologramsService i() {
        return i == null ? i = new HologramsService() : i;
    }

    /**
     * Spawns a new global hologram
     *
     * @param location     The location
     * @param textFunction The text function
     * @param floating     Whether the hologram is floating
     * @param players      The players to spawn for (or null for all)
     * @return The newly created hologram
     */
    public PacketHologram spawn(Location location, Function<GamePlayer, String> textFunction, Set<GamePlayer> players, boolean floating) {
        PacketHologram hologram = new PacketHologram(location, player -> {
            if (players != null && !players.contains(player)) {
                return null;
            }

            return textFunction.apply(player);
        });
        hologram.setFloating(floating);

        hologramList.add(hologram);
        return hologram;
    }

    public PacketHologram spawn(Location location, Function<GamePlayer, String> textFunction, boolean floating) {
        return spawn(location, textFunction, null, floating);
    }

    /**
     * Spawns a new global hologram that floats up
     *
     * @param location     The location
     * @param textFunction The text function
     * @param lifespan     The lifespan of the hologram
     * @return The newly created hologram
     */
    public PacketHologram spawnFloatingUp(Location location, Function<GamePlayer, String> textFunction, Set<GamePlayer> players, int lifespan) {
        PacketHologram hologram = new PacketHologram(location, player -> {
            if (players != null && !players.contains(player)) {
                return null;
            }

            return textFunction.apply(player);
        }) {
            @Override
            public void floatTick() {
                locationY = getLocation().getY() + (floatY += 0.025);
            }
        };
        hologram.setFloating(true);
        hologram.setLifespan(lifespan);

        hologramList.add(hologram);
        return hologram;
    }

    public PacketHologram spawnFloatingUp(Location location, Function<GamePlayer, String> textFunction, Set<GamePlayer> players) {
        return spawnFloatingUp(location, textFunction, players, 100);
    }

    public PacketHologram spawnBlockBreak(Location location, BigNumber income, boolean bold, Set<GamePlayer> players) {
        return spawnFloatingUp(location, player -> ChatColor.GREEN + (bold ? ChatColor.BOLD.toString() : "") + "+" + income.print(player, false, true, bold), players, bold ? 100 : 40);
    }

    public void tick(GamePlayer player) {
        hologramList.stream()
                .filter(hologram -> hologram.tick(player))
                .forEach(this::remove);
    }

    public void remove(PacketHologram hologram) {
        PlayersService.i().<GamePlayer>getAll().forEach(hologram::despawnFor);
        hologramList.remove(hologram);
    }
}
