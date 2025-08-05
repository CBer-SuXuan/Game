package net.mineclick.game.gadget.generic;

import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Runner;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class ChickenGun extends Gadget {
    @Override
    public void run(GamePlayer player, Action action) {
        boolean machineGun = action.name().contains("RIGHT");

        Player p = player.getPlayer();
        Set<Chicken> chickens = new HashSet<>();

        if (machineGun) {
            Runner.sync(0, 2, state -> {
                if (state.getTicks() > 15) {
                    state.cancel();
                    return;
                }

                Location l = p.getEyeLocation();
                l.add(l.getDirection());
                Vector v = l.getDirection().multiply(2);

                chickens.add(spawn(l, v));

                player.playSound(Sound.ENTITY_CHICKEN_EGG);
            });
        } else {
            Location l = p.getEyeLocation();
            for (int i = 0; i < 15; i++) {
                Vector dir = l.getDirection();
                dir.add(VectorUtil.getPerpendicularTo(dir, true).multiply(Game.getRandom().nextDouble() * 0.2)).normalize();
                chickens.add(spawn(l.clone().add(dir), dir.multiply(2)));

                player.playSound(Sound.ENTITY_CHICKEN_EGG);
            }
        }

        Runner.sync(200, () -> chickens.forEach(c -> {
            EntityPolice.getGloballyExcluded().remove(c.getId());
            c.discard();
        }));
    }

    private Chicken spawn(Location l, Vector v) {
        Chicken chicken = new Chicken(EntityType.CHICKEN, ((CraftWorld) l.getWorld()).getHandle());
        chicken.moveTo(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
        chicken.getBukkitEntity().setVelocity(v);
        chicken.setInvulnerable(true);

        EntityPolice.getGloballyExcluded().add(chicken.getId());
        chicken.level().addFreshEntity(chicken, CreatureSpawnEvent.SpawnReason.CUSTOM);

        return chicken;
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().material(Material.FEATHER);
    }

    @Override
    public String getImmutableName() {
        return "chickenGun";
    }

    @Override
    public String getName() {
        return "Chicken gun";
    }

    @Override
    public String getDescription() {
        return "Chickens!";
    }

    @Override
    public int getCooldown() {
        return 20;
    }
}
