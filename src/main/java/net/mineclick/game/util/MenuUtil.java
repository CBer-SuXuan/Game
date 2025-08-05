package net.mineclick.game.util;


import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.WorkersService;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MenuUtil {
    public static final String ARROW_DOWN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDJjNWRjNTA4YzA1MjY5MzY1NjY2MTViNWM3YTY2OWEwYWQyNzFiMTBhMDExNzhjMDVjYjc0ODg2ZTIxMTU5MSJ9fX0=";
    public static final String ARROW_UP = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVmNzczYWM5NzJkZDU4YzgxMmQzOGUwMTczMTUyNThkMTA0NmY4MDBlNDRkODljNTc0ZmMyNTdjZGE0YTdiYiJ9fX0=";
    public static final String LOCKED_SKIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWYyMmI2YTNhMGYyNGJkZWVhYjJhNmFjZDliMWY1MmJiOTU5NGQ1ZjZiMWUyYzA1ZGRkYjIxOTQxMGM4In19fQ==";

    public static String prerequisiteGold(GamePlayer player, BigNumber amount, String prefix) {
        return prerequisite(player.getGold().greaterThanOrEqual(amount), amount.print(player) + ChatColor.YELLOW + " gold", prefix);
    }

    public static String prerequisiteWorker(GamePlayer player, WorkerType type, long level, String prefix) {
        return prerequisite(
                player.getWorkers().containsKey(type) && player.getWorkers().get(type).getLevel() >= level,
                ChatColor.YELLOW + WorkersService.i().getConfigurations().get(type).getName() + " lvl " + level, prefix);
    }

    public static String prerequisite(boolean has, String extra, String prefix) {
        return ChatColor.GRAY + prefix + (has ? ChatColor.GREEN + "[✔] " : ChatColor.RED + "[✘] ") + extra;
    }

    public static ItemUI setVisitingLockedSkull(ItemUI itemUI) {
        return setLockedSkull(itemUI, "Locked", "Visiting a friend's island");
    }

    public static ItemUI setLockedSkull(ItemUI itemUI) {
        return setLockedSkull(itemUI, "Can't unlock yet");
    }

    public static ItemUI setLockedSkull(ItemUI itemUI, String lore) {
        return setLockedSkull(itemUI, "???", lore);
    }

    public static ItemUI setLockedSkull(ItemUI itemUI, String title, String lore) {
        setSkull(itemUI, LOCKED_SKIN);

        itemUI.setTitle(ChatColor.DARK_GRAY + title);
        itemUI.setLore(ChatColor.GRAY + lore);

        return itemUI;
    }

    public static void openConfirmationMenu(GamePlayer player, Consumer<Boolean> consumer, String... lore) {
        InventoryUI menu = new InventoryUI("           Please Confirm...", 36);

        ItemStack cancelStack = ItemBuilder.builder().material(Material.RED_WOOL).title(ChatColor.RED + "CANCEL").build().toItem();
        for (int x = 15; x <= 16; x++) {
            for (int i = 0; i <= 1; i++) {
                menu.setItem(x + (i * 9), new ItemUI(cancelStack, clickPack -> {
                    menu.destroy();
                    consumer.accept(false);
                }));
            }
        }

        List<String> loreList = Arrays.stream(lore).map(s -> ChatColor.GRAY + s).collect(Collectors.toList());
        loreList.add(0, " ");
        ItemStack confirmStack = ItemBuilder.builder().material(Material.LIME_WOOL).title(ChatColor.GREEN + "CONFIRM").lores(loreList).build().toItem();
        for (int x = 10; x <= 11; x++) {
            for (int i = 0; i <= 1; i++) {
                menu.setItem(x + (i * 9), new ItemUI(confirmStack, clickPack -> {
                    menu.destroy();
                    consumer.accept(true);
                }));
            }
        }

        menu.open(player.getPlayer());
    }

    public static ItemUI setSkull(ItemUI itemUI, String skin) {
        itemUI.setMaterial(Material.PLAYER_HEAD);
        itemUI.setSkin(skin);
        itemUI.setTitle(ChatColor.DARK_GRAY + "Loading...");
        return itemUI;
    }

    public static ItemUI getCloseMenu() {
        return getCloseMenu(null);
    }

    public static ItemUI getCloseMenu(Consumer<GamePlayer> callback) {
        return new ItemUI(ItemBuilder.builder().material(Material.BARRIER).title(ChatColor.GRAY + "Close menu").build().toItem(), e -> {
            if (callback != null) {
                callback.accept((GamePlayer) e.playerModel());
            } else {
                e.playerModel().getPlayer().closeInventory();
            }
            e.playerModel().playSound(Sound.BLOCK_FENCE_GATE_CLOSE, 0.5, 2);
        });
    }

    public static void openLoadingMenu(Player player) {
        new InventoryUI("Loading", 9).open(player);
    }

    public static ItemUI getSettingItem(GamePlayer player, String setting, String description, AtomicBoolean settingValue) {
        return getSettingItem(player, setting, description, settingValue, false, null);
    }

    public static ItemUI getSettingItem(GamePlayer player, String setting, String description, AtomicBoolean settingValue, boolean premium) {
        return getSettingItem(player, setting, description, settingValue, premium, null);
    }

    public static ItemUI getSettingItem(GamePlayer player, String setting, String description, AtomicBoolean settingValue, boolean premium, Consumer<Boolean> setConsumer) {
        boolean enabled = settingValue.get() && (!premium || player.getRank().isAtLeast(Rank.PAID));

        ItemStack stack = ItemBuilder.builder()
                .material(enabled ? Material.LIME_WOOL : Material.RED_WOOL)
                .title(ChatColor.YELLOW + setting + (enabled ? ChatColor.GREEN + " - enabled" : ChatColor.RED + " - disabled"))
                .lore(ChatColor.GRAY + description.split("\n")[0])
                .lore(" ")
                .lore(premium && !player.getRank().isAtLeast(Rank.PAID) ? ChatColor.RED + "Requires Premium Membership" : (enabled ? ChatColor.RED : ChatColor.GREEN) + "Click to " + (enabled ? "disable" : "enable"))
                .build().toItem();
        ItemUI itemUI = new ItemUI(stack, clickPack -> {
            //TODO create a system to limit the number of clicks per minute to avoid griefers
            if (premium && !player.getRank().isAtLeast(Rank.PAID)) {
                player.noSound();
            } else {
                boolean set = !settingValue.get();
                player.clickSound();
                settingValue.set(set);
                if (setConsumer != null) {
                    setConsumer.accept(set);
                }
            }
        });
        itemUI.setUpdateConsumer(item -> {
            boolean e = settingValue.get();

            item.setMaterial(e ? Material.LIME_WOOL : Material.RED_WOOL);
            item.setTitle(ChatColor.YELLOW + setting + (e ? ChatColor.GREEN + " - enabled" : ChatColor.RED + " - disabled"));
            item.setLore("");
            for (String s : description.split("\n")) {
                item.addLore(ChatColor.GRAY + s);
            }
            item.addLore(" ");
            item.addLore(premium && !player.getRank().isAtLeast(Rank.PAID) ? ChatColor.RED + "Requires Premium Membership" : (e ? ChatColor.RED : ChatColor.GREEN) + "Click to " + (e ? "disable" : "enable"));
            if (premium && !player.getRank().isAtLeast(Rank.PAID)) {
                item.addLore(ChatColor.GRAY + "Get yours at " + ChatColor.AQUA + "store.mineclick.net");
            }
        });

        return itemUI;
    }
}
