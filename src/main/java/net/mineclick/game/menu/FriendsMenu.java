package net.mineclick.game.menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.service.FriendsService;
import net.mineclick.global.service.PlayerListService;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.Skins;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FriendsMenu {
    private final GamePlayer player;
    private List<Friend> friends = new ArrayList<>();
    private List<Friend> pending = new ArrayList<>();
    private boolean loading;

    public FriendsMenu(GamePlayer player) {
        this.player = player;

        reloadFriends();
    }

    public void open(Sort sort) {
        open(sort, 0, null);
    }

    public void open(Sort sort, int index, Consumer<GamePlayer> callback) {
        if (loading) {
            MenuUtil.openLoadingMenu(player.getPlayer());
            Runner.sync(0, 1, state -> {
                if (!loading) {
                    state.cancel();
                    open(sort, 0, callback);
                }
            });
        } else {
            InventoryUI ui = new InventoryUI(sort.getName(), 54);

            for (Sort s : Sort.values()) {
                ItemStack stack = ItemBuilder.builder()
                        .material(sort == s ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                        .amount(Math.max(1, getMapBySort(s).size()))
                        .title(ChatColor.GREEN + s.name)
                        .lore(ChatColor.GRAY + (sort == s ? "Selected" : s.getLore()))
                        .build().toItem();
                ui.setItem(s.getPosition(), new ItemUI(stack, clickPack -> {
                    if (sort != s) {
                        player.clickSound();
                        open(s, 0, callback);
                    }
                }));
            }

            List<Friend> friends = getMapBySort(sort);
            int position = 10;
            int i = 0;
            for (Friend friend : friends) {
                if (i++ < index)
                    continue;

                String name = friend.getName();
                ItemUI itemUI = new ItemUI(friend.texture, clickPack -> {
                    PlayerInfoMenu.openMenu(player, friend.uuid, p -> open(sort, index, callback));
                    player.clickSound();
                });
                itemUI.setUpdateConsumer(item -> {
                    boolean online = PlayerListService.i().getOnlineNames().contains(name);
                    item.setTitle((online ? ChatColor.GREEN : ChatColor.GRAY) + name);
                    item.setLore(ChatColor.GRAY + (online ? "Online" : "Offline"));
                    item.addLore(" ");
                    item.addLore(ChatColor.GRAY + "Click for details");
                });

                ui.setItem(position, itemUI);
                if ((position + 2) % 9 == 0) {
                    position += 2;
                }
                position++;

                if (i >= index + 28) {
                    int nextIndex = i;
                    ui.setItem(53, new ItemUI(ItemBuilder.builder().material(Material.BOOK).title(ChatColor.GREEN + "Next page"), clickPack -> {
                        open(sort, nextIndex, callback);
                        player.clickSound();
                    }));
                    break;
                }
            }

            if (index != 0) {
                ui.setItem(45, new ItemUI(ItemBuilder.builder().material(Material.BOOK).title(ChatColor.GREEN + "Previous page"), clickPack -> {
                    open(sort, index - 28, callback);
                    player.clickSound();
                }));
            }

            ui.setItem(8, new ItemUI(ItemBuilder.builder().material(Material.NETHER_STAR).title(ChatColor.GREEN + "Add friends").lore(ChatColor.GRAY + "Click to add a friend").build().toItem(), clickPack -> {
                TextComponent text = new TextComponent(ChatColor.GREEN + "Click here to add a friend " + ChatColor.GRAY + "[click]");
                text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/friends add "));
                text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here!").color(net.md_5.bungee.api.ChatColor.GRAY).create()));

                player.getPlayer().getOpenInventory().close();
                player.getPlayer().spigot().sendMessage(text);
            }));

            ui.setItem(0, MenuUtil.getCloseMenu(callback));
            ui.open(player.getPlayer());
        }
    }

    private List<Friend> getMapBySort(Sort sort) {
        return sort == Sort.PENDING ? pending : friends;
    }

    public void reloadFriends() {
        loading = true;

        Set<UUID> uuids = new HashSet<>();
        uuids.addAll(player.getFriendsData().getFriends());
        uuids.addAll(player.getFriendsData().getSentRequests());
        uuids.addAll(player.getFriendsData().getReceivedRequests());

        FriendsService.i().getTextures(uuids, textures -> {
            if (player.isOffline()) return;

            friends = mapAndFilter(textures, player.getFriendsData().getFriends());
            pending = mapAndFilter(textures, player.getFriendsData().getReceivedRequests());
            pending.addAll(mapAndFilter(textures, player.getFriendsData().getSentRequests()));

            loading = false;
        });
    }

    private List<Friend> mapAndFilter(Map<UUID, String> textures, List<UUID> list) {
        List<String> onlineNames = PlayerListService.i().getOnlineNames();
        return list.stream()
                .map(uuid -> new Friend(uuid, textures.get(uuid)))
                .filter(friend -> friend.getName() != null)
                .sorted(Comparator.comparing(friend -> onlineNames.contains(friend.getName())))
                .collect(Collectors.toList());
    }

    @Getter
    @RequiredArgsConstructor
    public enum Sort {
        ALL("All friends", "Click to see all friends", 3),
        PENDING("Pending friends", "Click to see pending friends", 5);

        private final String name;
        private final String lore;
        private final int position;
    }

    @RequiredArgsConstructor
    private static class Friend {
        private final UUID uuid;
        private final String texture;
        private String name;

        public String getName() {
            if (name == null && texture != null) {
                name = Skins.getUsernameFromTexture(texture);
            }

            return name;
        }
    }
}
