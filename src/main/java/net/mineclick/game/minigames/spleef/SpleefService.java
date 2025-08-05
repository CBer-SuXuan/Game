package net.mineclick.game.minigames.spleef;

import com.comphenix.protocol.wrappers.BlockPosition;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.minigames.GameState;
import net.mineclick.game.minigames.MiniGameService;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.LobbyService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.service.StatisticsService;
import net.mineclick.game.type.StatisticType;
import net.mineclick.game.util.packet.BlockPolice;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.global.service.ConfigurationsService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.*;
import net.mineclick.global.util.location.LocationParser;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@SingletonInit
public class SpleefService extends MiniGameService {
    final static List<String> BREAKABLE_BLOCK_IDS = ImmutableList.of(
            "minecraft:light_blue_concrete",
            "minecraft:cyan_concrete",
            "minecraft:white_concrete"
    );
    private final static List<String> SHOVEL_NAMES = ImmutableList.of(
            "Magic wand",
            "Spleef tool",
            "Fancy shovel",
            "Shovel",
            "Digging contraption",
            "Duct taped shovel",
            "Mighty shovel",
            "One punch shovel",
            "Hole puncher",
            "A secret weapon",
            "It's just a shovel...",
            "Broken shovel",
            "Dirty shovel",
            "Banana!",
            "Spoon",
            "Old shovel",
            "Hot glued shovel"
    );
    private static SpleefService i;

    private final List<Map<Block, BlockData>> domeBlocks = new ArrayList<>();
    private final Set<Block> arenaBlocks = new HashSet<>();
    private final Set<Block> pickaxeBlocks = new HashSet<>();
    private Location domeMin;
    private Location domeMax;
    private Location pickaxeStart;
    private Location respawn;
    private int minArenaY;
    private boolean domeClosed = true;
    private boolean domeMoving;

    private SpleefService() {
        ConfigurationsService.i().onUpdate("lobby.spleef", this::load);
    }

    public static SpleefService i() {
        return i == null ? i = new SpleefService() : i;
    }

    private void load() {
        ConfigurationSection config = ConfigurationsService.i().get("lobby.spleef");

        // Mid game reload is not feasible due to the possibility of someone playing the game
        if (config == null || !domeBlocks.isEmpty()) {
            return;
        }

        domeMin = LocationParser.parse(config.getString("domeMin"));
        domeMax = LocationParser.parse(config.getString("domeMax"));
        pickaxeStart = LocationParser.parse(config.getString("pickaxeStart"));
        respawn = LocationParser.parse(config.getString("respawn"));
        Set<Material> materials = config.getStringList("domeMaterials").stream()
                .map(Material::getMaterial)
                .collect(Collectors.toSet());

        //Find dome blocks
        for (int y = domeMax.getBlockY(); y >= domeMin.getBlockY(); y--) {
            Map<Block, BlockData> layer = new HashMap<>();
            domeBlocks.add(layer);

            for (int x = domeMin.getBlockX(); x <= domeMax.getBlockX(); x++) {
                for (int z = domeMin.getBlockZ(); z <= domeMax.getBlockZ(); z++) {
                    Block block = domeMin.getWorld().getBlockAt(x, y, z);
                    if (materials.contains(block.getType())) {
                        layer.put(block, block.getBlockData());
                    }
                }
            }
        }
        openDome();

        //Find arena blocks
        minArenaY = pickaxeStart.getBlockY() - 10;
        Block startBlock = pickaxeStart.getBlock();

        Material arenaMaterial = null;
        for (int i = 0; i < 4; i++) {
            arenaMaterial = startBlock.getRelative(BlockFace.values()[i]).getType();
            if (arenaMaterial != startBlock.getType()) {
                break;
            }
        }
        recFillSearch(arenaBlocks, pickaxeBlocks, arenaMaterial, startBlock.getType(), startBlock);

        //Exclude from block police
        BlockPolice.exclude.addAll(arenaBlocks.parallelStream().map(b -> new BlockPosition(b.getX(), b.getY(), b.getZ())).collect(Collectors.toSet()));

        //Start the state machine
        //Scheduled since we don't wanna call Spleef.i() before finishing this constructor
        Runner.sync(() -> nextState(new WaitingState()));
    }

    public boolean isInArena(Location location) {
        Block block = location.getBlock().getRelative(BlockFace.DOWN);
        return arenaBlocks.contains(block) || arenaBlocks.contains(block.getRelative(BlockFace.DOWN));
    }

    public boolean isAboveArena(Location l) {
        for (Block block : getArenaBlocks()) {
            Location bl = block.getLocation();
            if (bl.getBlockX() == l.getBlockX() && bl.getBlockZ() == l.getBlockZ() && (l.getBlockY() >= bl.getBlockY() && l.getBlockY() < bl.getBlockY() + 20)) {
                return true;
            }
        }

        return false;
    }

    private void recFillSearch(Set<Block> arenaSet, Set<Block> pickaxeSet, Material arenaMaterial, Material pickaxeMaterial, Block block) {
        if (block == null || (!block.getType().equals(arenaMaterial) && !block.getType().equals(pickaxeMaterial)) || arenaSet.contains(block) || arenaSet.size() > 10000)
            return;

        arenaSet.add(block);
        if (block.getType().equals(pickaxeMaterial)) {
            pickaxeSet.add(block);
        }
        for (int i = 0; i < 4; i++) {
            recFillSearch(arenaSet, pickaxeSet, arenaMaterial, pickaxeMaterial, block.getRelative(BlockFace.values()[i]));
        }
    }

