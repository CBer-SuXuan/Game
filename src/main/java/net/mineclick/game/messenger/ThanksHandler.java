package net.mineclick.game.messenger;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.core.messenger.Message;
import net.mineclick.core.messenger.MessageName;
import net.mineclick.game.service.BoostersService;
import net.mineclick.global.model.ChatSenderData;

@Getter
@Setter
@MessageName("thanks")
public class ThanksHandler extends Message {
    private ChatSenderData from;

    @Override
    public void onReceive() {
        BoostersService.i().handleThanks(from);
    }
}
