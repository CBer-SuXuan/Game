package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;

public class Creeper extends Powerup {
    private final int maxCount;
    private int count = 0;

    public Creeper(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);

        maxCount = Math.min(getLevel() + 1, 5);
    }

    @Override
    public double getPeriod() {
        return 6;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 20 == 0 && count++ < maxCount) {
            Pair<Block, BlockFace> pair = getRandomBlockRelative();
            if (pair == null) return;

            Location location = pair.key().getLocation().add(0.5, 0.5, 0.5);

            net.minecraft.world.entity.monster.Creeper creeper = new net.minecraft.world.entity.monster.Creeper(EntityType.CREEPER, ((CraftWorld) location.getWorld()).getHandle()) {
                @Override
                public void tick() {
                    super.tick();

                    if (tickCount == 40) {
                        ignite();
                    }
                }

                @Override
                protected void registerGoals() {
                    this.goalSelector.addGoal(1, new FloatGoal(this));
                    this.goalSelector.addGoal(2, new SwellGoal(this));
                    this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1, 10));
                    this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
                }

                @Override
                public void explodeCreeper() {
                    Location loc = getBukkitEntity().getLocation();

                    discard();
                    getPlayer().playSound(Sound.ENTITY_GENERIC_EXPLODE);
                    ParticlesUtil.send(ParticleTypes.EXPLOSION_EMITTER, loc, Triple.of(0F, 0F, 0F), 0F, 1, getPlayers());

                    dropItem(loc, 5);
                }
            };
            creeper.moveTo(location.getX(), location.getY(), location.getZ(), Game.getRandom().nextInt(360), 0);
            creeper.setInvulnerable(true);

            addEntity(creeper);
        }
    }
}
