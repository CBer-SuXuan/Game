package net.mineclick.game.gadget.christmas;

import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.util.*;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ChristmasTree extends Gadget {
    private static final int RESOLUTION = 3;
    private static final Color GREEN = new Color(0x018001);
    private static final String SKIN = Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzNiYTlhOTQ4ZWYxMjVhYWZkYjZiYzkxYmU2YzgwNGQ3MWFlZTE2NGJhMjE5N2IwYjIyNWE4NDdmZmYwNDhjMCJ9fX0=");

    @Override
    public void run(GamePlayer player, Action action) {
        Location location = player.getPlayer().getLocation();
        Vector dir = new Vector(0, 1, 0);
        Vector rot = VectorUtil.getPerpendicularTo(dir, true);

        Set<Pair<Location, Color>> particles = new LinkedHashSet<>();
        Runner.sync(0, 1, new Consumer<Runner.State>() {
            double radius = 1.8;
            int finishTicks = 60;
            boolean finished = false;

            @Override
            public void accept(Runner.State state) {
                if (finished && finishTicks-- <= 0) {
                    state.cancel();
                }

                if (!finished) {
                    if (radius <= 0) {
                        finished = true;
                        return;
                    }

                    for (int i = 0; i < RESOLUTION; i++) {
                        radius -= 0.03 / RESOLUTION;
                        location.add(dir.clone().multiply(0.1 / RESOLUTION));
                        VectorUtil.rotateOnVector(dir, rot, Math.PI / 5 / RESOLUTION);

                        particles.add(Pair.of(location.clone().add(rot.clone().multiply(radius)), Game.getRandom().nextInt(10) == 0 ? getRandomColor() : GREEN));
                        particles.add(Pair.of(location.clone().add(rot.clone().multiply(-radius)), Game.getRandom().nextInt(10) == 0 ? getRandomColor() : GREEN));
                    }
                    if (state.getTicks() % 2 == 0) {
                        player.playSound(Sound.ENTITY_PLAYER_LEVELUP, 0.1, 2);
                    }
                }

                for (Pair<Location, Color> pair : particles) {
                    Color color = pair.value();
                    ParticlesUtil.sendColor(pair.key(), color.getRed(), color.getGreen(), color.getBlue(), getPlayersInLobby(pair.key()));
                }
            }
        });
    }

    private Color getRandomColor() {
        return java.awt.Color.getHSBColor(Game.getRandom().nextFloat(), 1, 1);
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().skull(SKIN);
    }

    @Override
    public String getImmutableName() {
        return "christmasTree";
    }

    @Override
    public String getName() {
        return "Christmas Tree";
    }

    @Override
    public String getDescription() {
        return "Create a Christmas tree";
    }

    @Override
    public int getCooldown() {
        return 30;
    }
}
