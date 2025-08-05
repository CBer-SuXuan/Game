package net.mineclick.game.util.visual;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.util.Runner;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;

public class FollowingItem extends ArmorStand {
    private final Entity following;
    private double yFloat = 0;

    public FollowingItem(Entity following, Material material, GamePlayer... players) {
        super(EntityType.ARMOR_STAND, following.level());
        this.following = following;

        moveTo(following.getX(), following.getY() + following.getBbHeight() + 1, following.getZ());
        setInvisible(true);
        setNoGravity(true);
        setMarker(true);
        setSmall(true);
        setSilent(true);
        setCustomNameVisible(false);

        setItemSlot(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(material)));

        for (GamePlayer player : players) {
            player.getAllowedEntities().add(getId());
        }

        Runner.sync(() -> level().addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM));
    }

    @Override
    public void baseTick() {
        if (!following.isAlive()) {
            discard();
            return;
        }
        super.baseTick();

        yFloat += Math.PI / 40;
        if (yFloat >= Math.PI * 2) yFloat = 0;
        setHeadPose(new Rotations(0, headPose.getY() + 3, 0));
        moveTo(following.getX(), following.getY() + following.getBbHeight() + Math.cos(yFloat) * 0.5, following.getZ());
    }
}
