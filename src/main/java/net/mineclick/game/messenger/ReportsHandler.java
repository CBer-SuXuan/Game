package net.mineclick.game.messenger;

import lombok.Setter;
import net.mineclick.core.messenger.Message;
import net.mineclick.core.messenger.MessageName;

@Setter
@MessageName("reports")
public class ReportsHandler extends Message {
    private String reporterName;
    private String reporterDiscordId;
    private String reason;
    private String punishment;
    private String offenderName;
    private String offenderUuid;
}
