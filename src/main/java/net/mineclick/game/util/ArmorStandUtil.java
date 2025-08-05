package net.mineclick.game.util;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.Triple;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Builder
public class ArmorStandUtil {
    @Getter
    private final List<ArmorStand> stands = new ArrayList<>();
    private final Location location;
    private final Vector velocity;
    private final boolean global;
    private final boolean hasGravity;
    private final boolean small;
    private final boolean visible;
    private final boolean arms;
    private final Set<GamePlayer> viewers;
    private final ItemStack head;
    private final ItemStack leftHand;
    private final ItemStack rightHand;
    private final ItemStack chest;
    private final ItemStack legs;
    private final String displayName;
    @Singular
    private final Map<Part, Triple<Double, Double, Double>> rotations;
    private final BiConsumer<ArmorStand, Block> onBlockCollision;
    private final BiConsumer<ArmorStand, GamePlayer> onPlayerCollision;
    private final BiConsumer<ArmorStand, GamePlayer> onPlayerClick;
    @Singular
    private final List<Consumer<ArmorStand>> tickConsumers;
    @Builder.Default
    private boolean marker = true;

    public static void rotate(ArmorStand stand, Part part, double deltaX, double deltaY, double deltaZ, boolean add) {
        Rotations v = add ? part.getVector(stand) : new Rotations(0, 0, 0);
        switch (part) {
            case HEAD ->
                    stand.setHeadPose(new Rotations(v.getX() + ((float) deltaX), v.getY() + ((float) deltaY), (v.getZ() + ((float) deltaZ))));
            case LEFT_ARM ->
                    stand.setLeftArmPose(new Rotations(v.getX() + ((float) deltaX), v.getY() + ((float) deltaY), (v.getZ() + ((float) deltaZ))));
            case RIGHT_ARM ->
                    stand.setRightArmPose(new Rotations(v.getX() + ((float) deltaX), v.getY() + ((float) deltaY), (v.getZ() + ((float) deltaZ))));
            case BODY ->
                    stand.setBodyPose(new Rotations(v.getX() + ((float) deltaX), v.getY() + ((float) deltaY), (v.getZ() + ((float) deltaZ))));
            case LEFT_LEG ->
                    stand.setLeftLegPose(new Rotations(v.getX() + ((float) deltaX), v.getY() + ((float) deltaY), (v.getZ() + ((float) deltaZ))));
            case RIGHT_LEG ->
                    stand.setRightLegPose(new Rotations(v.getX() + ((float) deltaX), v.getY() + ((float) deltaY), (v.getZ() + ((float) deltaZ))));
        }
    }

    public ArmorStand spawn() {
        Preconditions.checkNotNull(location);
        Preconditions.checkNotNull(location.getWorld());

        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, ((CraftWorld) location.getWorld()).getHandle()) {
            @Override
            public void baseTick() {
                super.baseTick();
                tickConsumers.forEach(c -> c.accept(this));

                if (hasGravity && velocity != null) {
                    velocity.setY(velocity.getY() - 0.02);
                }
                if (velocity != null) {
                    moveTo(getX() + velocity.getX(), getY() + velocity.getY(), getZ() + velocity.getZ());
                }

                Location loc = getBukkitEntity().getLocation().add(0.25, isBaby() ? 0.5 : 1.76, 0.25);
                if (onBlockCollision != null) {
                    Block block = loc.getBlock();
                    if (!block.getType().equals(Material.AIR)) {
                        onBlockCollision.accept(this, block);
                        return;
                    }
                }

                if (onPlayerCollision != null) {
                    AABB box = new AABB(loc.getX() - 0.25, loc.getY() - 0.25, loc.getZ() - 0.25, loc.getX() + 0.25, loc.getY() + 0.25, loc.getZ() + 0.25);
                    List<Entity> list = level().getEntities(this, box, entity -> entity instanceof ServerPlayer);
                    if (!list.isEmpty()) {
                        PlayersService.i().<GamePlayer>get(list.get(0).getUUID(), player -> onPlayerCollision.accept(this, player));
                    }
                }
            }

            @Override
            public InteractionResult interactAt(Player entityhuman, Vec3 vec3d, InteractionHand enumhand) {
                if (onPlayerClick != null) {
                    PlayersService.i().<GamePlayer>get(entityhuman.getUUID(), playerModel -> onPlayerClick.accept(this, playerModel));
                }
                return InteractionResult.PASS;
            }
        };
        stands.add(stand);

        stand.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        stand.setInvisible(!visible);
        stand.setMarker(marker);
        stand.setSilent(true);
        stand.setShowArms(arms);
        stand.setSmall(small);
        stand.setNoGravity(!hasGravity);

        if (displayName != null) {
            stand.setCustomNameVisible(true);
            stand.setCustomName(CraftChatMessage.fromStringOrNull(displayName));
        }

        for (Map.Entry<Part, Triple<Double, Double, Double>> entry : rotations.entrySet()) {
            rotate(stand, entry.getKey(), entry.getValue().first(), entry.getValue().second(), entry.getValue().third(), false);
        }

        if (head != null) {
            stand.setItemSlot(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(head));
        }
        if (leftHand != null) {
            stand.setItemSlot(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(leftHand));
        }
        if (rightHand != null) {
            stand.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(rightHand));
        }
        if (chest != null) {
            stand.setItemSlot(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(chest));
        }
        if (legs != null) {
            stand.setItemSlot(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(legs));
        }

        if (global) {
            EntityPolice.getGloballyExcluded().add(stand.getId());
        } else {
            for (GamePlayer viewer : viewers) {
                viewer.getAllowedEntities().add(stand.getId());
            }
        }
        stand.level().addFreshEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);

        return stand;
    }

    public void rotate(Part part, double deltaX, double deltaY, double deltaZ, boolean add) {
        for (ArmorStand stand : stands) {
            rotate(stand, part, deltaX, deltaY, deltaZ, add);
        }
    }

    public void move(Location location, boolean rotate) {
        for (ArmorStand stand : stands) {
            if (rotate) {
                stand.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            } else {
                stand.moveTo(location.getX(), location.getY(), location.getZ());
            }
        }
    }

    public void removeAll() {
        stands.forEach(Entity::kill);
        stands.clear();
    }

    public boolean allRemoved() {
        return stands.isEmpty() || stands.stream().noneMatch(Entity::isAlive);
    }

    public enum Part {
        HEAD,
        LEFT_ARM,
        RIGHT_ARM,
        BODY,
        LEFT_LEG,
        RIGHT_LEG;


        public Rotations getVector(ArmorStand stand) {
            return switch (this) {
                case HEAD -> stand.headPose;
                case LEFT_ARM -> stand.leftArmPose;
                case RIGHT_ARM -> stand.rightArmPose;
                case BODY -> stand.bodyPose;
                case LEFT_LEG -> stand.leftLegPose;
                case RIGHT_LEG -> stand.rightLegPose;
            };

        }
    }

    public enum Axis {
        X,
        Y,
        Z;

        public double getFromVector(Rotations vector3f) {
            return switch (this) {
                case X -> vector3f.getX();
                case Y -> vector3f.getY();
                case Z -> vector3f.getZ();
            };

        }
    }
}
