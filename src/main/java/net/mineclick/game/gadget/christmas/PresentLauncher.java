package net.mineclick.game.gadget.christmas;

import com.google.common.collect.EvictingQueue;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.ArmorStandUtil;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.function.Consumer;

public class PresentLauncher extends Gadget {
    @Override
    public void run(GamePlayer player, Action action) {
        Player bukkitPlayer = player.getPlayer();
        Vector dir = bukkitPlayer.getLocation().getDirection();
        Location location = bukkitPlayer.getEyeLocation().add(dir);

        ItemBuilder.ItemBuilderBuilder stack = ItemBuilder.builder()
                .skull(Santa.PRESENT_SKINS.get(Game.getRandom().nextInt(Santa.PRESENT_SKINS.size())));
        ArmorStandUtil standUtil = ArmorStandUtil.builder()
                .location(location.clone().add(0, -1.8, 0))
                .global(true)
                .head(stack.build().toItem())
                .onBlockCollision((stand, b) -> stand.discard())
                .onPlayerCollision((stand, p) -> {
                    if (!p.equals(player)) {
                        stand.discard();
                    }
                })
                .build();
        ArmorStand stand = standUtil.spawn();

        player.playSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH);

        Runner.sync(0, 1, new Consumer<>() {
            final EvictingQueue<Location> redParticles = EvictingQueue.create(10);
            final Vector vector = VectorUtil.getPerpendicularTo(dir, true).multiply(0.6);
            final Vector moveDir = dir.clone().multiply(0.8);

            @Override
            public void accept(Runner.State state) {
                Set<GamePlayer> players = getPlayersInLobby();
                if (!stand.isAlive() || PresentLauncher.this.isPlayerUnavailable(player) || state.getTicks() > 100) {
                    state.cancel();
                    EntityPolice.getGloballyExcluded().remove(stand.getId());

                    location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
                    ParticlesUtil.send(ParticleTypes.EXPLOSION, stand.getBukkitEntity().getLocation().add(0, 1.8, 0), Triple.of(0.2f, 0.2f, 0.2f), 3, players);
                    for (int i = 0; i < 5; i++) {
                        DroppedItem.spawn(stack.title(i + "").build().toItem(), location.clone().add(0.5, 0, 0.5), 60, players, null);
                    }

                    stand.discard();
                    return;
                }

                VectorUtil.rotateOnVector(dir, vector, Math.PI / 15);
                Location loc = location.add(moveDir).clone().add(0, -1.8, 0).add(vector);
                stand.moveTo(loc.getX(), loc.getY(), loc.getZ(), 0, 0);
                standUtil.rotate(ArmorStandUtil.Part.HEAD, 0, 15, 0, true);

                ParticlesUtil.send(ParticleTypes.CLOUD, loc, Triple.of(0f, 0f, 0f), 1, players);

                redParticles.add(loc.clone().add(vector));
                redParticles.add(loc.clone().add(vector.clone().multiply(-1)));
                for (Location l : redParticles) {
                    ParticlesUtil.sendColor(l, 255, 0, 0, players);
                }
            }
        });
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().skull(Santa.PRESENT_SKINS.get(0));
    }

    @Override
    public String getImmutableName() {
        return "presentLauncher";
    }

    @Override
    public String getName() {
        return "Present Launcher";
    }

    @Override
    public String getDescription() {
        return "Launch explosive Christmas presents";
    }

    @Override
    public int getCooldown() {
        return 5;
    }
}
