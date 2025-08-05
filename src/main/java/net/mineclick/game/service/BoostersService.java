package net.mineclick.game.service;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.mineclick.core.messenger.Action;
import net.mineclick.game.Game;
import net.mineclick.game.messenger.BoostersHandler;
import net.mineclick.game.messenger.ThanksHandler;
import net.mineclick.game.model.ActiveBooster;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.BoosterType;
import net.mineclick.global.commands.Commands;
import net.mineclick.global.model.ChatSenderData;
import net.mineclick.global.service.ChatService;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

@SingletonInit
public class BoostersService {
    private static BoostersService i;

    private List<ActiveBooster> activeBoosters = new ArrayList<>();

    private BoostersService() {
        Runner.async(20, 300, state -> {
            BoostersHandler handler = new BoostersHandler();
            handler.setResponseConsumer(message -> {
                if (message != null) {
                    state.cancel();

                    update(((BoostersHandler) message).getBoosters());
                } else {
                    Game.i().getLogger().severe("Could not load Boosters");
                }
            });

            handler.send(Action.GET);
        });

        Commands.addCommand(Commands.Command.builder()
                .name("boosters")
                .description("List currently active boosters")
                .callFunction((data, strings) -> {
                    Player player = data.getPlayer();

                    player.sendMessage(Strings.line());
                    if (activeBoosters.isEmpty()) {
                        player.sendMessage(Strings.middle(ChatColor.GRAY + "There are no active Boosters"));
                    } else {
                        player.sendMessage(Strings.middle(ChatColor.YELLOW + "Active Boosters") + "\n ");
                        for (ActiveBooster booster : activeBoosters) {
                            BoosterType type = booster.getType();
                            int minLeft = (int) ((booster.getExpiresAt().getTime() - System.currentTimeMillis()) / 60000);
                            TextComponent text = new TextComponent(ChatColor.GOLD + "★ " + ChatColor.YELLOW + type.getName() + " " + ChatColor.GRAY + minLeft + " min left" + ChatColor.DARK_AQUA + " [hover]");

                            StringBuilder hover = new StringBuilder(ChatColor.YELLOW + type.getDescription() + "\n \n" + ChatColor.YELLOW + "Contributors:");
                            for (String pl : booster.getPlayers()) {
                                hover.append("\n")
                                        .append(ChatColor.GOLD)
                                        .append("★ ")
                                        .append(ChatColor.AQUA)
                                        .append(pl);
                            }
                            hover.append("\n \n").append(ChatColor.GREEN).append("Click to send thanks");

                            text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover.toString()).create()));
                            text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/thanks"));
                            player.spigot().sendMessage(text);
                        }
                    }

                    player.sendMessage(" ");
                    Strings.sendStore(player, Strings.middle(ChatColor.GRAY + "Purchase Boosters at " + ChatColor.DARK_AQUA + "store.mineclick.net"));
                    player.sendMessage(Strings.line());
                    return null;
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("thanks")
                .description("Thank all players that activated boosters")
                .callFunction((data, strings) -> {
                    GamePlayer player = ((GamePlayer) data);

                    long currentTime = System.currentTimeMillis();
                    long diff = currentTime - player.getLastThankedAt();
                    if (diff < 60 * 60 * 1000) {
                        player.sendMessage("You can only thank players once an hour (" + Formatter.duration(60 * 60 * 1000 - diff) + ")", MessageType.ERROR);
                        return null;
                    }


                    Set<String> players = new HashSet<>();
                    for (ActiveBooster activeBooster : getActiveBoosters()) {
                        players.addAll(activeBooster.getPlayers());
                    }
                    players.remove(player.getName());
                    if (players.isEmpty()) {
                        player.sendMessage("There is no one to thank at the moment", MessageType.ERROR);
                        return null;
                    }

                    player.setLastThankedAt(currentTime);
                    player.sendMessage(ChatColor.GREEN + "Your thank you message was sent to all");
                    player.sendMessage(ChatColor.GREEN + "players that activated boosters.");
                    player.sendMessage(ChatColor.GREEN + "You get" + ChatColor.AQUA + " +25 EXP " + ChatColor.GREEN + "for your generosity");
                    player.addExp(25);

                    ChatSenderData chatSenderData = ChatSenderData.from(player);
                    handleThanks(chatSenderData);
                    ThanksHandler handler = new ThanksHandler();
                    handler.setFrom(chatSenderData);
                    handler.send(Action.POST);

                    return null;
                })
                .build());
    }

    public static BoostersService i() {
        return i == null ? i = new BoostersService() : i;
    }

    /**
     * @return currently active boosters
     */
    public List<ActiveBooster> getActiveBoosters() {
        Date now = new Date();
        activeBoosters.removeIf(b -> b.getExpiresAt().before(now));
        return activeBoosters;
    }

    /**
     * Update boosters
     *
     * @param activeBoosters The boosters
     */
    public void update(List<ActiveBooster> activeBoosters) {
        if (activeBoosters != null) {
            this.activeBoosters = activeBoosters;
        }
    }

    /**
     * Check if the booster (by type) is active
     *
     * @param type The booster type
     * @return True if the booster of this type if active
     */
    public boolean isActive(BoosterType type) {
        return activeBoosters.stream()
                .filter(b -> b.getType().equals(type))
                .anyMatch(b -> b.getExpiresAt().after(new Date()));
    }

    /**
     * Get the booster value if the type of the booster provided
     * is current active. Otherwise return 1
     *
     * @param type The booster type
     * @return The boost value if active, or 1
     */
    public double getActiveBoost(BoosterType type) {
        return isActive(type) ? type.getBoost() : 1;
    }

    /**
     * Activate a booster
     *
     * @param player The player who activated
     * @param type   The booster type
     */
    public boolean activateBooster(GamePlayer player, BoosterType type) {
        int available = player.getBoosters().getOrDefault(type, 0);
        if (available > 0) {
            if (available == 1) {
                player.getBoosters().remove(type);
            } else {
                player.getBoosters().put(type, available - 1);
            }

            ActiveBooster booster = new ActiveBooster();
            booster.setType(type);
            booster.setPlayers(Collections.singletonList(player.getName()));
            booster.setDuration(type.getDurationMin());

            BoostersHandler handler = new BoostersHandler();
            handler.setBoosters(Collections.singletonList(booster));
            handler.send(Action.POST);

            ChatService.i().sendBroadcast(
                    ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " activated the " + type.getName(),
                    type.getDescription(),
                    true
            );
            return true;
        }

        return false;
    }

    /**
     * Add boosters to player's collection
     *
     * @param player The player
     * @param type   Booster type
     * @param amount Amount to add
     */
    public void addBooster(GamePlayer player, BoosterType type, int amount) {
        player.getBoosters().merge(type, amount, Integer::sum);
    }

    /**
     * Handle the thanks command
     *
     * @param from The player's name that ran the thanks command
     */
    public void handleThanks(ChatSenderData from) {
        Set<String> players = new HashSet<>();
        for (ActiveBooster activeBooster : getActiveBoosters()) {
            players.addAll(activeBooster.getPlayers());
        }
        players.remove(from.getName());

        TextComponent textComponent = Strings.playerHoverInfo(from);
        textComponent.addExtra(ChatColor.GREEN + " thanked you for activating a booster " + ChatColor.AQUA + "+5 EXP");

        PlayersService.i().<GamePlayer>getAll().stream()
                .filter(p -> players.contains(p.getName()))
                .forEach(p -> {
                    p.addExp(5);
                    p.sendMessage(textComponent);
                    p.levelUpSound();
                });
    }
}
