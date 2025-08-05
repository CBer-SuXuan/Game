package net.mineclick.game.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.skills.SkillType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkillsService {
    private static SkillsService i;

    public static SkillsService i() {
        return i == null ? i = new SkillsService() : i;
    }

    /**
     * @param player    The player
     * @param skillType Skill type
     * @return True if the player has this skill type unlocked
     */
    public boolean has(GamePlayer player, SkillType skillType) {
        return player.getSkills().contains(skillType);
    }

    /**
     * Check if all the prerequisites were met for the given skill type to be unlocked
     *
     * @param player    The player
     * @param skillType Skill type
     * @return True if the skill type can be unlocked
     */
    public boolean canUnlock(GamePlayer player, SkillType skillType) {
        if (has(player, skillType)) return false;

        SkillType previous = skillType.getPrevious();
        boolean previousUnlocked = previous == null || has(player, previous);

        return previousUnlocked
                && player.getSchmepls() >= skillType.getCost()
                && LevelsService.i().getLevel(player.getExp()) >= skillType.getMinLevel();
    }

    /**
     * @param player The player
     * @return True if the player has anything to unlock
     */
    public boolean hasAnythingToUnlock(GamePlayer player) {
        for (SkillType skillType : SkillType.values()) {
            if (canUnlock(player, skillType)) return true;
        }

        return false;
    }

    /**
     * Unlock a given skill type for the player
     *
     * @param player    The player
     * @param skillType The skill type
     */
    public void unlock(GamePlayer player, SkillType skillType) {
        if (!has(player, skillType)) {
            player.getSkills().add(skillType);
            if (skillType.getOnPurchase() != null) {
                skillType.getOnPurchase().accept(player);
            }
        }
    }

    public int getUnlocked(GamePlayer player, SkillType.Category category) {
        return (int) player.getSkills().stream().filter(skillType -> skillType.getCategory().equals(category)).count();
    }
}
