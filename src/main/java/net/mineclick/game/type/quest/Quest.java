package net.mineclick.game.type.quest;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.global.util.Strings;
import org.bukkit.ChatColor;

import javax.annotation.Nullable;
import java.util.List;

public abstract class Quest {
    public abstract String getId();

    public abstract String getName(GamePlayer player);

    public abstract List<QuestObjective> getObjectives();

    public abstract int getExpReward(GamePlayer player);

    public abstract int getSchmeplsReward(GamePlayer player);

    public boolean hasPrerequisite(GamePlayer player) {
        return true;
    }

    @Nullable
    public QuestProgress getQuestProgress(GamePlayer player) {
        return player.getQuests().get(getId());
    }

    public QuestObjective getObjective(int index) {
        return getObjectives().get(Math.min(index, getObjectives().size() - 1));
    }

    public boolean isNewQuest(GamePlayer player) {
        QuestProgress progress = getQuestProgress(player);
        return progress == null;
    }

    public boolean isComplete(GamePlayer player) {
        QuestProgress progress = getQuestProgress(player);
        return progress != null && progress.isComplete();
    }

    public boolean isNewObjective(GamePlayer player) {
        QuestProgress progress = getQuestProgress(player);
        QuestObjective objective = getObjectives().get(progress == null ? 0 : Math.min(progress.getObjective(), getObjectives().size() - 1));
        boolean complete = progress != null && progress.isComplete();
        return objective.talkToVillager() && !complete;
    }

    public void sendQuestUpdateMessage(GamePlayer player, @Nullable String firstLine, @Nullable String secondLine) {
        player.sendMessage(Strings.line());
        player.sendMessage(Strings.middle(ChatColor.GOLD + getName(player)));
        player.sendMessage(" ");
        if (firstLine != null) {
            player.sendMessage(Strings.middle(firstLine));
        }
        if (secondLine != null) {
            player.sendMessage(Strings.middle(secondLine));
        }
        player.sendMessage(Strings.line());
    }

    public boolean isUnlocked(GamePlayer player) {
        return getQuestProgress(player) != null;
    }
}
