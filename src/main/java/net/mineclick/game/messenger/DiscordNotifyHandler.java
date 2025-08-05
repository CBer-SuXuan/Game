package net.mineclick.game.messenger;

import net.mineclick.core.messenger.Action;
import net.mineclick.core.messenger.Message;
import net.mineclick.core.messenger.MessageName;
import net.mineclick.core.messenger.Response;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.service.PlayersService;

import java.util.UUID;

@MessageName("discordNotify")
public class DiscordNotifyHandler extends Message {
    private UUID uuid;
    private String setting;

    @Override
    public void onReceive() {
        if (getAction().equals(Action.DELETE)) {
            PlayersService.i().<GamePlayer>get(uuid, playerModel -> {
                playerModel.getNotify().remove(setting);
                send(Response.OK);
            });
        }
    }
}
