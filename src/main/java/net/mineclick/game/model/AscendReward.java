package net.mineclick.game.model;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.christmas.MagicWand;
import net.mineclick.game.service.GeodesService;
import net.mineclick.game.service.HologramsService;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.Triple;
import net.mineclick.global.util.location.RandomVector;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Data
public class AscendReward {
    private final Type type;
    private final long amount;

    private transient boolean dropped;
    private transient ArmorStandUtil armorStandUtil;
    private transient AtomicReference<Runner.State> stateReference;

    public void apply(GamePlayer player) {
        if (armorStandUtil != null) {
            armorStandUtil.removeAll();
            armorStandUtil = null;
        }

        type.getApply().accept(player, amount);

        String name = type.getName();
        if (type.getPluralName() != null && amount > 1)
            name = type.getPluralName();

        player.sendMessage(
                "%sReceived %s%d %s%s from ascension".formatted(
                        ChatColor.YELLOW,
                        ChatColor.GREEN,
                        amount,
                        name,
                        ChatColor.YELLOW
                ));
    }

    public void spawn(GamePlayer player, Location location) {
        player.expSound();

        Location spawn = location.clone();

        if (stateReference == null) {
            stateReference = new AtomicReference<>();
        }
        if (stateReference.get() != null) {
            stateReference.get().cancel();
        }
        if (armorStandUtil != null) {
            armorStandUtil.removeAll();
        }
        armorStandUtil = ArmorStandUtil.builder()
                .head(new ItemStack(Material.CHEST))
                .small(true)
                .visible(false)
                .marker(false)
                .location(location)
                .viewers(Collections.singleton(player))
                .onPlayerClick((stand, pl) -> {
                    if (pl.equals(player) && armorStandUtil != null) {
                        Location loc = location.clone().add(0, 1, 0);
                        player.playSound(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5, 2);
                        for (int i = 0; i < 5; i++) {
                            DroppedItem.spawn(Material.GOLD_NUGGET, loc, 30, Collections.singleton(player));
                        }

                        apply(player);
                        player.getAscendRewards().remove(this);

                        HologramsService.i().spawnFloatingUp(
                                location,
                                p -> ChatColor.GREEN + "+" + ChatColor.YELLOW + amount + " " + ChatColor.AQUA + type.getName(),
                                Collections.singleton(player)
                        );
                    }
                })
                .tickConsumer(new Consumer<>() {
                    double lastY = 0;
                    double wave = 0;

                    @Override
                    public void accept(ArmorStand stand) {
                        if (lastY == location.getY()) {
                            wave += Math.PI / 40;

                            stand.setCustomNameVisible(true);
                            stand.setCustomName(CraftChatMessage.fromStringOrNull(ChatColor.GREEN + "Ascension Reward"));
                        } else {
                            lastY = location.getY();
                        }

                        stand.setHeadPose(new Rotations(0, stand.headPose.getY() + 5, 0));
                        stand.moveTo(location.getX(), location.getY() + Math.sin(wave) / 4, location.getZ());
                    }
                })
                .build();

        if (!dropped) {
            dropped = true;
            location.add(0, 5, 0);
        }

        List<MagicWand.Flare> flares = new ArrayList<>();
        Runner.sync(0, 1, state -> {
            stateReference.set(state);
            boolean landed = location.getY() == spawn.getY();
            if (!landed) {
                location.add(0, -0.2, 0);
                if (location.getY() < spawn.getY()) {
                    location.setY(spawn.getY());
                }
            }

            if (player.isOffline() || armorStandUtil == null || armorStandUtil.allRemoved()) {
                state.cancel();
                return;
            }

            if (!landed && Game.getRandom().nextInt(4) == 0 || Game.getRandom().nextInt(20) == 0) {
                Vector v = new RandomVector(0.2).setY(1);
                MagicWand.Flare flare = new MagicWand.Flare(location.clone().add(0, 1, 0), v, Color.getHSBColor(Game.getRandom().nextFloat() * 0.166F, 1, 1));
                flares.add(flare);
            }
            flares.forEach(flare -> {
                Location loc = flare.tick();
                ParticlesUtil.sendColor(loc, flare.getColor(), player);
            });
            flares.removeIf(MagicWand.Flare::isDone);

            if (Game.getRandom().nextInt(5) == 0) {
                ParticlesUtil.send(ParticleTypes.FIREWORK, location.clone().add(0, 1, 0), Triple.of(0.25F, 0.25F, 0.25F), 1, player);
            }
        });

        armorStandUtil.spawn();
    }

    @Getter
    public enum Type {
        EXP("exp", GamePlayer::addExp),
        SCHMEPLS("schmepls", GamePlayer::addSchmepls),
        GEODES("geode", "geodes", (player, number) -> {
            // give x amount of common geodes
            GeodesService.i().addGeode(player, Rarity.COMMON, Math.toIntExact(number));

            // add 1 or 2 extra random geodes
            int specialGeodes = player.isRankAtLeast(Rank.PAID) ? 2 : 1;
            for (int i = 0; i < specialGeodes; i++) {
                GeodesService.i().addGeode(player);
            }
        });

        private final String name;
        private final @Nullable String pluralName;
        private final BiConsumer<GamePlayer, Long> apply;

        Type(String name, BiConsumer<GamePlayer, Long> apply) {
            this.name = name;
            this.pluralName = null;
            this.apply = apply;
        }

        Type(String name, @NotNull String pluralName, BiConsumer<GamePlayer, Long> apply) {
            this.name = name;
            this.pluralName = pluralName;
            this.apply = apply;
        }
    }
}
