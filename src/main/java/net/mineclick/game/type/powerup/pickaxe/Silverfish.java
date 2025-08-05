package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.global.util.Pair;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;

public class Silverfish extends Powerup {
    private final int maxCount;
    private int count;

    public Silverfish(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);

        maxCount = Math.min(20, getLevel() * 5);
    }

    @Override
    public double getPeriod() {
        return 5;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 5 == 0) {
            spawnSilverfish();
        }
    }

    private void spawnSilverfish() {
        if (count > maxCount) return;
        count++;

        Pair<Block, BlockFace> pair = getRandomBlockRelative();
        if (pair == null) return;
        Location location = pair.key().getLocation().add(0.5, 0.5, 0.5);

        net.minecraft.world.entity.monster.Silverfish silverfish = new net.minecraft.world.entity.monster.Silverfish(EntityType.SILVERFISH, ((CraftWorld) location.getWorld()).getHandle()) {
            @Override
            protected void registerGoals() {
                this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1, 10));
            }

            @Override
            public void tick() {
                if (tickCount % 20 == 0) {
                    dropItem(getBukkitEntity().getLocation(), 1);
                }
                if (tickCount > 80) {
                    discard();
                }

                super.tick();
            }
        };
        silverfish.moveTo(location.getX(), location.getY(), location.getZ(), Game.getRandom().nextInt(360), 0);
        silverfish.setInvulnerable(true);
        addEntity(silverfish);
    }
}
