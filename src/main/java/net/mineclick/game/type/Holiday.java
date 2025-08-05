package net.mineclick.game.type;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.MonthDay;

public enum Holiday {
    APRIL_FOOLS("April fool's", MonthDay.of(Month.APRIL, 1), MonthDay.of(Month.APRIL, 2)),
    WINTER("Winter", MonthDay.of(Month.DECEMBER, 1), MonthDay.of(Month.JANUARY, 6)),
    CHRISTMAS("Christmas", MonthDay.of(Month.DECEMBER, 25), MonthDay.of(Month.DECEMBER, 27)), // 48 hours to make sure every time zone is included
    HALLOWEEN("Halloween", MonthDay.of(Month.OCTOBER, 20), MonthDay.of(Month.NOVEMBER, 11)),
    SUMMER("Summer", MonthDay.of(Month.JUNE, 1), MonthDay.of(Month.SEPTEMBER, 1)),
    ;

    @Getter
    private final String name;
    private final LocalDateTime from;
    private final LocalDateTime to;

    Holiday(String name, MonthDay from, MonthDay to) {
        int year = LocalDateTime.now().getYear();
        this.name = name;
        this.from = LocalDateTime.of(year, from.getMonth(), from.getDayOfMonth(), 0, 0);
        this.to = LocalDateTime.of(to.getMonthValue() < from.getMonthValue() ? year + 1 : year, to.getMonth(), to.getDayOfMonth(), 0, 0);
    }

    public boolean isNow() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(from) && now.isBefore(to);
    }
}
