package net.mineclick.game.gadget.generic;

import com.google.common.collect.EvictingQueue;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.LobbyService;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.List;
import java.util.*;

public class Flash extends Gadget {
    private static final Random r = Game.getRandom();
    private static final Color BLUE = new Color(25, 118, 210);
    private static final Color ORANGE = new Color(230, 74, 25);

    private static final Map<GamePlayer, Vector> dirMap = new HashMap<>();

    @Override
    public void run(GamePlayer player, Action action) {
        Player p = player.getPlayer();
        Color color = p.isSneaking() ? BLUE : ORANGE;
        List<Lightning> lightnings = new ArrayList<>();

        dirMap.put(player, p.getLocation().getDirection().setY(0).normalize().multiply(-4));
        Runner.sync(0, 1, state -> {
            if (state.getTicks() >= 400 || (state.getTicks() % 40 == 0 && !LobbyService.i().isInLobby(player))) {
                state.cancel();
                return;
            }

            lightnings.removeIf(l -> l.length > 40);
            if (r.nextInt(10) == 0 || lightnings.size() < 5) {
                if (lightnings.size() >= 5)
                    lightnings.remove(0);

                lightnings.add(new Lightning(player, p, color));
            }

            for (Lightning lightning : lightnings) {
                lightning.tick();
            }

            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 3), true);
        });
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().material(Material.DIAMOND_BOOTS);
    }

    @Override
    public String getImmutableName() {
        return "flash";
    }

    @Override
    public String getName() {
        return "Flash";
    }

    @Override
    public String getDescription() {
        return "Leave lightning behind you\nas you run around";
    }

    @Override
    public int getCooldown() {
        return 20;
    }

    @EventHandler
    public void on(PlayerMoveEvent e) {
        PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
            if (dirMap.containsKey(player)) {
                if (e.getTo().getX() != e.getFrom().getX() || e.getTo().getZ() != e.getFrom().getZ()) {
                    dirMap.put(player, e.getFrom().toVector().subtract(e.getTo().toVector()).setY(0).normalize().multiply(4));
                }
            }
        });
    }

    private class Lightning {
        private final EvictingQueue<Location> queue = EvictingQueue.create(8);
        private final Color color;
        private final Player p;
        private final Location location;
        private Vector offset;
        private int length = 0;

        Lightning(GamePlayer player, Player p, Color color) {
            this.p = p;
            this.color = color;

            offset();

            location = p.getLocation().add(offset).add(dirMap.get(player));
        }

        private void offset() {
            offset = new Vector(r.nextDouble() - 0.5, r.nextDouble() * 2, r.nextDouble() - 0.5);
        }

        private void tick() {
            if (r.nextInt(5) == 0) {
                offset();
            }
            Vector dir = p.getLocation().add(offset).toVector()
                    .subtract(location.toVector());
            if (dir.lengthSquared() <= 0.1) {
                length += 999;
                return;
            }
            dir.normalize().multiply(0.2);

            for (int i = 0; i < 3; i++) {
                queue.add(location.add(dir).clone());
            }
            Set<GamePlayer> players = getPlayersInLobby(location);
            queue.forEach(l -> ParticlesUtil.sendColor(l, color, players));

            length++;
        }
    }
}
