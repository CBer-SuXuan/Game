package net.mineclick.game.gadget.christmas;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AtomicDouble;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.packet.EntityPolice;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.*;
import java.util.function.Consumer;

public class Santa extends Gadget {
    public static final List<String> PRESENT_SKINS = ImmutableList.of(
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmNlZjlhYTE0ZTg4NDc3M2VhYzEzNGE0ZWU4OTcyMDYzZjQ2NmRlNjc4MzYzY2Y3YjFhMjFhODViNyJ9fX0="),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWI2NzMwZGU3ZTViOTQxZWZjNmU4Y2JhZjU3NTVmOTQyMWEyMGRlODcxNzU5NjgyY2Q4ODhjYzRhODEyODIifX19"),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDA4Y2U3ZGViYTU2YjcyNmE4MzJiNjExMTVjYTE2MzM2MTM1OWMzMDQzNGY3ZDVlM2MzZmFhNmZlNDA1MiJ9fX0="),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzU0MTlmY2U1MDZhNDk1MzQzYTFkMzY4YTcxZDIyNDEzZjA4YzZkNjdjYjk1MWQ2NTZjZDAzZjgwYjRkM2QzIn19fQ=="),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ5N2Y0ZjQ0ZTc5NmY3OWNhNDMwOTdmYWE3YjRmZTkxYzQ0NWM3NmU1YzI2YTVhZDc5NGY1ZTQ3OTgzNyJ9fX0="),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODRlMWM0MmYxMTM4M2I5ZGM4ZTY3ZjI4NDZmYTMxMWIxNjMyMGYyYzJlYzdlMTc1NTM4ZGJmZjFkZDk0YmI3In19fQ=="),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGFjYjNjMWUxYjM0Zjg3MzRhZWRmYWJkMWUxZjVlMGIyODBiZWY5MjRmYjhiYmYzZTY5MmQyNTM4MjY2ZjQifX19"),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDdlNTVmY2M4MDlhMmFjMTg2MWRhMmE2N2Y3ZjMxYmQ3MjM3ODg3ZDE2MmVjYTFlZGE1MjZhNzUxMmE2NDkxMCJ9fX0="),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWM2Mjc0YzIyZDcyNmZjMTIwY2UyNTczNjAzMGNjOGFmMjM4YjQ0YmNiZjU2NjU1MjA3OTUzYzQxNDQyMmYifX19"),
            Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmYyZDE4OTVmZmY0YjFiYjkxMTZjOGE5ZTIyOTU5N2Y2OWYzZWVlODgxMjI3NzZlNWY5NzMzNTdlNmIifX19")
    );
    private static final String ITEM_SKULL = Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTRlNDI0YjE2NzZmZWVjM2EzZjhlYmFkZTllN2Q2YTZmNzFmNzc1NmE4NjlmMzZmN2RmMGZjMTgyZDQzNmUifX19");
    private static final int MIN_OFFSET = 3;
    private static final int MAX_OFFSET = 8;
    private static final int QUEUE_SIZE = 15;
    private static final Map<GamePlayer, Pair<Vector, AtomicDouble>> cached = new HashMap<>();

    static {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Game.i(), ListenerPriority.HIGH, PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PlayersService.i().<GamePlayer>get(event.getPlayer().getUniqueId(), player -> {
                    if (player != null && cached.containsKey(player)) {
                        float side = event.getPacket().getFloat().read(0);
                        float forward = event.getPacket().getFloat().read(1);
                        boolean jump = event.getPacket().getBooleans().read(0);

                        Pair<Vector, AtomicDouble> pair = cached.get(player);
                        Vector vector = pair.key();
                        if (side != 0) {
                            double length = vector.length();
                            double theta = Math.atan2(-vector.getX(), vector.getZ());
                            float yaw = (float) Math.toDegrees((theta + 6.283185307179586D) % 6.283185307179586D);
                            yaw -= 3 * Math.signum(side);
                            vector.setX(-Math.sin(Math.toRadians(yaw)));
                            vector.setZ(Math.cos(Math.toRadians(yaw)));
                            vector.multiply(length);
                        }

                        if (forward != 0) {
                            pair.value().addAndGet(Math.signum(forward) * 0.2);
                        }

                        if (jump && Game.getRandom().nextInt(5) == 0) {
                            ItemStack stack = ItemBuilder.builder()
                                    .title(Game.getRandom().nextInt(1000) + "")
                                    .skull(PRESENT_SKINS.get(Game.getRandom().nextInt(PRESENT_SKINS.size())))
                                    .build().toItem();
                            DroppedItem.spawn(stack, player.getPlayer().getLocation().add(0.5, 0.5, 0.5), 200, null, null);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void run(GamePlayer player, Action action) {
        CraftPlayer bukkitPlayer = (CraftPlayer) player.getPlayer();
        Location loc = bukkitPlayer.getLocation().add(0.3, -0.5, 0.3);
        Vector dir = loc.getDirection().setY(0).normalize().multiply(0.2);
        AtomicDouble y = new AtomicDouble(loc.getY());
        cached.put(player, Pair.of(dir, y));
        Level world = ((CraftWorld) bukkitPlayer.getWorld()).getHandle();

        Minecart cart = new Minecart(EntityType.MINECART, world);
        cart.moveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), 0);
        cart.setYHeadRot(loc.getYaw());
        cart.setNoGravity(true);
        cart.noPhysics = true;
        cart.setInvulnerable(true);

        EntityPolice.getGloballyExcluded().add(cart.getId());
        ((CraftWorld) loc.getWorld()).addEntity(cart, CreatureSpawnEvent.SpawnReason.CUSTOM);

        List<Pair<Horse, Horse>> horses = new ArrayList<>();
        for (double z = MAX_OFFSET; z >= MIN_OFFSET; z -= 2.5) {
            Horse left = null;
            for (double x = -1; x <= 1; x += 2) {
                Vector vector = new Vector(x, 0, z);
                double length = vector.length();
                double theta = Math.atan2(-vector.getX(), vector.getZ());
                float yaw = (float) Math.toDegrees((theta + 6.283185307179586D) % 6.283185307179586D);
                yaw += loc.getYaw();
                Vector offset = new Vector();
                offset.setX(-Math.sin(Math.toRadians(yaw)));
                offset.setZ(Math.cos(Math.toRadians(yaw)));
                offset.multiply(length);

                Horse horse = new Horse(EntityType.HORSE, world);
                horse.moveTo(loc.getX() + offset.getX(), loc.getY(), loc.getZ() + offset.getZ(), loc.getYaw(), loc.getPitch());
                horse.setYHeadRot(loc.getYaw());
                horse.setNoGravity(true);
                horse.noPhysics = true;
                horse.setInvulnerable(true);
                horse.setNoAi(true);
                horse.setSilent(true);
                horse.setAge(0);
                horse.ageLocked = true;
                horse.getBukkitEntity().setMetadata("santaHorse", new FixedMetadataValue(Game.i(), null));

                EntityPolice.getGloballyExcluded().add(horse.getId());
                ((CraftWorld) loc.getWorld()).addEntity(horse, CreatureSpawnEvent.SpawnReason.CUSTOM);
                horse.setVariantAndMarkings(Variant.CHESTNUT, Markings.WHITE_DOTS);

                if (left == null) {
                    left = horse;
                } else {
                    horses.add(Pair.of(left, horse));
                }
                ClientboundSetEntityLinkPacket packet = new ClientboundSetEntityLinkPacket(horse, bukkitPlayer.getHandle());
                getPlayersInLobby().forEach(p -> p.sendPacket(packet));
            }
        }

        int capacity = horses.size() * QUEUE_SIZE;
        LinkedList<Consumer<Location>> locationModifiers = new LinkedList<>();
        Vector clone = dir.clone();
        for (int i = 0; i < capacity; i++) {
            locationModifiers.add(l -> l.add(clone));
        }

        bukkitPlayer.getHandle().startRiding(cart);
        bukkitPlayer.playSound(loc, Sound.ENTITY_HORSE_AMBIENT, 1, 1);
        Runner.sync(0, 1, new Consumer<>() {
            double yBounce = 0;

            @Override
            public void accept(Runner.State state) {
                if (isPlayerUnavailable(player) || cart.passengers.isEmpty() || state.getTicks() > 600) {
                    state.cancel();
                    horses.forEach(pair -> {
                        EntityPolice.getGloballyExcluded().remove(pair.key().getId());
                        EntityPolice.getGloballyExcluded().remove(pair.value().getId());
                        pair.key().discard();
                        pair.value().discard();
                    });
                    horses.clear();
                    EntityPolice.getGloballyExcluded().remove(cart.getId());
                    cart.discard();
                    cached.remove(player);
                    locationModifiers.clear();
                    return;
                }

                yBounce += Math.PI / 40;
                if (yBounce >= 2 * Math.PI) {
                    yBounce = 0;
                }
                double yAdd = Math.sin(yBounce);

                locationModifiers.removeLast();
                Vector clone = dir.clone();
                double yy = y.get();
                locationModifiers.addFirst(location -> location.add(clone).setDirection(clone).setY(yy + yAdd));
                int index = 0;
                for (Pair<Horse, Horse> pair : horses) {
                    Location lLoc = pair.key().getBukkitEntity().getLocation();
                    Location rLoc = pair.value().getBukkitEntity().getLocation();
                    Consumer<Location> consumer = locationModifiers.get(Math.max(index - 1, 0));
                    consumer.accept(lLoc);
                    consumer.accept(rLoc);

                    pair.key().moveTo(lLoc.getX(), lLoc.getY(), lLoc.getZ(), lLoc.getYaw(), lLoc.getPitch());
                    pair.value().moveTo(rLoc.getX(), rLoc.getY(), rLoc.getZ(), rLoc.getYaw(), rLoc.getPitch());

                    index += QUEUE_SIZE;
                }

                Location cartLoc = cart.getBukkitEntity().getLocation();
                locationModifiers.getLast().accept(cartLoc);
                cart.moveTo(cartLoc.getX(), cartLoc.getY(), cartLoc.getZ(), cartLoc.getYaw(), 0);

                //Santa particles
                ParticlesUtil.send(ParticleTypes.FIREWORK, bukkitPlayer.getLocation().add(0.5, -1, 0.5), Triple.of(0.5f, 0.5f, 0.5f), 1, getPlayersInLobby());
            }
        });
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().skull(ITEM_SKULL);
    }

    @Override
    public String getImmutableName() {
        return "santa";
    }

    @Override
    public String getName() {
        return "Santa";
    }

    @Override
    public String getDescription() {
        return "Ride the Santa's sleigh";
    }

    @Override
    public int getCooldown() {
        return 60;
    }

    @EventHandler
    public void on(EntityMountEvent e) {
        if (e.getMount().hasMetadata("santaHorse")) {
            e.setCancelled(true);
        }
    }
}
