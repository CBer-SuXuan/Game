package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.game.type.BoosterType;

import java.util.Date;
import java.util.List;

@Data
public class ActiveBooster {
    private BoosterType type;
    private List<String> players;
    private Date expiresAt;
    private int duration;
}
