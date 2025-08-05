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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;

import java.util.Random;

public class Tnt extends Powerup {
    private final int maxTnt;
    private int tntCount = 0;

    public Tnt(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
        maxTnt = Math.min(getLevel(), 5);
    }

    @Override
    public double getPeriod() {
        return 5;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 10 == 0 && tntCount++ < maxTnt) {
            spawnTnt();
        }
    }

    private void spawnTnt() {
        Random random = Game.getRandom();
        Pair<Block, BlockFace> pair = getRandomBlockRelative();
        if (pair == null) return;
        Location spawnLocation = pair.key().getLocation();

        PrimedTnt tnt = new PrimedTnt(EntityType.TNT, ((CraftWorld) spawnLocation.getWorld()).getHandle()) {
            private int fuseTicks = 60;

            @Override
            public void tick() {
                if (!this.isNoGravity()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
                }

                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
                if (this.onGround) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
                }

                --this.fuseTicks;
                if (this.fuseTicks <= 0) {
                    Location loc = getBukkitEntity().getLocation();

                    discard();
                    getPlayer().playSound(Sound.ENTITY_GENERIC_EXPLODE);
                    ParticlesUtil.send(ParticleTypes.EXPLOSION_EMITTER, loc, Triple.of(0F, 0F, 0F), 0F, 1, getPlayers());

                    dropItem(loc, 5);
                }
            }
        };

        tnt.moveTo(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
        tnt.setDeltaMovement(new Vec3(random.nextDouble() * 0.5 - 0.25, 0.25, random.nextDouble() * 0.5 - 0.25));
        tnt.setFuse(60);
        addEntity(tnt);
    }
}
