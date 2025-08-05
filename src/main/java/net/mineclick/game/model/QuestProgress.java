package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.game.type.quest.Quest;

import javax.annotation.Nullable;
import java.time.Instant;

@Data
public class QuestProgress {
    private int taskProgress;
    private int objective;
    private boolean complete;
    private int completedCount; // 1 for normal, >= 1 for daily
    private Instant completedAt;

    private transient GamePlayer player;
    @Nullable
    private transient Quest quest;
    private transient boolean talkedToVillager;
}
