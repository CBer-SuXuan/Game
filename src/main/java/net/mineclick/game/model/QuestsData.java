package net.mineclick.game.model;

import lombok.Data;

@Data
public class QuestsData {
    private CampfireData campfireData = new CampfireData();
    private CookieThievesData cookieThievesData = new CookieThievesData();
    private BookshelvesData bookshelvesData = new BookshelvesData();
}
