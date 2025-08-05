package net.mineclick.game.messenger;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.core.messenger.Message;
import net.mineclick.core.messenger.MessageName;
import net.mineclick.game.model.Statistic;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@MessageName("statistics")
public class StatisticsHandler extends Message {
    private Set<UUID> uuids;
    private Set<String> keys;
    private Map<UUID, Map<String, Statistic>> statistics;
}
