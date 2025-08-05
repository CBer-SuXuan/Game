package net.mineclick.game.util.visual;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.util.Vector;

import java.util.function.Function;

@Getter
public class Orb extends ArmorStand {
    private static final Vector UP = new Vector(0, 1, 0);
    private static final double Y = 1.5;
    private final GamePlayer player;
    private final Vector vector;
    @Setter
    private Function<Orb, Boolean> onShoot;
    @Setter
    private int spinTicks;
    @Setter
    private double rotationSpeed = 0;
    private boolean shot;
    private boolean stopSpinning;
    @Setter
    private Runnable particleTick;

    public Orb(GamePlayer player, ItemStack itemStack) {
        super(EntityType.ARMOR_STAND, ((CraftWorld) player.getPlayer().getWorld()).getHandle());
        this.player = player;
        vector = player.getPlayer().getLocation().getDirection().setY(0).normalize().multiply(2);

        setMarker(true);
        setInvisible(true);
        setInvulnerable(true);
        setNoGravity(true);
        setSmall(true);
        Location location = player.getPlayer().getLocation().add(vector.getX(), Y, vector.getZ());
        moveTo(location.getX(), location.getY(), location.getZ());

        setHeadPose(new Rotations(0, Game.getRandom().nextFloat() * 360, 0));
        setItemSlot(EquipmentSlot.HEAD, itemStack);
    }

    public Location getHeadLocation() {
        return getBukkitEntity().getLocation().add(0, 1, 0);
    }

    @Override
    public void baseTick() {
        if (player.isOffline()) {
            discard();
            return;
        }
        super.baseTick();

        if (!shot && spinTicks-- <= 0) {
            shot = true;
            stopSpinning = onShoot.apply(this);
        }
        if (stopSpinning) return;

        if (particleTick != null && tickCount % 2 == 0) {
            particleTick.run();
        }

        setHeadPose(new Rotations(0, headPose.getY() + 9, 0));
        vector.rotateAroundY(rotationSpeed);
        Location loc = player.getPlayer().getLocation().add(vector.getX(), Y, vector.getZ());
        moveTo(loc.getX(), loc.getY(), loc.getZ());
    }
}
