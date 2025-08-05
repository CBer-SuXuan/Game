package net.mineclick.game.menu;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.service.ConfigurationsService;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.Strings;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class SettingsMenu extends InventoryUI {
    private static final String DISCORD_SKIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzg3M2MxMmJmZmI1MjUxYTBiODhkNWFlNzVjNzI0N2NiMzlhNzVmZjFhODFjYmU0YzhhMzliMzExZGRlZGEifX19";
//    private static final String YOUTUBE_SKIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDJmNmMwN2EzMjZkZWY5ODRlNzJmNzcyZWQ2NDU0NDlmNWVjOTZjNmNhMjU2NDk5YjVkMmI4NGE4ZGNlIn19fQ==";
//    private static final String TWITCH_SKIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDZiZTY1ZjQ0Y2QyMTAxNGM4Y2RkZDAxNThiZjc1MjI3YWRjYjFmZDE3OWY0YzFhY2QxNThjODg4NzFhMTNmIn19fQ==";

    private final GamePlayer player;

    public SettingsMenu(GamePlayer player) {
        super("         MineClick Settings", 54);
        this.player = player;

        boolean isYoutube = player.getRank().equals(Rank.YOUTUBER);
        boolean isTwitch = player.getRank().equals(Rank.TWITCH);

        //Discord
        ItemUI discordItem = new ItemUI(DISCORD_SKIN, clickPack -> {
            if (player.getDiscordId() != null) {
                player.getDiscordMenu().open();
            } else {
                if (!ConfigurationsService.i().contains("discordUrl")) {
                    player.sendMessage("Error creating a Discord link, please contact staff", MessageType.ERROR);
                    return;
                }

                if (player.getDiscordState() == null) {
                    player.setDiscordState(UUID.randomUUID().toString());
                    PlayersService.i().save(player);
                }

                String url = ConfigurationsService.i().get().getString("discordUrl") + "&state=" + player.getDiscordState();

                TextComponent textComponent = new TextComponent("Click here to link your Discord account");
                textComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to link your Discord account").color(net.md_5.bungee.api.ChatColor.AQUA).create()));
                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

                player.sendMessage(Strings.line());
                player.sendMessage(textComponent);
                player.sendMessage(Strings.line());
            }
            player.clickSound();
        });
        discordItem.setTitle(ChatColor.YELLOW + "Discord Settings");
        discordItem.addLore(" ");
        discordItem.setUpdateConsumer(item -> {
            discordItem.setLore(" ");
            if (player.getDiscordId() != null) {
                item.addLore(ChatColor.GRAY + "Click to see Discord settings");
            } else {
                item.addLore(ChatColor.GRAY + "Click to link your Discord account");
            }
        });
        setItem(isYoutube || isTwitch ? 3 : 4, discordItem);

        //YouTube/Twitch TODO not sure if I should remove these ranks...
//        if (isYoutube || isTwitch) {
//            ItemUI itemUI = new ItemUI(MenuUtil.getSkull(Skins.loadSkin(isYoutube ? YOUTUBE_SKIN : TWITCH_SKIN)), clickPack -> {
//                player.getStreamerMenu().open();
//                player.clickSound();
//            });
//            itemUI.setName(ChatColor.YELLOW + (isYoutube ? "YouTuber" : "Twitch") + " Settings");
//            itemUI.addLore(" ");
//            itemUI.addLore(ChatColor.GRAY + "Click to see the " + (isYoutube ? "YouTuber" : "Twitch") + " settings");
//            setItem(5, itemUI);
//        }

        //Display units
        setItem(21, "Numeric notation", "Display gold in engineering notation", player.getGameSettings().getNumericNotation());

        //Chat
        setItem(22, "Disable chat", "Only display announcements\nand important information in chat", player.getChatData().getDisabled());

        //Boosters
        setItem(23, "Hide Boosters", "Hide boosters from the sidebar", player.getPlayerSettings().getHideBoosters());

        //Friend msgs
        setItem(30, "Msg restriction", "Only let friends and staff\nsend you direct messages", player.getChatData().getPrivateFromFriendsOnly());

        //Friend visiting
        setItem(31, "No visiting", "Do not allow friends to\nvisit your island", player.getPlayerSettings().getNoIslandVisits());

        //Auto set parkour point
        setItem(32, MenuUtil.getSettingItem(player, "No auto-checkpoints", "Disable auto setting\nparkour checkpoints", player.getPlayerSettings().getNoAutoCheckpoints(), true));

        //Reset
        ItemStack resetItem = ItemBuilder.builder()
                .material(Material.TNT)
                .title(ChatColor.RED + "FULL RESET")
                .lore(ChatColor.DARK_RED + "Completely reset your account:")
                .lore(ChatColor.RED + "- Schmepls")
                .lore(ChatColor.RED + "- EXP and Level")
                .lore(ChatColor.RED + "- Achievements")
                .lore(ChatColor.RED + "- Leaderboards")
                .lore(ChatColor.RED + "- Every single stat")
                .lore(" ")
                .lore(ChatColor.GOLD + "You will only keep:")
                .lore(ChatColor.YELLOW + "- Premium Membership")
                .lore(ChatColor.YELLOW + "- Boosters")
                .build().toItem();
        setItem(53, new ItemUI(resetItem, pack -> MenuUtil.openConfirmationMenu(player, a -> {
            if (a) {
                player.hardReset();
            }
        }, ChatColor.RED + "RESET EVERYTHING!" + ChatColor.DARK_RED + " CANNOT BE UNDONE", "You will only keep:", "- Premium Membership", "- Boosters")));

        setItem(0, MenuUtil.getCloseMenu(p -> p.getMainMenu().open(p.getPlayer())));
        setDestroyOnClose(false);
    }

    private void setItem(int pos, String setting, String description, AtomicBoolean settingValue) {
        setItem(pos, MenuUtil.getSettingItem(player, setting, description, settingValue));
    }
}
