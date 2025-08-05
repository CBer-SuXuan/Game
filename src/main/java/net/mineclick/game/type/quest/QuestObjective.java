package net.mineclick.game.type.quest;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;

import javax.annotation.Nullable;


public abstract class QuestObjective {
    private final String name;
    private final int value;

    public QuestObjective(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public QuestObjective() {
        name = "";
        value = 0;
    }

    public String getName(GamePlayer player) {
        return name;
    }

    public int getValue(GamePlayer player) {
        return value;
    }

    public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
    }

    public boolean sendCompleteObjective() {
        return false;
    }

    public boolean hideAfterNextObjective() {
        return false;
    }

    public boolean talkToVillager() {
        return false;
    }

    public void completeObjective(@Nullable QuestProgress questProgress) {
        if (questProgress != null) {
            questProgress.setTaskProgress(value);
        }
    }

    public abstract static class Talk extends QuestObjective {
        private final boolean initial;

        public Talk(String villagerName, boolean initial) {
            super("Talk to " + villagerName, 1);
            this.initial = initial;
        }

        @Override
        public boolean sendCompleteObjective() {
            return !initial;
        }

        @Override
        public boolean talkToVillager() {
            return true;
        }
    }
}
