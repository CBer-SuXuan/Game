package net.mineclick.game.type.powerup.orb;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.mineclick.game.Game;
import net.mineclick.game.model.DynamicMineBlock;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.DynamicMineBlocksService;
import net.mineclick.game.service.HologramsService;
import net.mineclick.game.service.PowerupService;
import net.mineclick.game.service.SkillsService;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupCategory;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.util.visual.Orb;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.ItemBuilder;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public abstract class OrbPowerup extends Powerup {
    public static double ROTATIONS = 2;
    public static int SPIN_TICKS = 60;
    private final List<Orb> orbs = new ArrayList<>();
    private final ItemStack itemStack;
    @Setter
    private Block targetedBlock;

    public OrbPowerup(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);

        ItemBuilder.ItemBuilderBuilder builder = ItemBuilder.builder();
        if (powerupType.getSkull() != null) {
            builder.skull(getPowerupType().getSkull());
        } else {
            builder.material(getPowerupType().getMaterial());
        }
        itemStack = CraftItemStack.asNMSCopy(builder.build().toItem());
    }

    @Override
    public void tick(long ticks) {
        int size = orbs.size();
        if (ticks % 5 == 0 && size < maxOrbs()) {
            Orb orb = new Orb(getPlayer(), itemStack);
            orb.setSpinTicks(SPIN_TICKS);
            orb.setOnShoot(o -> {
                moveToRandomBlock(o);

                schedule(5, () -> onShoot(o));
                return true;
            });
            orb.setRotationSpeed(ROTATIONS * 2 * Math.PI / getSpinTicks());
            orbs.add(orb);
            addEntity(orb);

            onOrbSpawn(orb);
        }
    }

    private int maxOrbs() {
        return Math.min(getLevel(), 6);
    }

    public void onOrbSpawn(Orb orb) {
    }

    public int getSpinTicks() {
        return SPIN_TICKS;
    }

    public void moveToRandomBlock(Orb orb) {
        targetedBlock = getRandomBlock();
        if (targetedBlock == null) return;

        Location blockLocation = targetedBlock.getLocation();
        Vector v = getPlayer().getPlayer().getLocation().toVector().subtract(blockLocation.toVector()).normalize();

        DynamicMineBlock dynamicMineBlock = DynamicMineBlocksService.i().get(getPlayer(), targetedBlock);
        if (dynamicMineBlock != null) {
            int clicks = (int) PowerupService.i().getPower(getPlayer(), PowerupCategory.ORB, false) / maxOrbs();
            dynamicMineBlock.onClick(getPlayer(), clicks);
            HologramsService.i().spawnFloatingUp(blockLocation.add(v), p -> ChatColor.GREEN.toString() + clicks + ChatColor.GOLD + " clicks", Collections.singleton(getPlayer()));
        } else {
            double multiplier = SkillsService.i().has(getPlayer(), SkillType.POWERUP_5) && Game.getRandom().nextDouble() <= 0.1 ? 5 : 1;
            BigNumber gold = PowerupService.i().getGoldReward(getPlayer(), PowerupCategory.ORB, false).multiply(new BigNumber(multiplier));
            HologramsService.i().spawnFloatingUp(blockLocation.add(v), p -> ChatColor.GOLD + "Orb gold: " + gold.print(getPlayer(), false, true, multiplier > 1), Collections.singleton(getPlayer()));
            getPlayer().addGold(gold);
        }

        orb.moveTo(targetedBlock.getX() + 0.5, targetedBlock.getY(), targetedBlock.getZ() + 0.5);
    }

    /**
     * @param orb The orb that finishing its spinning ticks
     * @return True if it should stop spinning if it hasn't already been stopped
     */
    public abstract boolean onShoot(Orb orb);
}
