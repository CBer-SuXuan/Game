package net.mineclick.game.util;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.BoostersService;
import net.mineclick.game.service.HologramsService;
import net.mineclick.game.service.LevelsService;
import net.mineclick.game.type.BoosterType;
import net.mineclick.game.util.visual.PacketHologram;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import net.minecraft.core.Rotations;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.Collections;
import java.util.function.Consumer;

public class Vault {
    public static void spawn(GamePlayer player, Location location, long time) {
        if (player.isOffline())
            return;

        BigNumber goldRate = player.getGoldRate();
        if (BoostersService.i().isActive(BoosterType.GOLD_BOOSTER)) {
            goldRate = new BigNumber(goldRate.divide(new BigNumber(BoostersService.i().getActiveBoost(BoosterType.GOLD_BOOSTER))));
        }

        if (goldRate.compareTo(BigNumber.ZERO) > 0) {
            Location loc = location.add(0, -1, 0);
            loc.add(loc.getDirection().setY(0).normalize().multiply(5));
            loc.setDirection(loc.getDirection().multiply(-1));
            Location particleLoc = loc.clone().add(0, 1.1, 0);

            long validTime = (long) Math.min(time, player.getVaults() * (3.6e+6));
            int vaults = (int) Math.ceil(validTime / 3.6e+6);
            BigNumber goldMade = new BigNumber(goldRate.multiply(new BigNumber(validTime * 0.02)));
            goldMade.setNoZeroes(true);
            goldMade.setMainColor(ChatColor.BOLD);
            player.setUncollectedVaultsGold(goldMade);
            PacketHologram hologram = HologramsService.i().spawn(loc.clone().add(0, 2.5, 0), p -> ChatColor.GREEN + "Click to collect your Vaults " + ChatColor.GRAY + "(" + vaults + "/" + player.getVaults() + ")", Collections.singleton(player), true);

            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, ((CraftWorld) player.getPlayer().getWorld()).getHandle()) {
                private boolean collected;

                @Override
                public InteractionResult interactAt(Player entityhuman, Vec3 vec3d, InteractionHand enumhand) {
                    if (!collected && player.getUuid().equals(entityhuman.getUUID())) {
                        if (player.getUncollectedVaultsGold() != null) {
                            player.addGold(player.getUncollectedVaultsGold());
                            player.setUncollectedVaultsGold(null);

                            if (!player.getTutorial().isVaultsComplete()) {
                                player.getTutorial().setVaultsComplete(true);

                                TutorialVillager.msg(player, () -> {
                                            player.sendMessage(ChatColor.GREEN + "+" + ChatColor.AQUA + "200 EXP");
                                            player.addExp(200);
                                            player.expSound();
                                        },
                                        Pair.of("Hey it's me again!", 0),
                                        Pair.of("This thing that you just picked up is called a vault", 40),
                                        Pair.of("Each vault holds up to an hour worth of gold while you're away or afk", 100),
                                        Pair.of("So if you have " + player.getVaults() + " vaults, this means that you can be offline for " + player.getVaults() + " hours while your gold is still being collected", 100),
                                        Pair.of("To get more vaults you need to collect EXP - for every 5 levels you get an extra vault", 160),
                                        Pair.of("Also, with a Premium Membership you can get double the vaults and setup a Discord notification that lets you know when your vaults are full", 120),
                                        Pair.of("Here's a bit of EXP to help you out. See you!", 160)
                                );
                            }
                        }

                        setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        HologramsService.i().remove(hologram);

                        int nextLevel = (LevelsService.i().getLevel(player.getExp()) / 5) * 5 + 5;
                        setCustomName(CraftChatMessage.fromStringOrNull(ChatColor.GOLD + "Extra vault at " + ChatColor.GREEN + "level " + nextLevel));
                        Runner.sync(400, this::kill);

                        collected = true;
                    }

                    return InteractionResult.PASS;
                }
            };
            stand.setCustomNameVisible(true);
            stand.setCustomName(CraftChatMessage.fromStringOrNull(ChatColor.YELLOW + "You've made " + goldMade.print(player) + ChatColor.RESET + ChatColor.YELLOW + " gold while being away"));
            stand.setItemSlot(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.ENDER_CHEST)));
            stand.setNoGravity(true);
            stand.setInvisible(true);
            stand.moveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            player.getAllowedEntities().add(stand.getId());
            stand.level().addFreshEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);

            Runner.sync(0, 1, new Consumer<>() {
                int headDir = 0;
                double particleRadius = 0.1;
                double particleDir = 1;

                @Override
                public void accept(Runner.State state) {
                    if (player.isOffline() || !stand.isAlive()) {
                        state.cancel();
                        return;
                    }

                    if (player.getPlayer().getLocation().distanceSquared(loc) > (500 * 500)) {
                        if (player.getUncollectedVaultsGold() != null) {
                            player.addGold(player.getUncollectedVaultsGold());
                            player.setUncollectedVaultsGold(null);
                        }

                        stand.discard();
                        HologramsService.i().remove(hologram);
                        return;
                    }

                    Vector v = player.getPlayer().getLocation().toVector().subtract(loc.toVector()).setY(0);
                    double t = Math.atan2(-v.getX(), v.getZ());
                    stand.setYRot((float) Math.toDegrees((t + 6.283185307179586D) % 6.283185307179586D));

                    if (Game.getRandom().nextInt(50) == 0 && stand.headPose.getZ() == 0) {
                        headDir = 4;
                    }

                    float z = stand.headPose.getZ();
                    if (z == 16) {
                        headDir = -4;
                    } else if (z == -16) {
                        headDir = 4;
                    }
                    stand.setHeadPose(new Rotations(0, 0, stand.headPose.getZ() + headDir));
                    if (stand.headPose.getZ() == 0 && headDir == 4) {
                        headDir = 0;
                    }

                    //Particles
                    if (stand.isAlive()) {
                        particleRadius += 0.05 * particleDir;
                        if (particleRadius >= 1.25) {
                            particleDir = -1;
                        } else if (particleRadius <= 0.25) {
                            particleDir = 1;
                        }

                        double step = 1 / (Math.PI * particleRadius);
                        for (double theta = 0; theta < Math.PI * 2; theta += step) {
                            ParticlesUtil.sendColor(particleLoc.clone().add(particleRadius * Math.cos(theta), 0, particleRadius * Math.sin(theta)), new Color(102, 1, 204), player);
                        }
                    }
                }
            });
        }
    }
}
