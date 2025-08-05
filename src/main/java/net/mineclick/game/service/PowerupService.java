package net.mineclick.game.service;

import net.mineclick.game.Game;
import net.mineclick.game.menu.PowerupEffectMenu;
import net.mineclick.game.menu.PowerupsMenu;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeItem;
import net.mineclick.game.model.PowerupProgress;
import net.mineclick.game.type.BoosterType;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.powerup.PowerupCategory;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.SingletonInit;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SingletonInit
public class PowerupService {
    private static PowerupService i;

    private PowerupService() {
        // populate geodes list
        for (PowerupType type : PowerupType.values()) {
            Rarity rarity = type.getRarity();
            if (rarity.equals(Rarity.SPECIAL) && (type.getHoliday() == null || !type.getHoliday().isNow())) {
                continue;
            }

            String name = ChatColor.GREEN + type.getName() + ChatColor.YELLOW + " " + type.getCategory().toString().toLowerCase() + " Powerup Effect";
            ItemBuilder.ItemBuilderBuilder itemBuilder = ItemBuilder.builder();
            if (type.getSkull() != null) {
                itemBuilder.skull(type.getSkull());
            } else {
                itemBuilder.material(type.getMaterial());
            }
            GeodeItem geodeItem = new GeodeItem(name, rarity,
                    player -> unlockPowerup(player, type),
                    player -> player.getUnlockedPowerups().contains(type),
                    itemBuilder
            );
            GeodesService.i().addToListOfGeodes(geodeItem);
        }
    }

    public static PowerupService i() {
        return i == null ? i = new PowerupService() : i;
    }

    public void loadPlayerPowerups(GamePlayer player) {
        for (PowerupCategory category : PowerupCategory.values()) {
            player.getPowerupsProgress()
                    .computeIfAbsent(category, t -> new PowerupProgress())
                    .setCategory(category);
        }
    }

    public boolean isUnlocked(GamePlayer player, PowerupCategory category) {
        return player.getUnlockedPowerups().stream().anyMatch(powerupType -> powerupType.getCategory().equals(category));
    }

    public PowerupType getRandomUnlocked(GamePlayer player, PowerupCategory category) {
        List<PowerupType> types = player.getUnlockedPowerups().stream()
                .filter(type -> type.getCategory().equals(category))
                .collect(Collectors.toList());

        return types.isEmpty() ? null : types.get(Game.getRandom().nextInt(types.size()));
    }

    public PowerupProgress getProgress(GamePlayer player, PowerupCategory category) {
        return player.getPowerupsProgress().get(category);
    }

    public void addParts(GamePlayer player, int amount) {
        player.setPowerupParts(player.getPowerupParts() + amount);
    }

    public void unlockPowerup(GamePlayer player, PowerupType type) {
        if (!player.getUnlockedPowerups().contains(type)) {
            // check if unlocked for the first time
            boolean unlocked = isUnlocked(player, type.getCategory());
            if (!unlocked) {
                player.sendImportantMessage(StringUtils.capitalize(type.getCategory().toString().toLowerCase()) + " Powerup unlocked!", "Check out Main Menu");
                player.levelUpSound();
            }

            player.getUnlockedPowerups().add(type);
        }
    }

    public boolean unlockNextLevel(GamePlayer player, PowerupCategory category) {
        PowerupProgress progress = getProgress(player, category);
        int neededParts = progress.getPartsToNextLevel();
        int schmeplsCost = progress.getSchmeplsCost();
        if (neededParts <= player.getPowerupParts() && player.chargeSchmepls(schmeplsCost)) {
            player.setPowerupParts(player.getPowerupParts() - neededParts);

            progress.setParts(progress.getParts() + neededParts);
            progress.setLevel(progress.getLevel() + 1);
            return true;
        }

        return false;
    }

    public BigNumber getGoldReward(GamePlayer player, PowerupCategory category, boolean nextLevel) {
        double power = getPower(player, category, nextLevel);
        power *= BoostersService.i().getActiveBoost(BoosterType.POWERUPS_BOOSTER);

        BigNumber gold = player.getPickaxe().getIncome();
        return gold.multiply(new BigNumber(power));
    }

    public double getPower(GamePlayer player, PowerupCategory category, boolean nextLevel) {
        PowerupProgress progress = getProgress(player, category);

        double power = nextLevel ? progress.getNextLevelPower() : progress.getPower();

        // apply the powerup perk
        if (progress.getSelectedType() != null && progress.getSelectedType().getPerk() != null) {
            double perk = progress.getSelectedType().getPerk().getPerk(player);
            if (perk > 0) {
                power *= (1 + perk);
            }
        }

        return power;
    }

    public void openMenu(GamePlayer player) {
        new PowerupsMenu(player);
    }

    public void openEffectMenu(GamePlayer player, PowerupCategory category) {
        new PowerupEffectMenu(player, category);
    }

    public double getTotalParts(GamePlayer player) {
        return player.getPowerupsProgress().values().stream().mapToInt(PowerupProgress::getParts).sum() + player.getPowerupParts();
    }

    public void resetParts(GamePlayer player) {
        int toAdd = 0;
        for (PowerupProgress progress : player.getPowerupsProgress().values()) {
            toAdd += progress.getParts();
            progress.setParts(0);
            progress.setLevel(0);
        }

        addParts(player, toAdd);
    }

    @Nullable
    public PowerupType getSelectedPowerup(GamePlayer player, PowerupCategory category) {
        PowerupProgress progress = getProgress(player, category);

        PowerupProgress.SelectionType selection = progress.getSelection();
        if (selection.equals(PowerupProgress.SelectionType.BEST) && !player.isRankAtLeast(Rank.PAID)) {
            selection = PowerupProgress.SelectionType.MANUAL;
            progress.setSelection(selection);
        }

        if (selection.equals(PowerupProgress.SelectionType.MANUAL)) {
            PowerupType type = progress.getSelectedType();
            if (type != null) {
                return type;
            }
        }

        if (selection.equals(PowerupProgress.SelectionType.BEST)) {
            PowerupType bestType = progress.getSelectedType();

            if (bestType == null || System.currentTimeMillis() - progress.getLastCalculatedBest() > 10000) {
                bestType = player.getUnlockedPowerups().stream()
                        .filter(powerupType -> powerupType.getCategory().equals(category) && powerupType.getPerk() != null)
                        .max(Comparator.comparingDouble(type -> type.getPerk().getPerk(player)))
                        .orElse(null);

                progress.setSelectedType(bestType);
                progress.setLastCalculatedBest(System.currentTimeMillis());
            }

            if (bestType != null) {
                return bestType;
            }
        }

        return getRandomUnlocked(player, category);
    }

    public void setSelectedPowerup(GamePlayer player, PowerupCategory category, @Nullable PowerupType powerupType) {
        PowerupProgress progress = getProgress(player, category);
        progress.setSelection(PowerupProgress.SelectionType.MANUAL);
        progress.setSelectedType(powerupType);
    }

    public Pair<Long, Long> getUnlockedCount(GamePlayer player, PowerupCategory category) {
        long total = Arrays.stream(PowerupType.values()).filter(powerupType -> powerupType.getCategory().equals(category)).count();
        long unlocked = player.getUnlockedPowerups().stream().filter(powerupType -> powerupType.getCategory().equals(category)).count();
        return Pair.of(unlocked, total);
    }
}
