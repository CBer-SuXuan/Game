package net.mineclick.game.model.pickaxe;

import lombok.Data;
import net.md_5.bungee.api.ChatColor;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.*;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupCategory;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.mineclick.global.util.location.RandomVector;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.awt.*;
import java.util.Collections;
import java.util.UUID;

/**
 * Note: This handles both pickaxe and orb powerups.
 * Originally this was only built to handle pickaxe, so some refactoring will be needed, whenever that happens...
 */
@Data
public class PickaxePowerup {
    private double charge = 0;

    private transient BossBar bossBar;
    private transient NamespacedKey bossBarId;
    private transient GamePlayer player;
    private transient float hue = 0.25F;
    private transient float hueGoal = 0.25F;
    private transient long lastClick = System.currentTimeMillis();
    private transient double activatedDischargeRate = 0;
    private transient BigNumber collectedGold;
    private transient BigNumber goldPerTick;
    private transient long collectedTime;
    private transient boolean bold;
    private transient Powerup powerup;
    private transient boolean enabled;
    private transient long lastOrbActivation = System.currentTimeMillis();
    private transient long lastRightClick = 0;

    public void update(GamePlayer player) {
        this.player = player;

        boolean wasEnabled = enabled;
        enabled = PowerupService.i().getProgress(player, PowerupCategory.PICKAXE).getLevel() > 0;
        if (wasEnabled == enabled) return;

        if (enabled) {
            createBossBar();
        } else {
            removeBossBar();
        }
    }

    public void createBossBar() {
        bossBarId = new NamespacedKey(Game.i(), UUID.randomUUID().toString());
        bossBar = Bukkit.createBossBar(bossBarId, ChatColor.GOLD + "Click to charge the powerup", BarColor.PURPLE, BarStyle.SOLID);
        bossBar.addPlayer(player.getPlayer());
    }

