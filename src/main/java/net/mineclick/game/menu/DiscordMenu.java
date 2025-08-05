package net.mineclick.game.menu;

import net.mineclick.core.messenger.Action;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.messenger.DiscordLinkHandler;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class DiscordMenu extends InventoryUI {
    private final GamePlayer player;

    public DiscordMenu(GamePlayer player) {
        super("         MineClick Discord", 54);
        this.player = player;

        //Booster activated
        setItem(19, MenuUtil.getSettingItem(player, "Booster activated", "Notify if a booster is activated", player.getPlayerSettings().getDiscordBooster(), true));

        //Vaults fill up
        setItem(21, MenuUtil.getSettingItem(player, "Vaults fill-up", "Notify when the vaults are full", player.getPlayerSettings().getDiscordVaults(), true));

        //Friends join
        setItem(23, MenuUtil.getSettingItem(player, "Friends join", "Notify when a friend joins", player.getPlayerSettings().getDiscordFriends(), true));

        //Daily reward refill
        setItem(25, MenuUtil.getSettingItem(player, "Daily reward", "Notify when the daily rewards refill", player.getPlayerSettings().getDiscordReward(), true));

        //Send if afk
        setItem(45, MenuUtil.getSettingItem(player, "AFK mode", "Also send notifications\nwhen you are AFK", player.getPlayerSettings().getDiscordAfk()));

        //Unlink
        setItem(53, new ItemUI(ItemBuilder.builder().material(Material.TNT).title(ChatColor.RED + "Unlink").lore(ChatColor.GRAY + "Click to unlink Discord").build().toItem(), clickPack -> {
            if (player.getDiscordId() == null) return;

            player.sendMessage("Successfully unlinked your Discord account", MessageType.INFO);
            player.clickSound();
            player.getPlayer().getOpenInventory().close();

            DiscordLinkHandler handler = new DiscordLinkHandler();
            handler.setDiscordId(player.getDiscordId());
            handler.send(Action.DELETE);

            // just fiy, just setting this to null won't actually remove it since the handler on the Ender side ignores null values.
            // What happens instead is that the field is set null from the DiscordLinkHandler call above (in Ender)
            player.setDiscordId(null);
        }));

        setItem(0, MenuUtil.getCloseMenu(p -> p.getSettingsMenu().open(p.getPlayer())));
        setDestroyOnClose(false);
    }

    public void checkSettings() {
        if (!player.getRank().isAtLeast(Rank.PAID) || player.getDiscordId() == null) {
            player.getPlayerSettings().getDiscordBooster().set(false);
            player.getPlayerSettings().getDiscordVaults().set(false);
            player.getPlayerSettings().getDiscordFriends().set(false);
            player.getPlayerSettings().getDiscordReward().set(false);
        }
    }

    public void open() {
        open(player.getPlayer());
    }
}