    void openDome() {
        if (domeMoving || !domeClosed)
            return;
        domeMoving = true;

        Runner.sync(0, 5, state -> {
            if (state.getTicks() >= domeBlocks.size()) {
                state.cancel();
                domeClosed = false;
                domeMoving = false;
            } else {
                domeBlocks.get((int) state.getTicks()).forEach((block, materialData) -> block.setType(Material.AIR));
            }
        });
    }

    void closeDome() {
        if (domeMoving || domeClosed)
            return;
        domeMoving = true;

        Runner.sync(0, 5, state -> {
            if (state.getTicks() >= domeBlocks.size()) {
                state.cancel();
                domeClosed = true;
                domeMoving = false;
            } else {
                domeBlocks.get((int) (domeBlocks.size() - state.getTicks() - 1))
                        .forEach((block, data) -> block.setBlockData(data, false));
            }
        });
    }

    void nextState(GameState nextState) {
        if (currentState != null) {
            HandlerList.unregisterAll(currentState);
        }

        currentState = nextState;
        if (currentState == null) {
            currentState = new WaitingState();
        }

        Bukkit.getPluginManager().registerEvents(currentState, Game.i());
        Runner.sync(0, 1, currentState);
    }

    void setupPlayer(GamePlayer player, boolean giveSpade) {
        if (player.isOffline())
            return;

        Player p = player.getPlayer();
        if (player.getRank().isAtLeast(Rank.PAID) && !player.getRank().isAtLeast(Rank.STAFF)) {
            p.setFlying(false);
            p.setAllowFlight(false);
        }
        p.setWalkSpeed(0.2F);
        ((CraftPlayer) p).getHandle().stopRiding();
        p.getOpenInventory().close();

        PlayerInventory inventory = p.getInventory();
        inventory.clear();
        inventory.setItem(8, ItemBuilder.builder()
                .material(Material.BARRIER)
                .title(ChatColor.RED + "Leave the game")
                .build().toItem());
        if (giveSpade) {
            ItemStack item = ItemBuilder.builder()
                    .material(Material.DIAMOND_SHOVEL)
                    .title(ChatColor.YELLOW + SHOVEL_NAMES.get(Game.getRandom().nextInt(SHOVEL_NAMES.size())))
                    .lore(" ")
                    .lore(ChatColor.GRAY + "Break blocks under enemies")
                    .lore(ChatColor.GRAY + "Last standing wins")
                    .glowing(true)
                    .build().toItem();
            ItemMeta meta = item.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            item.setItemMeta(meta);

            // TODO test
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
            ListTag tagList = new ListTag();
            tagList.addAll(BREAKABLE_BLOCK_IDS.stream().map(StringTag::valueOf).toList());
            CompoundTag tag = nmsItem.hasTag() && nmsItem.getTag() != null ? nmsItem.getTag() : new CompoundTag();
            tag.put("CanDestroy", tagList);
            nmsItem.setTag(tag);

            inventory.setItem(0, CraftItemStack.asBukkitCopy(nmsItem));

            ItemStack snowball = ItemBuilder.builder()
                    .material(Material.SNOWBALL)
                    .amount(16)
                    .title(ChatColor.YELLOW + "Snowball")
                    .lore(" ")
                    .lore(ChatColor.GRAY + "Break blocks under enemies")
                    .build().toItem();

            inventory.setItem(1, snowball);
        }
    }

    void clearPlayer(GamePlayer player) {
        player.updateInventory();

        if (player.getRank().isAtLeast(Rank.PAID)) {
            player.getPlayer().setAllowFlight(true);
        }
    }

    public void playerDie(GamePlayer player) {
        if (player.isOffline())
            return;

        for (int i = 0; i < 10; i++) {
            Location l = player.getPlayer().getLocation().add(0.5 + Game.getRandom().nextDouble() - 0.5, Game.getRandom().nextDouble() * 2, 0.5 + Game.getRandom().nextDouble() - 0.5);
            DroppedItem.spawn(Material.BEEF, l, 60, null);
        }
        ParticlesUtil.send(ParticleTypes.CLOUD, player.getPlayer().getLocation(), Triple.of(0F, 0F, 0F), 10, LobbyService.i().getPlayersInLobby());

        player.getPlayer().teleport(respawn);
        clearPlayer(player);
    }

    public void rewardPlayer(GamePlayer player, int numPlayers, boolean won) {
        int exp = won ? Math.max(Math.min(numPlayers, 20), 5) : 3;
        player.addExp(exp);
        player.sendMessage("Game over " + (won ? ChatColor.GREEN + "You won!" : ChatColor.RED + "You lost"), MessageType.INFO);
        player.sendMessage("You got" + ChatColor.GREEN + " +" + ChatColor.AQUA + exp + ChatColor.YELLOW + " EXP", MessageType.INFO);

        StatisticsService.i().increment(player.getUuid(), StatisticType.SPLEEF_GAMES);
        QuestsService.i().incrementProgress(player, "dailySpleef", 0, 1);
        if (won) {
            StatisticsService.i().increment(player.getUuid(), StatisticType.SPLEEF_WINS);
        }
    }

    public void playerLeave(GamePlayer player) {
        if (player.isOffline())
            return;

        player.getPlayer().teleport(respawn);
        clearPlayer(player);
    }

    public void updateInventory(GamePlayer player) {
        if (currentState != null) {
            currentState.updateInventory(player);
        }
    }
}
