package net.mineclick.game.commands;

import net.mineclick.game.menu.PlayerInfoMenu;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.commands.Commands;
import net.mineclick.global.service.PlayerListService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.SingletonInit;

@SingletonInit
public class PlayerInfoCommand {
    private static PlayerInfoCommand i;

    private PlayerInfoCommand() {
        init();
    }

    public static PlayerInfoCommand i() {
        return i == null ? i = new PlayerInfoCommand() : i;
    }

    private void init() {
        Commands.addCommand(Commands.Command.builder()
                .name("lookup")
                .usage("<name>")
                .minRank(Rank.STAFF)
                .playersTabComplete(true)
                .description("Lookup a player")
                .minArgs(1)
                .callFunction((data, strings) -> {
                    PlayerInfoMenu.openMenu((GamePlayer) data, strings[0]);
                    return null;
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("report")
                .minRank(Rank.STAFF)
                .playersTabComplete(true)
                .hidden(true)
                .minArgs(1)
                .callFunction((data, strings) -> {
                    PlayerInfoMenu.openMenu((GamePlayer) data, strings[0]);
                    return null;
                })
                .build());

        Commands.addCommand(Commands.Command.builder()
                .name("playerinfo")
                .usage("<name>")
                .playersTabComplete(true)
                .hidden(true)
                .minArgs(1)
                .callFunction((data, strings) -> {
                    if (data.getRank().isAtLeast(Rank.STAFF) || PlayerListService.i().getOnlineNames().contains(strings[0])) {
                        PlayerInfoMenu.openMenu((GamePlayer) data, strings[0]);
                    }

                    return null;
                })
                .build());
    }
}
