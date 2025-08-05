package net.mineclick.game.menu;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.Statistic;
import net.mineclick.game.service.IslandsService;
import net.mineclick.game.service.LevelsService;
import net.mineclick.game.service.StatisticsService;
import net.mineclick.game.type.BoosterType;
import net.mineclick.game.type.StatisticType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.model.OffenceData;
import net.mineclick.global.service.FriendsService;
import net.mineclick.global.service.OffencesService;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.service.TpService;
import net.mineclick.global.type.PunishmentType;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.*;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlayerInfoMenu extends InventoryUI {
//    private static final String YOUTUBE_SKIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJmNmMwN2EzMjZkZWY5ODRlNzJmNzcyZWQ2NDU0NDlmNWVjOTZjNmNhMjU2NDk5YjVkMmI4NGE4ZGNlIn19fQ==";
//    private static final String TWITCH_SKIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDZiZTY1ZjQ0Y2QyMTAxNGM4Y2RkZDAxNThiZjc1MjI3YWRjYjFmZDE3OWY0YzFhY2QxNThjODg4NzFhMTNmIn19fQ==";

    public PlayerInfoMenu(GamePlayer viewer, GamePlayer player) {
        this(viewer, player, null);
    }

    public PlayerInfoMenu(GamePlayer viewer, GamePlayer player, Consumer<GamePlayer> callback) {
        super(player.getName(), 36);

        player.isOffline(); // trigger the "destroyed" state to set to true

        // Main head info
        String skin;
        if (player.getTexture() != null) {
            skin = Skins.loadSkin(player.getTexture());
        } else {
            skin = MenuUtil.LOCKED_SKIN;
        }
        ItemUI head = new ItemUI(skin, click -> {
            if (click.playerModel().getRank().isAtLeast(Rank.SUPER_STAFF)) {
                String url = "https://ender.mineclick.net/players?search=" + player.getUuid();
                TextComponent text = new TextComponent(ChatColor.DARK_AQUA + "[Ender url]");
                text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click!").color(net.md_5.bungee.api.ChatColor.GRAY).create()));
                text.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                Runner.sync(() -> click.playerModel().sendMessage(text));
            }
        });
        head.setUpdateConsumer(item -> {
            Statistic expStat = StatisticsService.i().get(player.getUuid(), StatisticType.EXP);
            long exp = expStat == null ? 0 : (long) expStat.getScore();
            item.setTitle(ChatColor.GOLD + "Profile info: " + ChatColor.YELLOW + player.getName());
            item.setLore(" ");
            item.addLore(ChatColor.DARK_GREEN + "Rank: " + ChatColor.YELLOW + player.getRank().getName());
            item.addLore(ChatColor.DARK_GREEN + "Level: " + ChatColor.YELLOW + LevelsService.i().getLevel(exp) + ChatColor.GOLD + " (" + exp + " EXP)");
            item.addLore(ChatColor.DARK_GREEN + "Dimension: " + ChatColor.YELLOW + player.getDimensionsData().getDimension().getName());
            item.addLore(ChatColor.DARK_GREEN + "Hours played: " + ChatColor.YELLOW + (player.getActivityData().getPlayTime() / 3600));

            if (viewer.getFriendsData().isFriendsWith(player.getUuid()) || viewer.getRank().isAtLeast(Rank.STAFF)) {
                if (player.isOnline()) {
                    item.addLore(ChatColor.DARK_GREEN + "Online: " + ChatColor.YELLOW + "yes");
                } else {
                    item.addLore(ChatColor.DARK_GREEN + "Last online: " + ChatColor.YELLOW + Formatter.exactDateTime(player.getActivityData().getLastOnlineAt()));
                }
            }

            if (viewer.getRank().isAtLeast(Rank.STAFF)) {
                item.addLore(" ");
                item.addLore(ChatColor.DARK_GREEN + "Gold: " + ChatColor.YELLOW + player.getGold().print(viewer));
                item.addLore(ChatColor.DARK_GREEN + "Schmepls: " + ChatColor.YELLOW + player.getSchmepls());

                if (player.getActivityData().getAutoClickerKicks() > 0) {
                    item.addLore(ChatColor.DARK_GREEN + "Auto-clicker detected: " + player.getActivityData().getAutoClickerKicks());
                }

                if (!player.getOffences().isEmpty()) {
                    item.addLore(ChatColor.DARK_GREEN + "Reports: ");
                    for (OffenceData offence : player.getOffences()) {
                        item.addLore(ChatColor.YELLOW + " - " + offence.getType().getReason() + " " + Formatter.exactDateTime(offence.getPunishedOn()));
                    }
                }

                if (OffencesService.i().isCurrentlyPunishedWith(player, PunishmentType.BAN)) {
                    OffenceData offence = OffencesService.i().getHighestOffence(player, PunishmentType.BAN);
                    item.addLore(" ");
                    item.addLore(ChatColor.DARK_RED + "BANNED");
                    item.addLore(ChatColor.DARK_GRAY + "  for " + offence.getType().getReason());
                    item.addLore(ChatColor.DARK_GRAY + "  by " + offence.getPunishedBy());
                    item.addLore(ChatColor.DARK_GRAY + "  on " + offence.getPardonedOn());
                    String duration = offence.isPermanent() ? "eternity"
                            : Formatter.duration(offence.getPunishedOn().plus(offence.getDurationMinutes(), ChronoUnit.MINUTES).toEpochMilli() - System.currentTimeMillis());
                    item.addLore(ChatColor.DARK_GRAY + "  for next " + duration);
                }
            }
        });
        setItem(13, head);

//        //YouTube/Twitch link
//        if (hasYouTubeLink || hasTwitchLink) {
//            String platform = (hasYouTubeLink ? "YouTube" : "Twitch");
//            ItemUI youtubeItem = new ItemUI(ItemBuilder.builder().skull(hasYouTubeLink ? YOUTUBE_SKIN : TWITCH_SKIN), clickPack -> {
//                viewer.getPlayer().closeInventory();
//
//                String url;
//                if (hasYouTubeLink) {
//                    url = "https://www.youtube.com/channel/" + dataMap.get("youtubeLink");
//                } else {
//                    url = "https://www.twitch.tv/" + dataMap.get("twitchLink");
//                }
//
//                TextComponent text = new TextComponent(ChatColor.GREEN + "Visit " + name + "'s " + platform + " channel" + ChatColor.GRAY + " [click]");
//                text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to visit " + name + "'s\n" + platform + " channel").color(net.md_5.bungee.api.ChatColor.GRAY).create()));
//                text.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
//                viewer.getPlayer().spigot().sendMessage(text);
//            });
//            youtubeItem.setName(ChatColor.YELLOW + name + "'s " + platform + " channel");
//            youtubeItem.addLore(" ");
//            youtubeItem.addLore(ChatColor.GRAY + "Click to visit " + name + "'s");
//            youtubeItem.addLore(ChatColor.GRAY + platform + " channel");
//            setItem(14, youtubeItem);
//        }

        //Report
        boolean self = viewer.getUuid().equals(player.getUuid());
        boolean isStaff = player.getRank().isAtLeast(Rank.STAFF);

        boolean cant = self || isStaff;
        ItemStack reportItem = ItemBuilder.builder()
                .material(cant ? Material.RED_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE)
                .title(ChatColor.YELLOW + "Report the player")
                .lore(ChatColor.GRAY + (cant ? "Can't report this player" : "Click to report " + player.getName()))
                .build().toItem();
        ItemUI report = new ItemUI(reportItem, click -> {
            if (self) {
                viewer.sendMessage("You can't report yourself", MessageType.ERROR);
                return;
            }
            if (isStaff) {
                viewer.sendMessage("You can't report this player", MessageType.ERROR);
                return;
            }
            if (!viewer.getRank().isAtLeast(Rank.STAFF)
                    && viewer.getActivityData().getLastCreatedReport() != null
                    && viewer.getActivityData().getLastCreatedReport().plus(3, ChronoUnit.MINUTES).isAfter(Instant.now())) {
                viewer.sendMessage("You need to wait to report again", MessageType.ERROR);
                return;
            }

            new ReportMenu(viewer, player);
        });
        report.setUpdateConsumer(item -> {
            long time = viewer.getActivityData().getLastCreatedReport() != null
                    ? viewer.getActivityData().getLastCreatedReport().toEpochMilli() + 180000 // 3 min
                    : 0;
            boolean notCooled = !viewer.getRank().isAtLeast(Rank.STAFF) && time > System.currentTimeMillis();
            item.setMaterial(cant || notCooled ? Material.RED_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE);
            item.setLore(ChatColor.GRAY +
                    (cant ? "Can't report this player" : notCooled
                            ? "You can report again in: " + Formatter.duration(System.currentTimeMillis() - time)
                            : "Click to report " + player.getName()));
        });
        setItem(25, report);

        //Friend
        Supplier<Boolean> friends = () -> viewer.getFriendsData().isFriendsWith(player.getUuid());
        Supplier<Boolean> sentRequest = () -> viewer.getFriendsData().hasSentRequest(player.getUuid());
        Supplier<Boolean> receivedRequest = () -> viewer.getFriendsData().hasReceivedRequest(player.getUuid());
        ItemStack friendStack = ItemBuilder.builder()
                .material(friends.get() ? Material.RED_STAINED_GLASS_PANE : sentRequest.get() || self ? Material.GRAY_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE)
                .title(ChatColor.GRAY + "Loading...")
                .build().toItem();
        ItemUI friendItem = new ItemUI(friendStack, click -> {
            if (self) {
                viewer.sendMessage("Can't add this friend because " + Strings.getFunnyReason(), MessageType.ERROR);
                return;
            }

            //Delete
            if (friends.get()) {
                MenuUtil.openConfirmationMenu(viewer, aBoolean -> {
                    if (aBoolean) {
                        FriendsService.i().removeFriend(viewer, player);
                        viewer.sendMessage(player.getName() + " was removed from your friends list", MessageType.INFO);

                        viewer.getFriendsMenu().reloadFriends();
                    }
                    openMenu(viewer, player, callback);
                }, ChatColor.GOLD + "Are you sure you want to", ChatColor.GOLD + "remove " + player.getName() + " from your friends?");
                return;
            }

            //Cancel
            if (sentRequest.get()) {
                FriendsService.i().cancelRequest(viewer, player);
                viewer.sendMessage("Canceled a friend request sent to " + player.getName(), MessageType.INFO);
                viewer.getFriendsMenu().reloadFriends();
                openMenu(viewer, player, callback);
                return;
            }

            //Accept
            if (receivedRequest.get()) {
                FriendsService.i().answerRequest(viewer, player, true);
                viewer.getFriendsMenu().reloadFriends();
                openMenu(viewer, player, callback);
                return;
            }

            //Request
            FriendsService.i().sendRequest(viewer, player.getName());
            viewer.getFriendsMenu().reloadFriends();
            openMenu(viewer, player, callback);
        });
        friendItem.setUpdateConsumer(item -> {
            item.setMaterial(friends.get() ? Material.RED_STAINED_GLASS_PANE : sentRequest.get() || self ? Material.GRAY_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
            item.setTitle(ChatColor.YELLOW + (friends.get() ? "Friends" : sentRequest.get() ? "Sent request" : receivedRequest.get() ? "Received request" : "Not friends"));
            item.setLore(ChatColor.GRAY + (friends.get() ? "Click to remove" : sentRequest.get() ? "Click to cancel" : receivedRequest.get() ? "Click to accept" : "Click to send a request"));
        });
        setItem(21, friendItem);

        //Msg
        ItemStack msgStack = ItemBuilder.builder()
                .material(Material.ORANGE_STAINED_GLASS_PANE)
                .title(ChatColor.YELLOW + "Message")
                .lore(ChatColor.GRAY + "Click to send a")
                .lore(ChatColor.GRAY + "private message")
                .build().toItem();
        ItemUI msgItem = new ItemUI(msgStack, clickPack -> {
            viewer.getPlayer().closeInventory();

            TextComponent text = new TextComponent(ChatColor.AQUA + "[Click to msg " + ChatColor.BOLD + player.getName() + ChatColor.AQUA + "]");
            text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to send a private msg to " + player.getName()).color(net.md_5.bungee.api.ChatColor.GREEN).create()));
            text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + player.getName() + " "));
            viewer.getPlayer().spigot().sendMessage(text);
        });
        setItem(19, msgItem);

        //Visit island
        ItemStack visitStack = ItemBuilder.builder()
                .material(friends.get() ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .title(ChatColor.YELLOW + "Visit " + player.getName() + "'s island")
                .build().toItem();
        ItemUI visitItem = new ItemUI(visitStack, clickPack -> {
            if (!player.getTutorial().isComplete()) {
                player.sendMessage("You need to finish your tutorial before you can visit");
                return;
            }

            if (friends.get()) {
                if (!player.isOnline() || player.getServer() == null) {
                    viewer.sendMessage("Can't visit this player, they are offline", MessageType.ERROR);
                    return;
                }

                if (!player.isOffline()) { // is on the same server
                    IslandsService.i().visitPlayer(viewer, player);
                } else {
                    viewer.getOnJoinData().setVisitPlayer(player.getName());
                    TpService.i().tpToServer(viewer, player.getServer());
                }
                viewer.getPlayer().closeInventory();
            } else {
                viewer.sendMessage("You can only visit friends", MessageType.ERROR);
            }
        });
        visitItem.setUpdateConsumer(item -> {
            item.setMaterial(friends.get() ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
            item.setLore(ChatColor.GRAY + (friends.get() ? "Click to visit their island" : "You can only visit friends"));
        });
        setItem(23, visitItem);

        //Staff stuff like unban, unmute etc
        if (viewer.getRank().isAtLeast(Rank.STAFF)) {
            if (OffencesService.i().isCurrentlyPunishedWith(player, PunishmentType.MUTE)) {
                ItemStack unmuteStack = ItemBuilder.builder()
                        .material(Material.RED_STAINED_GLASS_PANE)
                        .title(ChatColor.YELLOW + "Un-mute")
                        .lore(ChatColor.GRAY + "Click to un-mute")
                        .build().toItem();
                ItemUI unmuteItem = new ItemUI(unmuteStack, clickPack ->
                        OffencesService.i().pardon(viewer, player, PunishmentType.MUTE, () ->
                                openMenu(viewer, player.getUuid(), callback))); // TODO maybe just edit the existing player data?
                setItem(27, unmuteItem);
            }

            // Unban
            if (OffencesService.i().isCurrentlyPunishedWith(player, PunishmentType.BAN)) {
                OffenceData offence = OffencesService.i().getHighestOffence(player, PunishmentType.BAN);
                if (offence.getPunishedBy().equals(viewer.getName()) || viewer.getRank().isAtLeast(Rank.DEV)) {
                    ItemStack unbanStack = ItemBuilder.builder()
                            .material(Material.RED_STAINED_GLASS_PANE)
                            .title(ChatColor.YELLOW + "Un-ban")
                            .lore(ChatColor.GRAY + "Click to un-ban")
                            .build().toItem();
                    ItemUI unbanItem = new ItemUI(unbanStack, clickPack ->
                            MenuUtil.openConfirmationMenu(viewer, aBoolean -> {
                                if (aBoolean) {
                                    OffencesService.i().pardon(viewer, player, PunishmentType.BAN, () -> {
                                        Runner.sync(() -> openMenu(viewer, player.getUuid(), callback));
                                    });
                                }

                                MenuUtil.openLoadingMenu(viewer.getPlayer());
                            }, "Are you sure you want to unban " + player.getName()));
                    setItem(28, unbanItem);
                }
            }
        }

        //Set rank
        if (viewer.getRank().isAtLeast(Rank.SUPER_STAFF)) {
            ItemStack setRankStack = ItemBuilder.builder()
                    .material(Material.ORANGE_STAINED_GLASS_PANE)
                    .title(ChatColor.GREEN + "Set rank")
                    .lore(ChatColor.GRAY + "Click to set rank")
                    .build().toItem();
            ItemUI setRank = new ItemUI(setRankStack, clickPack -> {
                InventoryUI ranksUi = new InventoryUI("", 27);

                int pos = 10;
                ItemBuilder.ItemBuilderBuilder builder = ItemBuilder.builder();
                for (Rank r : Rank.values()) {
                    if (r.getWeight() > viewer.getRank().getWeight()) continue;

                    boolean has = player.getRank().equals(r);
                    ItemBuilder.ItemBuilderBuilder item = builder
                            .title(ChatColor.YELLOW + r.getName())
                            .material(has ? Material.GRAY_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE)
                            .clearLores()
                            .lore(ChatColor.GRAY + (has ? "Current rank" : "Click to set"));
                    ranksUi.setItem(pos++, new ItemUI(item, click -> {
                        if (!has) {
                            if (!player.isOffline()) {
                                player.setRank(r);
                                player.setPremiumForLife(r.equals(Rank.PAID));

                                viewer.sendMessage("Set " + r.getName() + " rank to " + player.getName(), MessageType.INFO);
                                openMenu(viewer, player, callback);
                                return;
                            }

                            // TODO create a handler for this one day...
                            if (player.isOnline()) { // yeah ok, hold your wtf... they are offline on the server, but online on the network
                                viewer.sendMessage("The player is online, teleport to their server first");
                                return;
                            }

                            PlayersService.i().edit(player.getUuid(), playerModel -> {
                                playerModel.setRank(r);
                                playerModel.setPremiumForLife(r.equals(Rank.PAID));
                                player.setRank(r); // to keep the menu up to date

                                viewer.sendMessage("Set " + r.getName() + " rank to " + player.getName(), MessageType.INFO);
                                Runner.sync(() -> openMenu(viewer, player, callback));
                            });
                        }
                    }));
                }

                ranksUi.setItem(0, MenuUtil.getCloseMenu(p -> openMenu(viewer, player, callback)));
                ranksUi.open(viewer.getPlayer());
            });
            setItem(29, setRank);
        }

        if (viewer.getRank().isAtLeast(Rank.DEV)) {
            //Give boosters
            ItemStack giveBoosterStack = ItemBuilder.builder()
                    .material(Material.ORANGE_STAINED_GLASS_PANE)
                    .title(ChatColor.GREEN + "Give boosters")
                    .lore(ChatColor.GRAY + "Click to give boosters")
                    .build().toItem();
            ItemUI giveBoosters = new ItemUI(giveBoosterStack, clickPack -> {
                InventoryUI ui = new InventoryUI("", 27);

                int pos = 10;
                ItemBuilder.ItemBuilderBuilder builder = ItemBuilder.builder().material(Material.LIME_STAINED_GLASS_PANE).lore(ChatColor.GRAY + "Click to give");
                for (BoosterType b : BoosterType.values()) {
                    ui.setItem(pos++, new ItemUI(builder.title(ChatColor.YELLOW + b.getName()).build().toItem(), c -> {
                        if (!player.isOffline()) {
                            player.getBoosters().compute(b, (type, i) -> i == null ? 1 : i + 1);

                            viewer.sendMessage("Gave 1 " + b.getName() + " to " + player.getName());
                            return;
                        }

                        if (player.isOnline()) {
                            viewer.sendMessage("The player is online, teleport to their server first");
                            return;
                        }

                        PlayersService.i().<GamePlayer>edit(player.getUuid(), playerModel -> {
                            playerModel.getBoosters().compute(b, (type, i) -> i == null ? 1 : i + 1);
                            player.getBoosters().compute(b, (type, i) -> i == null ? 1 : i + 1); // to keep the menu up to date

                            viewer.sendMessage("Gave 1 " + b.getName() + " to " + player.getName());
                        });
                    }));
                }

                ui.setItem(0, MenuUtil.getCloseMenu(p -> openMenu(viewer, player, callback)));
                ui.open(viewer.getPlayer());
            });
            setItem(30, giveBoosters);
        }

        setItem(0, MenuUtil.getCloseMenu(callback));
        open(viewer.getPlayer());
    }

    public static void openMenu(GamePlayer viewer, String name) {
        if (Bukkit.getPlayerExact(name) != null) {
            UUID uuid = Bukkit.getPlayerExact(name).getUniqueId();

            openMenu(viewer, uuid, null);
        } else {
            MenuUtil.openLoadingMenu(viewer.getPlayer());

            UUIDFetcher.getUUID(name, uuid -> {
                if (uuid == null) {
                    Runner.sync(() -> {
                        viewer.getPlayer().closeInventory();
                        viewer.sendMessage("No such player: " + ChatColor.DARK_RED + name, MessageType.ERROR);
                    });
                } else {
                    PlayersService.i().<GamePlayer>get(
                            uuid,
                            playerModel -> {
                                if (playerModel == null) {
                                    Runner.sync(() -> {
                                        viewer.getPlayer().closeInventory();
                                        viewer.sendMessage("No such player: " + ChatColor.DARK_RED + name, MessageType.ERROR);
                                    });

                                    return;
                                }

                                StatisticsService.i().load(playerModel.getUuid());
                                Runner.sync(() -> openMenu(viewer, playerModel, null));
                            }
                    ).orLoad(false);
                }
            });
        }
    }

    public static void openMenu(GamePlayer viewer, UUID uuid, Consumer<GamePlayer> callback) {
        PlayersService.i().<GamePlayer>get(
                uuid,
                playerModel -> {
                    StatisticsService.i().load(playerModel.getUuid());
                    Runner.sync(() -> openMenu(viewer, playerModel, callback));
                }
        ).orLoad(false);
    }

    public static void openMenu(GamePlayer viewer, GamePlayer player, Consumer<GamePlayer> callback) {
        new PlayerInfoMenu(viewer, player, callback);
    }
}
