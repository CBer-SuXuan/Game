package net.mineclick.game.model.worker;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.IncrementalModel;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.service.AchievementsService;
import net.mineclick.game.service.BoostersService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.service.SkillsService;
import net.mineclick.game.type.BoosterType;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.global.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;

@Data
@EqualsAndHashCode(callSuper = true)
public class Worker extends IncrementalModel {
    private int incomePercent = 0;
    private double levelMultiplier = 1;
    private int excitedTicks = 0;
    private boolean noAutoCookies;

    private transient WorkerConfiguration configuration;
    private transient EntityWorker entityWorker;
    private transient WorkerType type;

    public EntityWorker spawn(IslandModel island) {
        if (entityWorker != null) {
            entityWorker.discard();
        }

        entityWorker = type.spawn(island, this);
        return entityWorker;
    }

    public void clear() {
        if (entityWorker != null) {
            entityWorker.discard();
            entityWorker = null;
        }
    }

    public void increaseLevel(long toAdd) {
        super.increaseLevel(toAdd);

        AchievementsService.i().setProgress(getPlayer(), "upgrade" + type.toString().toLowerCase(), getLevel());
        QuestsService.i().incrementProgress(getPlayer(), "dailyWorkers", 0, (int) toAdd);

        getPlayer().getWorkers().values().forEach(Worker::recalculateIncomePercent);

        if (entityWorker != null) {
            entityWorker.updateWorker();
        }
    }

    @Override
    public double getTotalMultiplier() {
        if (SkillsService.i().has(getPlayer(), SkillType.WORKERS_1)) {
            double lM = Math.pow(2, (int) (getLevel() / 50));
            if (lM != levelMultiplier) {
                if (getPlayer().getPlayer() != null) {
                    getPlayer().sendMessage(ChatColor.GREEN + getConfiguration().getName() + ChatColor.YELLOW + " is now " + ChatColor.GREEN + Formatter.format(lM) + "x" + ChatColor.YELLOW + " more productive");
                }
                levelMultiplier = lM;
            }
        } else {
            levelMultiplier = 1;
        }

        double multiplier = 1;
        if (getLevel() >= 50 && SkillsService.i().has(getPlayer(), SkillType.WORKERS_3)) {
            multiplier *= 2;
        }
        if (getLevel() >= 100 && SkillsService.i().has(getPlayer(), SkillType.WORKERS_4)) {
            multiplier *= 5;
        }
        if (getLevel() >= 250 && SkillsService.i().has(getPlayer(), SkillType.WORKERS_5)) {
            multiplier *= 10;
        }
        if (SkillsService.i().has(getPlayer(), SkillType.WORKERS_6)) {
            multiplier *= 10;
        }

        if (isExcited()) {
            if (SkillsService.i().has(getPlayer(), SkillType.COOKIE_3)) {
                multiplier *= 2;
            } else {
                multiplier *= 1.5;
            }
        }

        double total = levelMultiplier * super.getTotalMultiplier() * multiplier;
        if (!getPlayer().getActivityData().isAfk()) {
            total *= BoostersService.i().getActiveBoost(BoosterType.GOLD_BOOSTER);
        }
        return total;
    }

    @Override
    public BigNumber cost(long toBuy) {
        long threshold = 50;
        long level = getLevel();
        long levelToBe = level + toBuy;

        if (levelToBe >= threshold && SkillsService.i().has(getPlayer(), SkillType.WORKERS_2)) {
            if (level < threshold) {
                long fullPriceLevels = threshold - level;
                return super.cost(fullPriceLevels).add(super.cost(toBuy - fullPriceLevels, threshold).multiply(new BigNumber(0.9)));
            }

            return super.cost(toBuy).multiply(new BigNumber(0.9));
        }

        return super.cost(toBuy);
    }

    public void recalculateIncomePercent() {
        if (getPlayer() == null)
            return;

        BigNumber totalWorkersIncome = getPlayer().getTotalWorkersIncome();
        if (totalWorkersIncome.smallerThanOrEqual(BigNumber.ZERO))
            return;

        double percent = getIncome().divide(totalWorkersIncome).doubleValue();
        incomePercent = (int) (percent * 100);
    }

    public void showFor(GamePlayer player) {
        if (entityWorker == null)
            return;

        player.getAllowedEntities().add(entityWorker.getId());
        player.sendPacket(new ClientboundAddEntityPacket(entityWorker));
        entityWorker.updateWorker();
    }

    public void removeFor(GamePlayer player) {
        if (entityWorker == null)
            return;

        player.sendPacket(new ClientboundRemoveEntitiesPacket(entityWorker.getId()));
        player.getAllowedEntities().remove(entityWorker.getId());
    }

    public void giveCookie() {
        GamePlayer player = getPlayer();

        excitedTicks = SkillsService.i().has(player, SkillType.COOKIE_2) ? 600 : 300; // excite them for 15 sec (or 30)

        recalculate();
        if (entityWorker != null) {
            Location location = entityWorker.getLocation().add(0, entityWorker.getBbHeight() / 2D, 0);
            ParticlesUtil.send(ParticleTypes.HAPPY_VILLAGER, location, Triple.of(0.25F, entityWorker.getBbHeight() / 2F, 0.25F), 20, getPlayer().getCurrentIsland().getAllPlayers());
            Runner.sync(0, 5, state -> {
                if (state.getTicks() > 3) {
                    player.playSound(Sound.ENTITY_PLAYER_BURP, location, 1, 1);
                    state.cancel();
                    return;
                }

                player.playSound(Sound.ENTITY_GENERIC_EAT, location, 1, 1);
            });

            entityWorker.updateWorker();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (excitedTicks > 0) {
            excitedTicks--;

            if (excitedTicks == 0) {
                recalculate();
                if (getEntityWorker() != null) {
                    getEntityWorker().updateWorker();
                }
            }
        }
    }

    public boolean isExcited() {
        return excitedTicks > 0;
    }
}