    public void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            Bukkit.removeBossBar(bossBarId);
            bossBar = null;
        }
    }

    public void tick(long ticks) {
        if (!enabled || (charge == 0 && hue == 0.25F && collectedTime == 0)) return;

        if (collectedTime > 0 && (System.currentTimeMillis() - collectedTime > 5000 || charge == 1)) {
            collectedTime = 0;
            collectedGold = null;
            bold = false;
            powerup = null;
        }

        if (activatedDischargeRate > 0) {
            charge -= activatedDischargeRate;
            if (goldPerTick != null && collectedGold != null) {
                // award
                if (isClickingDynamicBlock()) {
                    if (!player.getDynamicMineBlocks().isEmpty()) {
                        int clicks = (int) goldPerTick.doubleValue();
                        int clicked = 0;
                        if (DynamicMineBlocksService.i().click(player, powerup.getRandomBlock(), clicks)) {
                            clicked += clicks;
                        }

                        collectedGold = collectedGold.add(new BigNumber(clicked));
                    }
                } else {
                    BigNumber toAdd = goldPerTick;
                    if (ticks % 20 == 0 && SkillsService.i().has(player, SkillType.SUPERBLOCK_3)) {
                        BigNumber superBlockReward = player.getSuperBlockReward(toAdd);
                        if (superBlockReward != null) {
                            toAdd = superBlockReward;

                            Location location = powerup.getRandomBlock().getLocation().add(0.5, 0.5, 0.5);
                            ParticlesUtil.send(ParticleTypes.LAVA, location, Triple.of(.3F, .3F, .3F), 1, player);
                            HologramsService.i().spawnBlockBreak(
                                    location.add(new RandomVector(Game.getRandom().nextDouble() * 0.8)),
                                    toAdd,
                                    true,
                                    Collections.singleton(player)
                            );
                        }
                    }

                    collectedGold = collectedGold.add(toAdd);
                    player.addGold(toAdd);
                }
            }

            if (charge <= 0) {
                charge = 0;
                activatedDischargeRate = 0;
                collectedTime = System.currentTimeMillis();
            }
        } else {
            if (charge < 1 && !player.getRank().isAtLeast(Rank.PAID) && lastClickExpired()) {
                // 100 seconds to discharge by default or 200 with the skill
                charge -= SkillsService.i().has(player, SkillType.POWERUP_2) ? 0.00025 : 0.0005;
                if (charge < 0) {
                    charge = 0;
                }
            }
        }

        if (bossBar != null) {
            bossBar.setProgress(charge);
            boolean full = charge == 1;

            if (full) {
                hueGoal = hue <= 0.06F ? 0.14F : hue >= 0.14F ? 0.06F : hueGoal;
            } else {
                hueGoal = 0.25F - (Math.min(player.getActivityData().calculateAvgCPS() * 2, 20) * 0.0125F);
            }
            if (hue < hueGoal) {
                hue += 0.005;
            } else if (hue > hueGoal) {
                hue -= 0.005;
            }

            ChatColor color = ChatColor.of("#" + Integer.toHexString(Color.getHSBColor(hue, 1, 1).getRGB()).substring(2));
            if (full) {
                bossBar.setTitle(color.toString() + ChatColor.BOLD + "CHARGED PICKAXE POWERUP " + ChatColor.DARK_AQUA + "right-click to activate");
            } else if (collectedGold != null) {
                int level = PowerupService.i().getProgress(player, PowerupCategory.PICKAXE).getLevel();

                if (isClickingDynamicBlock()) {
                    bossBar.setTitle((collectedTime > 0 ? ChatColor.GREEN + "Clicked " : "") + color + collectedGold.print(player, false, true, bold) + ChatColor.RESET + ChatColor.GREEN + (collectedTime > 0 ? " times " + ChatColor.GRAY + "(lvl " + level + ")" : ""));
                } else {
                    bossBar.setTitle((collectedTime > 0 ? ChatColor.GREEN + "Collected " : "") + color + collectedGold.print(player, false, true, bold) + ChatColor.RESET + ChatColor.GREEN + (collectedTime > 0 ? " gold " + ChatColor.GRAY + "(lvl " + level + ")" : ""));
                }
            } else if (charge == 0) {
                bossBar.setTitle(ChatColor.GOLD + "Click to charge the powerup");
            } else {
                bossBar.setTitle(color + "Pickaxe Powerup " + ChatColor.DARK_AQUA + (int) (charge * 100) + "%");
            }
        }
    }

    private boolean lastClickExpired() {
        return System.currentTimeMillis() - lastClick > 5000;
    }

    private boolean isClickingDynamicBlock() {
        return powerup != null && powerup.getDynamicMineBlockType() != null && powerup.getDynamicMineBlockType().isPowerupAllowed();
    }

    public double getChargeRate(PowerupCategory category, boolean superBlock) {
        double chargeRate = category.getChargeRate();
        if (superBlock && SkillsService.i().has(player, SkillType.POWERUP_3)) {
            chargeRate = 0.05;
        } else if (SkillsService.i().has(player, SkillType.POWERUP_4)) {
            chargeRate *= 1.25;
        } else if (SkillsService.i().has(player, SkillType.POWERUP_1)) {
            chargeRate *= 1.1;
        }

        return chargeRate;
    }

    public void click(boolean superBlock) {
        handleOrbClick();
        lastClick = System.currentTimeMillis();

        if (activatedDischargeRate > 0 || charge == 1) return;

        charge += getChargeRate(PowerupCategory.PICKAXE, superBlock);
        if (SkillsService.i().has(player, SkillType.POWERUP_6) && Game.getRandom().nextDouble() <= 0.005) {
            charge = 1;
        }

        if (charge >= 1) {
            charge = 1;
            player.playSound(Sound.BLOCK_NOTE_BLOCK_BELL);
        }
    }

    private void handleOrbClick() {
        long currentTime = System.currentTimeMillis();
        if (lastClickExpired()) {
            lastOrbActivation = currentTime;
            return;
        }

        if (lastOrbActivation + (long) (1D / getChargeRate(PowerupCategory.ORB, false) * 1000) <= currentTime) {
            lastOrbActivation = currentTime;
            PowerupType type = PowerupService.i().getSelectedPowerup(player, PowerupCategory.ORB);
            if (type == null) return;

            type.run(player);
        }
    }

    public void rightClick() {
        if (System.currentTimeMillis() - lastRightClick < 1000) return;
        lastRightClick = System.currentTimeMillis();

        if (!enabled || charge < 1 || activatedDischargeRate > 0) return;

        PowerupType type = PowerupService.i().getSelectedPowerup(player, PowerupCategory.PICKAXE);
        if (type == null) return;

        charge = 1;
        powerup = type.run(player);
        if (!powerup.isLoaded() || (MineshaftService.i().isInMineshaft(player) && powerup.getDynamicMineBlockType() == null)) {
            return;
        }

        activatedDischargeRate = 1 / (powerup.getPeriod() * 20);
        collectedGold = new BigNumber(0);

        if (isClickingDynamicBlock()) {
            BigNumber power = new BigNumber(PowerupService.i().getPower(player, PowerupCategory.PICKAXE, false));
            goldPerTick = power.multiply(new BigNumber(activatedDischargeRate));
        } else {
            BigNumber gold = PowerupService.i().getGoldReward(player, PowerupCategory.PICKAXE, false);

            double multiplier = SkillsService.i().has(player, SkillType.POWERUP_5) && Game.getRandom().nextDouble() <= 0.1 ? 5 : 1;
            goldPerTick = gold.multiply(new BigNumber(activatedDischargeRate * multiplier));
            bold = multiplier > 1;
        }
    }
}
