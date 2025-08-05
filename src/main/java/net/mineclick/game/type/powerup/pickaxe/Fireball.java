package net.mineclick.game.type.powerup.pickaxe;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.powerup.Powerup;
import net.mineclick.game.type.powerup.PowerupType;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Skins;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Fireball extends Powerup {
    private final static String SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzM2ODdlMjVjNjMyYmNlOGFhNjFlMGQ2NGMyNGU2OTRjM2VlYTYyOWVhOTQ0ZjRjZjMwZGNmYjRmYmNlMDcxIn19fQ==";
    private final Set<ArmorStandUtil> fireballs = new HashSet<>();

    public Fireball(PowerupType powerupType, GamePlayer player) {
        super(powerupType, player);
    }

    @Override
    public double getPeriod() {
        return 6;
    }

    @Override
    public void onReset(boolean premature) {
        fireballs.forEach(ArmorStandUtil::removeAll);
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 10 == 0) {
            Pair<Location, Location> spawnAndTarget = getRandomSpawnAndTarget();
            if (spawnAndTarget == null) return;

            Location location = spawnAndTarget.key().add(0, 4, 0);
            Location target = spawnAndTarget.value().add(0.5, 0.5, 0.5);
            Vector dir = target.toVector()
                    .subtract(location.toVector())
                    .normalize()
                    .multiply(0.5);

            ArmorStandUtil build = ArmorStandUtil.builder()
                    .small(true)
                    .location(location.clone().add(0, -1, 0))
                    .viewers(getPlayers())
                    .head(Skins.set(new ItemStack(Material.PLAYER_HEAD), SKULL))
                    .tickConsumer(new Consumer<>() {
                        double distance = 99999;

                        @Override
                        public void accept(ArmorStand stand) {
                            stand.setYHeadRot(Location.normalizeYaw(stand.getYHeadRot() + 18));

                            location.add(dir);
                            double d = location.distanceSquared(target);
                            if (d < distance) {
                                distance = d;
                            } else {
                                location.add(0, -1, 0);
                                ParticlesUtil.send(ParticleTypes.EXPLOSION, location, Triple.of(0F, 0F, 0F), 2, getPlayers());
                                getPlayer().playSound(Sound.ENTITY_GENERIC_EXPLODE, location, 0.3, 1);

                                dropItem(location, 3);
                                stand.discard();
                                return;
                            }

                            if (Game.getRandom().nextInt(3) == 0) {
                                ParticlesUtil.send(ParticleTypes.LARGE_SMOKE, location, Triple.of(0.15F, 0.15F, 0.15F), 2, getPlayers());
                            }

                            stand.moveTo(location.getX(), location.getY() - 1, location.getZ());
                        }
                    })
                    .build();
            build.spawn();

            fireballs.add(build);
        }
    }
}
