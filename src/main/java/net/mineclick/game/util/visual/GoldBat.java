package net.mineclick.game.util.visual;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.*;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.global.util.*;
import net.mineclick.global.util.location.RandomVector;
import net.mineclick.global.util.location.Region;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.Vec3;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;

public class GoldBat {
    public static Bat spawn(GamePlayer player) {
        if (player.isOffline()) return null;
        Location spawn = player.getPlayer().getLocation().add(0, 1, 0);
        Region region = new Region(spawn.clone().add(-5, -1, -5), spawn.clone().add(5, 1, 5));
        List<Block> blocks = region.getBlocks(Material.AIR);
        if (!blocks.isEmpty()) {
            spawn = blocks.get(Game.getRandom().nextInt(blocks.size()))
                    .getLocation().add(0.5, 0.5, 0.5);
        }

        Bat entityBat = new Bat(EntityType.BAT, ((CraftWorld) spawn.getWorld()).getHandle()) {
            Vector moveTowards = null;

            private void reset() {
                discard();
                player.setArrows(0);
                player.updateInventory();
            }

            @Override
            public void tick() {
                Location loc = getBukkitEntity().getLocation();
                if (player.isOffline() || tickCount > 2400
                        || tickCount % 20 == 0 && player.getPlayer().getLocation().distanceSquared(loc) > 2500) {
                    reset();
                    return;
                }

                if (tickCount % 2 == 0) {
                    ParticlesUtil.sendColor(loc.clone().add(0, 0.5, 0).add(new RandomVector()), java.awt.Color.ORANGE, player);
                }

                if (tickCount % 40 == 0 && Game.getRandom().nextInt(10) == 0) {
                    player.playSound(Sound.ENTITY_BAT_AMBIENT, loc, 1, 1);
                }

                super.tick();

                // move towards the player
                if (tickCount % 20 == 0) {
                    if (player.getPlayer().getLocation().distanceSquared(loc) > 225) {
                        moveTowards = player.getPlayer().getLocation().toVector().subtract(getBukkitEntity().getLocation().toVector())
                                .normalize().multiply(0.25);
                    } else {
                        moveTowards = null;
                    }
                }
                if (moveTowards != null) {
                    setDeltaMovement(moveTowards.getX(), moveTowards.getY(), moveTowards.getZ());
                }
            }

            @Override
            public void setDeltaMovement(Vec3 vec3d) {
                vec3d = new Vec3(vec3d.x * 0.9, vec3d.y * 0.9, vec3d.z * 0.9);

                super.setDeltaMovement(vec3d);
            }

            @Override
            public boolean hurt(DamageSource damagesource, float f) {
                if (damagesource.getEntity() != null && damagesource.getEntity().getUUID().equals(player.getUuid())) {
                    Location loc = getBukkitEntity().getLocation();

                    AchievementsService.i().incrementProgress(player, "bats", 1);
                    QuestsService.i().incrementProgress(player, "dailyBats", 0, 1);

                    discard();
                    player.popSound();
                    ParticlesUtil.send(ParticleTypes.CLOUD, loc, Triple.of(0.1f, 0.1f, 0.1f), 20, player);

                    double percent = Game.getRandom().nextDouble();
                    int count = (int) Math.max(1, Math.ceil(percent * 5));
                    BigNumber min = new BigNumber(1);
                    BigNumber gold = player.getTotalWorkersIncome();
                    if (gold.smallerThan(min)) {
                        gold = min;
                    }

                    if (SkillsService.i().has(player, SkillType.BAT_2)) {
                        gold = gold.multiply(new BigNumber(1.5));
                    }

                    // 5 to 30 minutes of income
                    double minutes = 5 + percent * 25;
                    BigNumber goldEach = gold.multiply(new BigNumber(minutes * 60 / count));
                    for (int i = 0; i < count; i++) {
                        dropItem(loc, player, location -> {
                            player.addGold(goldEach);
                            player.expSound();
                            HologramsService.i().spawnBlockBreak(location, goldEach, false, Collections.singleton(player));
                        });
                    }

                    // 10 to 20 schmepls with 25% chance
                    long schmepls = Game.getRandom().nextDouble() < 0.25 ? (long) (10 + percent * 10) : 0;
                    if (schmepls > 0) {
                        dropItem(loc, player, location -> {
                            player.addSchmepls(schmepls);
                            player.expSound();
                            HologramsService.i().spawnFloatingUp(location, p -> ChatColor.GREEN + "+" + ChatColor.YELLOW + schmepls + ChatColor.AQUA + " schmepls", Collections.singleton(player));
                        });
                    }

                    // 1 geode with 5% chance
                    if (Game.getRandom().nextDouble() < 0.05) {
                        dropItem(loc, player, location -> {
                            Rarity rarity = GeodesService.i().addGeode(player);
                            player.expSound();
                            HologramsService.i().spawnFloatingUp(location, p -> ChatColor.GREEN + "+" + ChatColor.YELLOW + "1 " + rarity.getGeodeName() + ChatColor.AQUA + " geode", Collections.singleton(player));
                        });
                    }
                }
                return false;
            }
        };
        entityBat.moveTo(spawn.getX(), spawn.getY(), spawn.getZ());
        entityBat.setSilent(true);

        player.getAllowedEntities().add(entityBat.getId());
        entityBat.level().addFreshEntity(entityBat, CreatureSpawnEvent.SpawnReason.CUSTOM);

        Location soundLoc = spawn;
        Runner.sync(0, 5, state -> {
            if (state.getTicks() == 2) return;
            if (state.getTicks() > 3) {
                state.cancel();
                return;
            }
            player.playSound(Sound.ENTITY_BAT_AMBIENT, soundLoc, 1, 1);
        });

        return entityBat;
    }

    private static void dropItem(Location loc, GamePlayer player, Consumer<Location> consumer) {
        DroppedItem.spawn(ItemBuilder.builder().material(Material.CHEST).title(Game.getRandom().nextInt(9999) + "").build().toItem(), loc, 200, Collections.singleton(player), (location, p) -> {
            consumer.accept(location);
            return true;
        }, (entityItem) -> {
            if (entityItem.getY() < 10 && !player.isOffline()) {
                Location location = player.getPlayer().getLocation().add(new RandomVector(true).setY(0.2));
                entityItem.moveTo(location.getX(), location.getY(), location.getZ());
            }
        });
    }
}
