package net.mineclick.game.commands;

import net.mineclick.game.menu.FriendsMenu;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.commands.Commands;
import net.mineclick.global.service.FriendsService;
import net.mineclick.global.util.SingletonInit;
import net.mineclick.global.util.Strings;
import org.bukkit.ChatColor;

@SingletonInit
public class FriendsCommand {
    private static FriendsCommand i;

    private FriendsCommand() {
        init();
    }

    public static FriendsCommand i() {
        return i == null ? i = new FriendsCommand() : i;
    }

    private void init() {
        Commands.addCommand(Commands.Command.builder()
                .name("friends")
                .description("Friends command (list or add)")
                .usage("[all/pending/add]")
                .minArgs(1)
                .playersTabComplete(true)
                .callFunction((playerData, strings) -> {
                    if (strings[0].equalsIgnoreCase("add") && strings.length > 1) {
                        String name = strings[1];
                        if (name.equalsIgnoreCase(playerData.getName())) {
                            return ChatColor.RED + "Error: " + Strings.getFunnyReason();
                        }

                        FriendsService.i().sendRequest(playerData, name);
                    } else if (strings[0].equalsIgnoreCase("pending")) {
                        ((GamePlayer) playerData).getFriendsMenu().open(FriendsMenu.Sort.PENDING);
                    } else if (strings[0].equalsIgnoreCase("all")) {
                        ((GamePlayer) playerData).getFriendsMenu().open(FriendsMenu.Sort.ALL);
                    }

                    return null;
                })
                .build());
    }
}
