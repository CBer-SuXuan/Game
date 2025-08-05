package net.mineclick.game.gadget.christmas;

import com.google.common.collect.EvictingQueue;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

public class SnowBooster extends Gadget {
    @Override
    public void run(GamePlayer player, Action action) {
        Vector dir = new Vector(0, 1, 0);
        Vector rot = VectorUtil.getPerpendicularTo(dir, true);
        Player bukkitPlayer = player.getPlayer();

        EvictingQueue<Location> particlesQueue = EvictingQueue.create(30);
        Runner.sync(0, 1, new Consumer<Runner.State>() {
            double radiusTheta = 0;

            @Override
            public void accept(Runner.State state) {
                if (radiusTheta >= Math.PI) {
                    state.cancel();
                    return;
                }

                Location location = bukkitPlayer.getLocation();
                Vector velocity = bukkitPlayer.getVelocity();
                double y = velocity.getY() + 0.1;
                y = Math.min(y, 0.8);
                bukkitPlayer.setVelocity(velocity.setY(y));

                double radius = Math.sin(radiusTheta += Math.PI / 40);
                VectorUtil.rotateOnVector(dir, rot, Math.PI / 10);
                Vector toAdd = rot.clone().multiply(radius);
                particlesQueue.add(location.clone().add(toAdd));
                particlesQueue.add(location.clone().add(toAdd.multiply(-1)));
                for (Location loc : particlesQueue) {
                    ParticlesUtil.send(ParticleTypes.CLOUD, loc, Triple.of(0f, 0f, 0f), 1, getPlayersInLobby(loc));
                }

                if (state.getTicks() % 2 == 0) {
                    player.playSound(Sound.ENTITY_PLAYER_LEVELUP, 0.1, 2);
                }
            }
        });
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().material(Material.SNOW_BLOCK);
    }

    @Override
    public String getImmutableName() {
        return "snowBooster";
    }

    @Override
    public String getName() {
        return "Snow Booster";
    }

    @Override
    public String getDescription() {
        return "Boost yourself up\nwith snow power";
    }

    @Override
    public int getCooldown() {
        return 10;
    }
}
