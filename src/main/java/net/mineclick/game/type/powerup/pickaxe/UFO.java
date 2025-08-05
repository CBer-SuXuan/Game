package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.mineclick.global.util.location.RandomVector;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class UFO extends Powerup {
    private final static Color SPIRAL_COLOR = new Color(0, 249, 255);
    private final static Color PARTICLE_COLOR = new Color(3, 0, 86);
    private final Set<ArmorStandUtil> stands = new HashSet<>();
    private final List<Pair<Location, AtomicInteger>> particles = new ArrayList<>();
    private final List<Pair<Double, Vector>> spirals = new ArrayList<>();
    private final Block block;

    public UFO(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);

        block = getRandomBlock();
    }

    @Override
    public double getPeriod() {
        return 7;
    }

    @Override
    public void tick(long ticks) {
        if (block == null) return;

        // particles
        if (ticks % 10 == 0) {
            Location location = block.getLocation().add(new RandomVector(true).multiply(Game.getRandom().nextDouble() * 1.5)).add(0.5, Game.getRandom().nextInt(2), 0.5);
            particles.add(Pair.of(location, new AtomicInteger(30)));
        }
        particles.removeIf(pair -> pair.value().decrementAndGet() <= 0);
        for (Pair<Location, AtomicInteger> pair : particles) {
            ParticlesUtil.sendColor(pair.key().add(0, 0.2, 0), PARTICLE_COLOR, getPlayers());
        }

        // spirals
        if (spirals.isEmpty()) {
            double angle = 0;
            for (double y = 0; y < 6; y += 0.4) {
                for (int i = 0; i < 4; i++) {
                    spirals.add(Pair.of(y, new Vector(1.5, 0, 0).rotateAroundY(Math.PI / 2D * i + angle)));
                }
                angle += Math.PI / 10;
            }
        }
        for (Pair<Double, Vector> pair : spirals) {
            pair.value().rotateAroundY(Math.PI / 150);
            ParticlesUtil.sendColor(block.getLocation().add(0.5, pair.key(), 0.5).add(pair.value()), SPIRAL_COLOR, getPlayers());
        }

        // saucer
        ParticlesUtil.send(ParticleTypes.CLOUD, block.getLocation().add(0.5, 7, 0.5), Triple.of(1.5F, 0.25F, 1.5F), 40, getPlayers());

        // floating blocks
        if (ticks % 5 == 0 && stands.size() < Math.min(getLevel() * 5, 20)) {
            Location loc = block.getLocation().add(0.5, 0.5, 0.5).add(new RandomVector(true).multiply(Game.getRandom().nextDouble() * 1.5));
            ArmorStandUtil armorStand = ArmorStandUtil.builder()
                    .viewers(getPlayers())
                    .location(loc)
                    .small(true)
                    .head(new ItemStack(getItemMaterial()))
                    .tickConsumer(stand -> {
                        if (stand.getY() - 5 >= loc.getY()) {
                            stand.discard();
                            ParticlesUtil.send(ParticleTypes.CRIT, stand.getBukkitEntity().getLocation().add(0, 1, 0), Triple.of(0.2F, 0.2F, 0.2F), 5, getPlayers());
                            return;
                        }
                        stand.moveTo(stand.getX(), stand.getY() + 0.1, stand.getZ());
                        stand.setHeadPose(new Rotations(0F, stand.headPose.getY() - 9F, 0F));
                    })
                    .build();

            armorStand.spawn();
            stands.add(armorStand);
        }

        stands.removeIf(ArmorStandUtil::allRemoved);
    }

    @Override
    public void onReset(boolean premature) {
        stands.forEach(ArmorStandUtil::removeAll);
    }
}
