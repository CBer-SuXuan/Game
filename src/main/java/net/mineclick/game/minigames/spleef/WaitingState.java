package net.mineclick.game.minigames.spleef;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.LobbyService;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.Runner;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

public class WaitingState implements SpleefGameState {
    private final static int MIN_PLAYERS = 2;
    private final Set<GamePlayer> waitingPlayers = new HashSet<>();
    private String status;
    private boolean notEnoughPlayers;
    private int countdown;

    public WaitingState() {
        for (Block block : service().getArenaBlocks()) {
            block.setType(Game.getRandom().nextBoolean() ? Material.LIGHT_BLUE_CONCRETE : Material.CYAN_CONCRETE, false);
        }
        for (Block block : service().getPickaxeBlocks()) {
            block.setType(Material.WHITE_CONCRETE, false);
        }
    }

    @Override
    public void tick(long ticks) {
        waitingPlayers.removeIf(GamePlayer::isOffline);

        if (waitingPlayers.size() < MIN_PLAYERS && !notEnoughPlayers) {
            notEnoughPlayers = true;

            service().openDome();
            status = ChatColor.YELLOW + "Waiting for players";
            sendStatusMsg();
        }

        PlayersService.i().<GamePlayer>forAll(player -> {
            if (LobbyService.i().isInLobby(player) && service().isInArena(player.getPlayer().getLocation()) && !player.getActivityData().isAfk()) {
                if (!waitingPlayers.contains(player)) {
                    waitingPlayers.add(player);

                    service().setupPlayer(player, false);
                    sendStatusMsg(player);
                }
            } else if (waitingPlayers.contains(player)) {
                waitingPlayers.remove(player);
                player.sendMessage("You left spleef", MessageType.ERROR);
                service().clearPlayer(player);
            }
        });

        if (waitingPlayers.size() >= MIN_PLAYERS) {
            if (notEnoughPlayers) {
                notEnoughPlayers = false;

                countdown = 6;
            }

            if (ticks % 20 == 0) {
                if (countdown == 0) {
                    status = ChatColor.GREEN + "Begin!";
                    sendStatusMsg();
                    sendSound(Sound.ENTITY_ENDER_DRAGON_GROWL);

                    //Kick hovering players
                    Set<GamePlayer> playersInLobby = LobbyService.i().getPlayersInLobby();
                    playersInLobby.removeAll(waitingPlayers);
                    for (GamePlayer player : playersInLobby) {
                        Location l = player.getPlayer().getLocation();
                        if (service().isAboveArena(l)) {
                            service().playerLeave(player);
                        }
                    }

                    //Start the game
                    service().nextState(new BattleState(waitingPlayers));
                } else if (countdown > 0) {
                    if (countdown <= 5) {
                        status = ChatColor.YELLOW + "Game will start in " + countdown + " second" + (countdown == 1 ? "" : "s");
                        sendStatusMsg();
                        sendSound(Sound.UI_BUTTON_CLICK);
                    }
                    if (countdown == 3) {
                        service().closeDome();
                    }
                }

                countdown--;
            }
        } else if (ticks % 200 == 0) {
            sendStatusMsg();
        }
    }

    @Override
    public void updateInventory(GamePlayer player) {
        service().setupPlayer(player, false);
    }

    private void sendSound(Sound sound) {
        waitingPlayers.forEach(p -> p.getPlayer().playSound(p.getPlayer().getLocation(), sound, 1, 1));
    }

    private void sendStatusMsg() {
        waitingPlayers.forEach(this::sendStatusMsg);
    }

    private void sendStatusMsg(GamePlayer player) {
        if (status != null) player.sendMessage(status);
    }

    @EventHandler
    public void on(PlayerInteractEvent e) {
        PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
            if (waitingPlayers.contains(player) && !e.getAction().equals(Action.PHYSICAL) && e.getPlayer().getInventory().getHeldItemSlot() == 8) {
                waitingPlayers.remove(player);
                Runner.sync(() -> {
                    player.sendMessage("You left spleef", MessageType.ERROR);
                    service().playerLeave(player);
                });
            }
        });
    }
}
