package net.mineclick.game.type.geode;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.GeodeCrusher;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.RandomCollection;
import net.mineclick.global.util.Triple;
import net.mineclick.global.util.location.RandomVector;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Legendary extends GeodeAnimation {
    private final Set<Entity> entities = new HashSet<>();
    private final Set<Block> changedBlocks = new HashSet<>();

    public Legendary(GeodeCrusher geodeCrusher, GamePlayer player) {
        super(geodeCrusher, player);

        init();
    }

    @Override
    public int getPeriod() {
        return 140;
    }

    private void init() {
        Location location = getGeodeCrusher().getBlockLocation().clone().add(0.5, 0, 0.5);

        // place blocks
        RandomCollection<Block> blocks = getBlocksAround(8);
        if (blocks.values().size() > 0) {
            for (int i = 0; i < blocks.values().size(); i++) {
                Block block = blocks.next();
                Location loc = block.getLocation();
                BlockData blockData = Bukkit.createBlockData(block.getType().isSolid() ? Material.END_STONE : Material.OBSIDIAN);

                getPlayers().forEach(player -> player.sendBlockChange(loc, blockData));
                changedBlocks.add(block);
            }
        }

        // end gateway
        Block endBlock = location.getBlock().getRelative(BlockFace.DOWN);
        changedBlocks.add(endBlock);
        getPlayers().forEach(player -> player.sendBlockChange(endBlock.getLocation(), Bukkit.createBlockData(Material.END_GATEWAY)));

        // stairs
        for (int i = 0; i < 10; i++) {
            if (i == 4 || i == 5) continue;

            BlockFace face = BlockFace.values()[i];
            Block block = endBlock.getRelative(face);
            BlockData blockData;
            if (block.getType().toString().endsWith("_STAIRS")) {
                Stairs state = (Stairs) block.getBlockData();
                blockData = Bukkit.createBlockData(Material.END_STONE_BRICK_STAIRS, "[facing=" + state.getFacing().toString().toLowerCase()
                        + ",half=" + state.getHalf().toString().toLowerCase()
                        + ",shape=" + state.getShape().toString().toLowerCase()
                        + "]");

            } else {
                blockData = Bukkit.createBlockData(Material.END_STONE_BRICKS);
            }
            changedBlocks.add(block);
            getPlayers().forEach(player -> player.sendBlockChange(block.getLocation(), blockData));
        }
    }

    private void spawnShulker() {
        Location location = getGeodeCrusher().getBlockLocation().clone().add(0.5, -2, 0.5);
        Shulker shulker = new Shulker(EntityType.SHULKER, ((CraftWorld) location.getWorld()).getHandle()) {
            @Override
            public void tick() {
                super.tick();

                if (tickCount == 2) {
                    setRawPeekAmount(100);
                }

                if (tickCount == 10 && !getPlayer().isOffline()) {
                    if (!Legendary.this.getGeodeStand().getStands().isEmpty()) {
                        ShulkerBullet bullet = new ShulkerBullet(level(), this, Legendary.this.getGeodeStand().getStands().get(0), getAttachFace().getAxis());
                        bullet.setSilent(true);
                        getPlayers().forEach(player -> player.getAllowedEntities().add(bullet.getId()));
                        level().addFreshEntity(bullet, CreatureSpawnEvent.SpawnReason.CUSTOM);
                        entities.add(bullet);

                        getPlayer().playSound(Sound.ENTITY_SHULKER_SHOOT, getBukkitEntity().getLocation(), 1, 1);
                    }
                }

                if (tickCount == 20) {
                    setRawPeekAmount(0);
                }

                if (tickCount == 40) {
                    discard();
                }
            }

            @Override
            protected void registerGoals() {
            }
        };
        shulker.setSilent(true);
        shulker.moveTo(location.getX(), location.getY(), location.getZ());

        getPlayers().forEach(player -> player.getAllowedEntities().add(shulker.getId()));
        shulker.level().addFreshEntity(shulker, CreatureSpawnEvent.SpawnReason.CUSTOM);
        entities.add(shulker);
    }

    private void spawnEndermite() {
        Location location = getGeodeCrusher().getBlockLocation().clone().add(0.5, 0, 0.5);
        location.add(new RandomVector().setY(0));
        Endermite endermite = new Endermite(EntityType.ENDERMITE, ((CraftWorld) location.getWorld()).getHandle()) {
            @Override
            public void tick() {
                if (tickCount >= 100) {
                    discard();
                    return;
                }
                super.tick();
            }
        };

        Vector direction = new RandomVector();
        direction.setY(Math.abs(direction.getY()));
        location.setDirection(direction);
        endermite.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        direction.multiply(0.3);
        endermite.setDeltaMovement(direction.getX(), direction.getY(), direction.getZ());
        endermite.setInvulnerable(true);
        endermite.setSilent(true);

        getPlayers().forEach(player -> player.getAllowedEntities().add(endermite.getId()));
        endermite.level().addFreshEntity(endermite, CreatureSpawnEvent.SpawnReason.CUSTOM);
        entities.add(endermite);

        if (Game.getRandom().nextBoolean()) {
            getPlayer().playSound(Sound.ENTITY_ENDERMITE_AMBIENT, location, 1, 1);
        }
    }

    @Override
    protected void onReset() {
        entities.forEach(Entity::kill);
        entities.clear();
        getPlayers().forEach(this::onPlayerRemove);
    }

    @Override
    protected void onPlayerRemove(GamePlayer player) {
        entities.forEach(entity -> {
            player.sendPacket(new ClientboundRemoveEntitiesPacket(entity.getId()));
            player.getAllowedEntities().remove(entity.getId());
        });
        changedBlocks.forEach(player::sendBlockChange);
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 5 == 0 && Game.getRandom().nextBoolean()) {
            spawnShulker();

            if (Game.getRandom().nextBoolean()) {
                spawnEndermite();
            }
        }

        if (ticks == 140) {
            Location location = getGeodeCrusher().getBlockLocation().clone().add(0.5, 0.25, 0.5);
            ParticlesUtil.send(ParticleTypes.DRAGON_BREATH, location, Triple.of(2F, 0.25F, 2F), 150, getPlayers());
            getPlayer().playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, location, 0.5, 2);
        }
    }
}
