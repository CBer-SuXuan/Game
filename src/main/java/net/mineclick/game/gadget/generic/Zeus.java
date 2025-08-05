package net.mineclick.game.gadget.generic;

import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Zeus extends Gadget {
    private static final Color COLOR = new Color(125, 249, 255);
    private static final double STEP = 0.3;
    private static final double MAX_DISTANCE = 20;
    private static final double VAR_MAX = 7;
    private static final double VAR_STOP = 0.5;
    private static final double VAR_DEC = 0.55;

    @Override
    public void run(GamePlayer player, Action action) {
        Player p = player.getPlayer();
        Block block = p.getTargetBlock(null, (int) MAX_DISTANCE);

        List<Vector> bolt = new ArrayList<>();
        List<Location> particles = new ArrayList<>();

        Location start = p.getEyeLocation();
        Vector direction = start.getDirection();
        start.add(direction);
        bolt.add(start.toVector());

        Location end = block.getLocation().add(0.5, 0.5, 0.5);
        double distance = Math.min(MAX_DISTANCE, start.distance(end)) / MAX_DISTANCE;
        recBolt(start.toVector(), end.toVector(), direction, bolt, distance * VAR_MAX);

        if (bolt.size() > 1) {
            Runner.sync(0, 1, new Consumer<Runner.State>() {
                Vector previous = bolt.get(0);

                @Override
                public void accept(Runner.State state) {
                    int i = (int) state.getTicks();
                    if (i < bolt.size()) {
                        Vector dir = bolt.get(i).clone().subtract(previous);
                        double length = dir.length();
                        dir.setX(dir.getX() / length * STEP);
                        dir.setY(dir.getY() / length * STEP);
                        dir.setZ(dir.getZ() / length * STEP);

                        Location loc = new Location(start.getWorld(), previous.getX(), previous.getY(), previous.getZ());
                        for (int j = 0; j < length / STEP; j++) {
                            particles.add(loc.add(dir).clone());
                        }

                        previous = bolt.get(i);
                    } else if (i > 60) {
                        state.cancel();
                    }

                    particles.forEach(l -> ParticlesUtil.sendColor(l, COLOR, getPlayersInLobby()));
                }
            });

            player.playSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
        }
    }

    private void recBolt(Vector start, Vector end, Vector dir, List<Vector> bolt, double variance) {
        if (variance > VAR_STOP) {
            Vector var = VectorUtil.getPerpendicularTo(dir, true)
                    .multiply(Game.getRandom().nextDouble() * variance * 2 - variance);
            Vector mid = start.getMidpoint(end).add(var);

            recBolt(start, mid, dir, bolt, variance * VAR_DEC);
            recBolt(mid, end, dir, bolt, variance * VAR_DEC);
        } else {
            bolt.add(end);
        }
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().material(Material.BLAZE_POWDER);
    }

    @Override
    public String getImmutableName() {
        return "zeus";
    }

    @Override
    public String getName() {
        return "Zeus";
    }

    @Override
    public String getDescription() {
        return "Strike lightning bolts";
    }

    @Override
    public int getCooldown() {
        return 10;
    }
}
