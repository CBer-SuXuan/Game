package net.mineclick.game.gadget.secret;

import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.DynamicMineBlocksService;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.Triple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.vehicle.Minecart;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.awt.*;

public class GrapplingHook extends Gadget {
    @Override
    public boolean canRun(GamePlayer player, Action action) {
        return action.name().contains("RIGHT");
    }

    @Override
    public void run(GamePlayer player, Action action) {
        Player p = player.getPlayer();
        Location loc = p.getEyeLocation();
        Vector dir = p.getLocation().getDirection().normalize().multiply(0.1);
        Location location = null;
        for (int i = 0; i < 150; i++) {
            loc.add(dir);
            if (i < 10) {
                continue;
            }

            ParticlesUtil.send(ParticleTypes.CRIT, loc, Triple.of(0F, 0F, 0F), 1, player);
            Block b = loc.getBlock();
            if (b.getType().isSolid() || DynamicMineBlocksService.i().contains(player, b)) {
                Vector back = dir.clone().normalize().multiply(-1.5);
                location = loc.add(back);
                player.playSound(Sound.ENTITY_LEASH_KNOT_PLACE);
                break;
            }
        }

        if (location == null) {
            Vector perpendicular = VectorUtil.getPerpendicularTo(dir.normalize(), true).multiply(0.1);
            for (int i = 0; i < 4; i++) {
                Location l = loc.clone();
                for (int j = 0; j < 10; j++) {
                    ParticlesUtil.sendColor(l.add(perpendicular), Color.RED, player);
                }
                perpendicular = VectorUtil.rotateOnVector(dir, perpendicular.normalize(), Math.PI / 2).multiply(0.1);
            }
            player.playSound(Sound.ENTITY_LEASH_KNOT_BREAK);
        } else {
            spawnLeashKnot(location, player);
        }
    }

    private void spawnLeashKnot(Location location, GamePlayer player) {
        Player bukkitPlayer = player.getPlayer();
        Location pLoc = bukkitPlayer.getLocation();

        Entity anchor = spawnEntity(location, false);
        Entity vehicle = spawnEntity(pLoc, true);

        player.sendPacket(new ClientboundSetEntityLinkPacket(anchor, ((CraftPlayer) bukkitPlayer).getHandle()));
        vehicle.getBukkitEntity().addPassenger(bukkitPlayer);

        Vector vector = location.toVector().subtract(pLoc.toVector()).normalize().multiply(0.4);
        Runner.sync(0, 1, state -> {
            if (vehicle.getBukkitEntity().getLocation().distanceSquared(location) < 1 || vehicle.getPassengers().isEmpty()) {
                player.playSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1, 0.5);
                state.cancel();

                player.sendPacket(new ClientboundSetEntityLinkPacket(anchor, null));
                anchor.discard();
                return;
            }
            if (state.getTicks() % 2 == 0) {
                player.playSound(Sound.BLOCK_NOTE_BLOCK_HAT, 0.1, 0.1);
            }
            vehicle.moveTo(vehicle.getX() + vector.getX(), vehicle.getY() + vector.getY(), vehicle.getZ() + vector.getZ());
        });
    }

    private Entity spawnEntity(Location location, boolean cart) {
        Entity entity;

        if (cart) {
            entity = new Minecart(EntityType.MINECART, ((CraftWorld) location.getWorld()).getHandle()) {
                @Override
                public void tick() {
                    if (tickCount > 5 && passengers.isEmpty()) {
                        discard();
                    }

                    if (tickCount % 5 == 0) {
                        ParticlesUtil.send(ParticleTypes.FIREWORK, getBukkitEntity().getLocation(), Triple.of(0.3F, 0.1F, 0.3F), 4, getPlayersInLobby());
                    }
                }

                @Override
                public InteractionResult interact(net.minecraft.world.entity.player.Player var0, InteractionHand var1) {
                    return InteractionResult.PASS;
                }

                @Override
                public void remove(RemovalReason removalReason) {
                    super.remove(removalReason);

                    EntityPolice.getGloballyExcluded().remove(getId());
                }
            };
        } else {
            entity = new Bat(EntityType.BAT, ((CraftWorld) location.getWorld()).getHandle()) {
                @Override
                public void tick() {
                }

                @Override
                protected void customServerAiStep() {
                }

                @Override
                public void remove(RemovalReason removalReason) {
                    super.remove(removalReason);

                    EntityPolice.getGloballyExcluded().remove(getId());
                }
            };
        }

        EntityPolice.getGloballyExcluded().add(entity.getId());
        entity.setInvulnerable(true);
        entity.setNoGravity(true);
        entity.noPhysics = true;
        entity.setInvisible(true);
        entity.persistentInvisibility = true;
        entity.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), 0);
        entity.level().addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);

        return entity;
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().material(Material.LEAD);
    }

    @Override
    public String getImmutableName() {
        return "grapplingHook";
    }

    @Override
    public String getName() {
        return "Grappling hook";
    }

    @Override
    public String getDescription() {
        return "Helps you get around the lobby";
    }

    @Override
    public int getCooldown() {
        return 3;
    }

    @Override
    public boolean isSecret() {
        return true;
    }
}
