package net.mineclick.game.util;

import net.mineclick.core.util.Formatter;
import org.junit.Test;

public class FormatterTest {
    @Test
    public void roundedTime() throws Exception {
        System.out.print(Formatter.roundedTime(60 * 1000)); //60 sec
        System.out.print(Formatter.roundedTime(5 * 60 * 1000)); //5 min
        System.out.print(Formatter.roundedTime(25 * 60 * 1000)); //25 min
        System.out.print(Formatter.roundedTime(60 * 60 * 1000)); //60 min
        System.out.print(Formatter.roundedTime(15 * 60 * 60 * 1000)); //15 hours
        System.out.print(Formatter.roundedTime(24 * 60 * 60 * 1000)); //1 day
        System.out.print(Formatter.roundedTime(3 * 24 * 60 * 60 * 1000)); //3 days
        System.out.print(Formatter.roundedTime(30 * 24 * 60 * 60 * 1000L)); //30 days
        System.out.print(Formatter.roundedTime(2 * 30 * 24 * 60 * 60 * 1000L)); //2 month
        System.out.print(Formatter.roundedTime(12 * 30 * 24 * 60 * 60 * 1000L)); //a year
        System.out.print(Formatter.roundedTime(2 * 12 * 30 * 24 * 60 * 60 * 1000L)); //2 years
    }
}