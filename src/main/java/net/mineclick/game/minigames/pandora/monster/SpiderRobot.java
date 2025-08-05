package net.mineclick.game.minigames.pandora.monster;

import net.mineclick.game.ai.BodyPart;
import net.mineclick.game.ai.Robot;
import net.mineclick.game.ai.movables.LookAtPlayer;
import net.mineclick.game.ai.movables.Oscillate;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Triple;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.Set;

public class SpiderRobot extends Robot {
    private static final Color COLOR = Color.fromRGB(52, 52, 52);
    private static final String HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGVkYzFjNzc2ZDRhZWFmYjc1Y2I4YjkzOGFmODllMjA5MDJkODY4NGI3NDJjNmE4Y2M3Y2E5MjE5N2FiN2IifX19";

    public SpiderRobot(Location location, Set<GamePlayer> players) {
        super(location, players);
        setSpeed(0.1);

        addBodyParts(
                //Head
                new BodyPart(this, new Vector(0, -0.4, 0), 0, ArmorStandUtil.builder()
                        .head(ItemBuilder.builder().skull(HEAD).build().toItem())
                        .tickConsumer(new LookAtPlayer(this))
                ).rotateWithRobot(false),
                //Base
                new BodyPart(this, new Vector(-0.22, 0, 0), 90, ArmorStandUtil.builder()
                        .rotation(ArmorStandUtil.Part.LEFT_LEG, Triple.of(90D, 0D, 0D))
                        .rotation(ArmorStandUtil.Part.RIGHT_LEG, Triple.of(90D, 0D, 0D))
                        .legs(ItemBuilder.builder().color(COLOR).material(Material.LEATHER_LEGGINGS).build().toItem())
                ),
                new BodyPart(this, new Vector(0.22, 0, 0), -90, ArmorStandUtil.builder()
                        .rotation(ArmorStandUtil.Part.LEFT_LEG, Triple.of(90D, 0D, 0D))
                        .rotation(ArmorStandUtil.Part.RIGHT_LEG, Triple.of(90D, 0D, 0D))
                        .legs(ItemBuilder.builder().color(COLOR).material(Material.LEATHER_LEGGINGS).build().toItem())
                ),
                //Legs
                new BodyPart(this, new Vector(0.22, 0, 0), 90, ArmorStandUtil.builder()
                        .rotation(ArmorStandUtil.Part.LEFT_LEG, Triple.of(30D, 60D, 0D))
                        .rotation(ArmorStandUtil.Part.RIGHT_LEG, Triple.of(30D, 300D, 0D))
                        .legs(ItemBuilder.builder().color(COLOR).material(Material.LEATHER_LEGGINGS).build().toItem())
                        .tickConsumer(new Oscillate(this)
                                .dynamicSpeed(ArmorStandUtil.Part.LEFT_LEG, ArmorStandUtil.Axis.Y, 0, 60, true, () -> 5D)
                                .dynamicSpeed(ArmorStandUtil.Part.RIGHT_LEG, ArmorStandUtil.Axis.Y, 300, 360, true, () -> 5D)
                        )
                ),
                new BodyPart(this, new Vector(-0.22, 0, 0), -90, ArmorStandUtil.builder()
                        .rotation(ArmorStandUtil.Part.LEFT_LEG, Triple.of(30D, 60D, 0D))
                        .rotation(ArmorStandUtil.Part.RIGHT_LEG, Triple.of(30D, 300D, 0D))
                        .legs(ItemBuilder.builder().color(COLOR).material(Material.LEATHER_LEGGINGS).build().toItem())
                        .tickConsumer(new Oscillate(this)
                                .dynamicSpeed(ArmorStandUtil.Part.LEFT_LEG, ArmorStandUtil.Axis.Y, 0, 60, true, () -> 5D)
                                .dynamicSpeed(ArmorStandUtil.Part.RIGHT_LEG, ArmorStandUtil.Axis.Y, 300, 360, true, () -> 5D)
                        )
                ));
    }
}
