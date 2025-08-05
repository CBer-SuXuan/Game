package net.mineclick.game.type;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

@Getter
public enum StatisticType {
    CLICKS,
    ASCENDS,
    EXP,
    VOTES,
    PARKOUR_FAILS,
    PARKOUR_SUCCESS,
    SPLEEF_GAMES,
    SPLEEF_WINS,
    BROKEN_CRIMSON,
    SILVERFISH,
    ;

    private final String key;
    private final String display;

    StatisticType() {
        key = this.name().toLowerCase();
        display = StringUtils.capitalize(key);
    }

    StatisticType(String key, String display) {
        this.key = key;
        this.display = display;
    }

    public static StatisticType getByKey(String key) {
        return Arrays.stream(values()).filter(type -> type.getKey().equals(key)).findFirst().orElse(null);
    }
}
