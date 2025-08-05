package net.mineclick.game.gadget.christmas;

import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.util.*;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CandyCane extends Gadget {
    private static final String SKIN = Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmIyMTYxN2QyNzU1YmMyMGY4ZjdlMzg4ZjQ5ZTQ4NTgyNzQ1ZmVjMTZiYjE0Yzc3NmY3MTE4Zjk4YzU1ZTgifX19");

    @Override
    public void run(GamePlayer player, Action action) {
        Location location = player.getPlayer().getLocation().add(player.getPlayer().getLocation().getDirection().setY(0));
        Vector dir = new Vector(0, 1, 0);
        Vector rot = VectorUtil.getPerpendicularTo(dir, true);

        Set<Pair<Location, Color>> particles = new LinkedHashSet<>();
        Runner.sync(0, 1, new Consumer<Runner.State>() {
            double angle = Math.PI / 2;
            int finishTicks = 60;
            boolean finished = false;

            @Override
            public void accept(Runner.State state) {
                if (finished && finishTicks-- <= 0) {
                    state.cancel();
                }

                if (!finished) {
                    if (angle > Math.PI * 3 / 2) {
                        finished = true;
                        return;
                    }

                    if (state.getTicks() > 40) {
                        angle += Math.PI / 60;
                        dir.setY(Math.sin(angle));
                        dir.setX(Math.cos(angle));
                    }
                    location.add(dir.clone().multiply(0.1));

                    VectorUtil.rotateOnVector(dir, rot, Math.PI / 5);

                    particles.add(Pair.of(location.clone().add(rot.clone().multiply(0.5)), Color.RED));
                    particles.add(Pair.of(location.clone().add(rot.clone().multiply(-0.5)), Color.WHITE));

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

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().skull(SKIN);
    }

    @Override
    public String getImmutableName() {
        return "candyCane";
    }

    @Override
    public String getName() {
        return "Candy Cane";
    }

    @Override
    public String getDescription() {
        return "Create a candy cane";
    }

    @Override
    public int getCooldown() {
        return 30;
    }
}
