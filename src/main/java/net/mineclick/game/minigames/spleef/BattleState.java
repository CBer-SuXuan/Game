package net.mineclick.game.minigames.spleef;

import net.mineclick.game.Game;
import net.mineclick.game.gadget.christmas.MagicWand;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.MessageType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BattleState implements SpleefGameState {
    private final Set<GamePlayer> players;
    private final int numPlayers;
    private final List<Block> blocks;
    private int killAllCountdown = 300;
    private boolean gameWon;
    private int endTicks = 100;

    BattleState(Set<GamePlayer> players) {
        this.players = players;
        numPlayers = players.size();
        blocks = new ArrayList<>(service().getArenaBlocks());

        players.forEach(p -> {
            service().setupPlayer(p, true);
            p.getPlayer().getInventory().setHeldItemSlot(0);
        });
    }

    @Override
    public void tick(long ticks) {
        players.removeIf(GamePlayer::isOffline);

        if (players.isEmpty()) {
            service().nextState(new WaitingState());
            return;
        }

        if (players.size() == 1) {
            if (!gameWon) {
                players.forEach(p -> {
                    service().rewardPlayer(p, numPlayers, true);

                    //A bit hacky but whatever
                    // TODO dude not whatever, this is garbage
                    new MagicWand().explode(Color.RED, true, p.getPlayer().getLocation());
                });
                gameWon = true;
            }

            if (endTicks-- == 0) {
                players.forEach(p -> service().playerDie(p));
                service().nextState(new WaitingState());
            }
            return;
        }

        if (ticks == 1200) { //1 min
            players.forEach(p -> p.sendMessage("Starting to remove random blocks...", MessageType.INFO));
        }
        if (!blocks.isEmpty()) {
            if (((ticks >= 1200) && ((ticks % 20) == 0)) || ((ticks >= 3000) && ((ticks % 2) == 0))) {
                breakBlock(blocks.get(Game.getRandom().nextInt(blocks.size())));
            }
        } else {
            if (killAllCountdown-- <= 0) {
                service().nextState(new WaitingState());
            }
        }
    }

    @Override
    public void updateInventory(GamePlayer player) {
        service().setupPlayer(player, true);
    }

    private void breakBlock(Block block) {
        blocks.remove(block);
        block.setType(Material.AIR);
        players.forEach(p -> p.playSound(Sound.BLOCK_SNOW_BREAK, block.getLocation(), 1, 1));
    }

    @EventHandler
    public void on(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.PHYSICAL)) {
            if (e.hasBlock() && e.getAction().equals(Action.LEFT_CLICK_BLOCK) && e.getPlayer().getInventory().getHeldItemSlot() == 0) {
                if (blocks.contains(e.getClickedBlock())) {
                    breakBlock(e.getClickedBlock());
                    e.setUseInteractedBlock(Event.Result.ALLOW);
                }
            } else if (e.getPlayer().getInventory().getHeldItemSlot() == 8) {
                PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
                    if (players.contains(player)) {
                        players.remove(player);
                        player.sendMessage("You left spleef", MessageType.ERROR);
                        service().playerLeave(player);
                    }
                });
            }
        }
    }

    @EventHandler
    public void on(PlayerMoveEvent e) {
        PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
            if (!gameWon && players.contains(player)) {
                int y = e.getTo().getBlockY();
                if (y < service().getMinArenaY()) {
                    players.remove(player);
                    service().playerDie(player);
                    service().rewardPlayer(player, numPlayers, false);
                    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
                }
                if ((y > service().getPickaxeStart().getY() && !service().isInArena(e.getTo()))) {
                    players.remove(player);
                    service().playerLeave(player);
                    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
                }
            }
        });
    }

    @EventHandler
    public void on(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Snowball snowball)) return;
        if (!(snowball.getShooter() instanceof Player)) return;

        EntityPolice.getGloballyExcluded().add(snowball.getEntityId());
    }

    @EventHandler
    public void on(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball snowball)) return;
        if (!(snowball.getShooter() instanceof Player player)) return;

        var hitBlock = e.getHitBlock();
        if (hitBlock == null) return;

        PlayersService.i().<GamePlayer>get(player.getUniqueId(), playerModel -> {
            if (!players.contains(playerModel)) return;
            if (!SpleefService.i().isInArena(hitBlock.getLocation())) return;

            for (var breakableBlock : SpleefService.BREAKABLE_BLOCK_IDS) {
                if (hitBlock.getType() == Material.getMaterial(breakableBlock)) {
                    breakBlock(hitBlock);
                    break;
                }
            }
        });

        EntityPolice.getGloballyExcluded().remove(snowball.getEntityId());
    }
}
