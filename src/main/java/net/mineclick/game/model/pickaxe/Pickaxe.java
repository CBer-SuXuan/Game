package net.mineclick.game.model.pickaxe;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.mineclick.game.model.IncrementalModel;
import net.mineclick.game.service.*;
import net.mineclick.game.type.BoosterType;
import net.mineclick.game.type.quest.villager.CollectorQuest;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Pickaxe extends IncrementalModel {
    private int amount = 1;

    private transient int tempAmount = 1;
    private transient PickaxeConfiguration configuration;

    /**
     * Update the item in player's inventory
     */
    public void updateItem() {
        tempAmount = amount;
        updateItem(0);
    }

    public BigNumber getIncomeDiff(long levelsToAdd) {
        BigNumber income = getIncome();

        return levelsToAdd <= 0 ? income : calculateIncome(getLevel() + levelsToAdd).subtract(income);
    }

    @Override
    public void recalculate() {
        setIncome(calculateIncome(getLevel()));
    }

    private BigNumber calculateIncome(long level) {
        BigNumber baseIncome = getConfiguration().getBaseIncome();
        BigNumber income = baseIncome.multiply(new BigNumber(level * getTotalMultiplier()));
        if (!SkillsService.i().has(getPlayer(), SkillType.PICKAXE_6)) {
            return income;
        }

        // max of x1,000 at level 1,000
        double exp = Math.pow(1.006932, Math.min(level, 1000));
        return income.multiply(new BigNumber(exp));
    }

    @Override
    public void increaseLevel(long toAdd) {
        super.increaseLevel(toAdd);

        AchievementsService.i().setProgress(getPlayer(), "upgradepickaxe", getLevel());
        QuestsService.i().incrementProgress(getPlayer(), "dailyPickaxe", 0, (int) toAdd);

        updateConfiguration();
        updateItem(0);
    }

    /**
     * Update the item in player's inventory
     *
     * @param amountChange By what to change the pickaxe amount
     */
    public void updateItem(int amountChange) {
        if (configuration == null) {
            return;
        }

        tempAmount += amountChange;
        if (tempAmount < 0) tempAmount = 0;
        if (tempAmount > amount) {
            tempAmount = amount;
        }

        ItemBuilder.ItemBuilderBuilder builder = ItemBuilder.builder()
                .title(ChatColor.YELLOW + configuration.getName() + ChatColor.GREEN + " lvl " + (getLevel()));

        ItemStack itemStack = builder.lore(" ")
                .lore(ChatColor.GRAY.toString() + ChatColor.ITALIC + "Left click to mine")
                .material(configuration.getMaterial())
                .amount(tempAmount)
                .glowing(configuration.isGlow())
                .build().toItem();

        getPlayer().getPlayer().getInventory().setItem(MineshaftService.i().isInMineshaft(getPlayer()) ? 0 : 4, itemStack);
    }

    @Override
    public double getTotalMultiplier() {
        double gM = Math.pow(2, (int) ((getLevel() - 50) / 50));
        if (gM != getMultiplier()) {
            if (getMultiplier() != 0 && getPlayer().getPlayer() != null) {
                getPlayer().sendMessage(ChatColor.YELLOW + "Your " + ChatColor.GREEN + "Pickaxe " + ChatColor.YELLOW + "is now " + ChatColor.GREEN + Formatter.format(gM) + "x" + ChatColor.YELLOW + " more productive");
            }
            setMultiplier(gM);
        }

        return super.getTotalMultiplier() * getCollectorQuestMultiplier() * BoostersService.i().getActiveBoost(BoosterType.PICKAXE_BOOSTER);
    }

    private double getCollectorQuestMultiplier() {
        CollectorQuest quest = (CollectorQuest) QuestsService.i().getQuest("collector");
        if (quest != null) {
            return quest.getMultiplier(getPlayer());
        }

        return 1;
    }

    public void updateConfiguration() {
        for (PickaxeConfiguration config : PickaxeService.i().getConfigurations().values()) {
            if (getLevel() >= config.getMinLevel()) {
                configuration = config;
            }

            if (getId().equals(config.getId())) return;
        }
    }
}

