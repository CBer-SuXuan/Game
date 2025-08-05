package net.mineclick.game.service;

import net.mineclick.game.Game;
import net.mineclick.game.model.DynamicMineBlock;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.Mineshaft;
import net.mineclick.game.type.DynamicMineBlockType;
import net.mineclick.global.config.MineshaftConfig;
import net.mineclick.global.config.field.specials.MineshaftField;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.MessageUtil;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.SingletonInit;
import net.mineclick.global.util.location.ConfigLocation;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

@SingletonInit
public class MineshaftService {
    private static MineshaftService i;

    private final List<BlockFace> blockFaces = Arrays.asList(BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.UP);
    private Location spawn;

    public static MineshaftService i() {
        return i == null ? i = new MineshaftService() : i;
    }

    public Location getSpawn() {
        if (spawn == null) {
            MineshaftField mineshaftField = MineshaftConfig.getMineshaft().getMineshaftField();
            if (mineshaftField == null || mineshaftField.getSpawn() == null) return null;
            spawn = mineshaftField.getSpawn().toLocation();
        }

        return spawn;
    }

    private void load(GamePlayer player) {
        MineshaftConfig config = MineshaftConfig.getMineshaft();
        if (config == null || config.getMineshaftField() == null) {
            Game.i().getLogger().severe("Mineshaft could not be loaded");
            return;
        }
        Set<GamePlayer> players = getPlayers(player);

        // Load config
        Material blockMaterial = getBlockMaterial(player);
        int clicksPerBlock = player.getDimensionsData().getDimension().getMineshaftClicksPerBlock();

        // Create the break consumer
        BiConsumer<GamePlayer, DynamicMineBlock> breakConsumer = (p, b) -> player.getMineshaft().onBreak();

        // Shuffle
        List<ConfigLocation> blockLocations = config.getMineshaftField().getBlockLocations();
        Collections.shuffle(blockLocations);

        // Place blocks
        int blockCount = 0;
        int locationCount = 0;
        for (ConfigLocation blockLocation : blockLocations) {
            if (locationCount >= blockLocations.size() / 3 && blockCount >= 600) break;
            locationCount++;

            Location origin = blockLocation.toLocation();

            int maxRadius = Game.getRandom().nextInt(6) + 2;

            Set<DynamicMineBlock> blocks = DynamicMineBlocksService.i().generateBlob(DynamicMineBlockType.MINESHAFT, origin, false, maxRadius, blockMaterial, clicksPerBlock, breakConsumer, players);
            blockCount += blocks.size();
        }
    }

    private Set<GamePlayer> getPlayers(GamePlayer player) {
        return player.getCurrentIsland(false).getAllPlayers();
    }

    public void spawn(GamePlayer player) {
        for (GamePlayer pl : getPlayers(player)) {
            Player bukkitPlayer = pl.getPlayer();
            if (bukkitPlayer == null) return; // Cancel everything if anyone is being weird

            bukkitPlayer.teleport(getSpawn());
            pl.updateInventory();
        }

        Mineshaft mineshaft = player.getMineshaft();
        Runner.sync(0, 20, (state) -> {
            for (GamePlayer pl : getPlayers(player)) {
                if (state.getTicks() == 0 && mineshaft.isServerShutdown()) {
                    player.sendMessage("Your progress was restored", MessageType.INFO);
                    if (mineshaft.getBlocks() > 0) {
                        player.sendMessage("You have already collected " + mineshaft.getBlocks() + " blocks", MessageType.INFO);
                    }
                }

                pl.clickSound();
                MessageUtil.sendTitle(
                        ChatColor.GOLD + (state.getTicks() >= 5 ? "GO!" : (5 - state.getTicks()) + "..."),
                        ChatColor.GOLD + "Mine " + getBlockMaterialName(player) + " blocks!",
                        pl
                );
            }

            if (state.getTicks() >= 5) {
                state.cancel();
                load(player);

                if (!mineshaft.isServerShutdown()) {
                    mineshaft.reset();
                }

                mineshaft.start();
            }
        });

        //TODO
        //  - need to balance the pickaxe powerup a little better
        //  - add game leave item
        //  - stop arrows being picked up
    }

    public void onDeath(GamePlayer player) {
        for (GamePlayer pl : getPlayers(player)) {
            pl.sendMessage(ChatColor.YELLOW + player.getName() + ChatColor.RED + " was killed");
        }

        respawn(player);
    }

    public void respawn(GamePlayer player) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer == null) return;

        bukkitPlayer.teleport(getSpawn());
        player.updateInventory();

        bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false));
        bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 4, true, false, true));
    }

    public Material getBlockMaterial(GamePlayer player) {
        return Material.getMaterial(player.getDimensionsData().getDimension().getMineshaftBlockMaterial());
    }

    public String getBlockMaterialName(GamePlayer player) {
        return getBlockMaterial(player).toString().toLowerCase().replace("_", " ");
    }

    public boolean isInMineshaft(GamePlayer player) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer == null) return false;

        Location loc = bukkitPlayer.getLocation();
        return isInMineshaft(loc);
    }

    public boolean isInMineshaft(Location location) {
        int x = location.getBlockX();
        int z = location.getBlockZ();

        // TODO probably shouldn't be hardcoded
        return x < 2500 && x > 1500 && z < -500 && z > -1500;
    }

    public void updateInventory(GamePlayer player) {
        PlayerInventory inventory = player.getPlayer().getInventory();
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
        inventory.setItem(1, sword);
        inventory.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.DIAMOND_BOOTS));

        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 1);
        inventory.setItem(2, bow);
    }

    public void handleArrowSpawn(EntityShootBowEvent e) {
        AtomicReference<Set<GamePlayer>> playersReference = new AtomicReference<>();
        if (e.getEntityType().equals(EntityType.PLAYER)) {
            PlayersService.i().<GamePlayer>get(e.getEntity().getUniqueId(), player ->
                    playersReference.set(player.getCurrentIsland(false).getAllPlayers()));
        } else {
            for (GamePlayer player : PlayersService.i().<GamePlayer>getAll()) {
                Projectile projectile = (Projectile) e.getProjectile();
                ProjectileSource shooter = projectile.getShooter();

                if (shooter != null && player.getMineshaft().getEntities().contains(((CraftEntity) shooter).getHandle())) {
                    playersReference.set(player.getCurrentIsland(false).getAllPlayers());
                }
            }
        }

        Set<GamePlayer> players = playersReference.get();
        if (players != null) {
            players.forEach(p -> p.getAllowedEntities().add(e.getProjectile().getEntityId()));
        }
    }
}
