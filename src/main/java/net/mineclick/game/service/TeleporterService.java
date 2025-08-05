package net.mineclick.game.service;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SingletonInit
public class TeleporterService implements Listener {
    private static TeleporterService i;
    private final Set<Player> teleporting = new HashSet<>();

    private TeleporterService() {
        Bukkit.getPluginManager().registerEvents(this, Game.i());

        List<TeleporterLine> lines = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            lines.add(new TeleporterLine());
        }
        TeleporterCircle circle = new TeleporterCircle();
        Runner.sync(0, 1, state -> {
            lines.forEach(TeleporterLine::tick);
            circle.tick();
        });
    }

    public static TeleporterService i() {
        return i == null ? i = new TeleporterService() : i;
    }

    private Location getLobbyLocation() {
        return LobbyService.i().getTeleporter();
    }

    @EventHandler
    public void on(PlayerMoveEvent e) {
        if (teleporting.contains(e.getPlayer()))
            return;

        PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
            if (!player.getTutorial().isComplete())
                return;

            double dis = 999;
            if (LobbyService.i().isInLobby(player)) {
                if (getLobbyLocation() != null) {
                    dis = e.getPlayer().getLocation().distanceSquared(getLobbyLocation());
                }
            } else {
                dis = e.getPlayer().getLocation().distanceSquared(player.getCurrentIsland().getTeleporterLocation());
            }

            if (dis <= 1) {
                teleporting.add(e.getPlayer());

                e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 110, 1, true, false));
                e.getPlayer().setVelocity(new Vector(0, 1, 0));
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1);
                Runner.sync(5, () -> {
                    if (!player.isOffline()) {
                        if (LobbyService.i().isInLobby(player)) {
                            player.tpToIsland(player.getCurrentIsland(), true);
                        } else {
                            LobbyService.i().spawn(player);
                        }
                    }

                    Runner.sync(5, () -> teleporting.remove(e.getPlayer()));
                });
            }
        });
    }

    class TeleporterLine {
        private int length = 0;
        private int currentLength = 0;
        private double offsetX;
        private double offsetZ;

        public void tick() {
            if (currentLength++ >= length) {
                length = 10 + Game.getRandom().nextInt(20);
                currentLength = 0;
                offsetX = Game.getRandom().nextDouble() - 0.5;
                offsetZ = Game.getRandom().nextDouble() - 0.5;
            }

            PlayersService.i().<GamePlayer>forAll(p -> {
                if (p.isOffline()) return;

                Location loc = LobbyService.i().isInLobby(p) && getLobbyLocation() != null
                        ? getLobbyLocation()
                        : p.getCurrentIsland().getTeleporterLocation();

                ParticlesUtil.sendColor(loc.clone().add(offsetX, currentLength * 0.05F, offsetZ), new Color(1, 120, 255), p);
            });
        }
    }

    class TeleporterCircle {
        private double radius = 0.1;
        private double dir = 1;

        public void tick() {
            radius += 0.05 * dir;
            if (radius >= 1.25) {
                dir = -1;
            } else if (radius <= 0.25) {
                dir = 1;
            }

            PlayersService.i().<GamePlayer>forAll(p -> {
                Location loc = LobbyService.i().isInLobby(p) && getLobbyLocation() != null
                        ? getLobbyLocation()
                        : p.getCurrentIsland().getTeleporterLocation();

                double step = 1 / (Math.PI * radius);
                for (double theta = 0; theta < Math.PI * 2; theta += step) {
                    ParticlesUtil.sendColor(loc.clone().add(radius * Math.cos(theta), 0, radius * Math.sin(theta)), new Color(140, 224, 250), p);
                }
            });
        }
    }
}
