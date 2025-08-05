package net.mineclick.game.messenger;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.core.messenger.Action;
import net.mineclick.core.messenger.Message;
import net.mineclick.core.messenger.MessageName;
import net.mineclick.game.model.ActiveBooster;
import net.mineclick.game.service.BoostersService;

import java.util.List;

@Getter
@Setter
@MessageName("boosters")
public class BoostersHandler extends Message {
    private List<ActiveBooster> boosters;

    @Override
    public void onReceive() {
        if (getAction().equals(Action.UPDATE)) {
            BoostersService.i().update(boosters);
        }
    }
}
