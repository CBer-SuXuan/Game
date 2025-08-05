package net.mineclick.game.model;

import lombok.Data;
import lombok.Value;
import net.mineclick.game.Game;
import net.mineclick.game.service.AchievementsService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.service.SkillsService;
import net.mineclick.game.type.TraderItemType;
import net.mineclick.game.type.skills.SkillType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Trader {
    private boolean spawned;
    private boolean refreshed;
    private boolean warned;
    private long lastVisit = 0;
    private long nextVisit = 0;
    private Map<Integer, Item> items = new HashMap<>();

    private transient GamePlayer player;
    private transient WanderingTrader trader;
    private transient boolean boughtEverything;

    public void secondTick() {
        if (!SkillsService.i().has(player, SkillType.TRADER_1)) {
            despawn();
            return;
        }
        long currentTime = System.currentTimeMillis();

        // find when next to visit at
        if (nextVisit == 0 || trader == null) {
            // visit every 40 minutes
            double minutes = 40;
            if (SkillsService.i().has(player, SkillType.TRADER_2)) {
                minutes /= 2; // 20 minutes
            }

            if (currentTime - lastVisit > minutes * 60 * 1000) {
                nextVisit = currentTime + 30 * 1000;
                lastVisit = System.currentTimeMillis();
            }
        }

        // check if should spawn
        if (trader == null && (spawned || nextVisit > 0 && nextVisit < currentTime)) {
            spawn();
            return;
        }

        long visitedFor = currentTime - nextVisit;

        // check if should leave now (despawn after 10 minutes)
        if (spawned && (items.isEmpty() || visitedFor > 10 * 60 * 1000)) {
            if (warned || items.isEmpty()) {
                player.playSound(Sound.ENTITY_WANDERING_TRADER_DISAPPEARED, 0.5, 1);
                player.sendMessage("The Wandering Trader has left your island", MessageType.INFO);
            }

            reset();
            return;
        }

        // check if about to leave
        if (!warned && nextVisit != 0 && visitedFor > 9 * 60 * 1000) {
            warned = true;
            player.sendMessage("The Wandering Trader will be leaving in 1 minute", MessageType.INFO);
        }
    }

    public void spawn() {
        if (player == null || !player.isOnOwnIsland() || player.getActivityData().isAfk()) return;

        // announce
        if (!spawned) {
            spawned = true;
            player.sendMessage("A Wandering Trader is visiting your island", MessageType.INFO);
            player.playSound(Sound.ENTITY_WANDERING_TRADER_REAPPEARED);
        }

        // calculate the items
        if (items.isEmpty()) {
            buildItems();
        }

        // spawn the trader
        despawn();
        trader = new WanderingTrader(EntityType.WANDERING_TRADER, ((CraftWorld) player.getPlayer().getLocation().getWorld()).getHandle()) {
            @Override
            public void tick() {
                if (!player.isOnOwnIsland() || player.getActivityData().isAfk()) {
                    despawn();
                    discard();
                    return;
                }
                super.tick();

                if (getY() < 0) {
                    Location location = player.getCurrentIsland(false).getConfig().getSpawn().toLocation();
                    trader.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
                }
            }

            @Override
            protected void registerGoals() {
                this.goalSelector.addGoal(0, new FloatGoal(this));
                this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
                this.goalSelector.addGoal(1, new PanicGoal(this, 0.5D));
                this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
                this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35D));
                this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.35D));
                this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
                this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
            }

            @Override
            public InteractionResult mobInteract(Player entityhuman, InteractionHand enumhand) {
                if (entityhuman.getUUID().equals(player.getUuid())) {
                    openMenu();
                }

                return InteractionResult.PASS;
            }
        };
        trader.setInvulnerable(true);
        trader.setSilent(true);
        trader.setInvisible(false);
        trader.setCustomNameVisible(true);
        trader.setCustomName(CraftChatMessage.fromStringOrNull(ChatColor.GREEN + "Wandering Trader" + ChatColor.GRAY + " - right-click"));

        Location location = player.getCurrentIsland(false).getConfig().getTutorialVillagerSpawn().toLocation();
        trader.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        player.getAllowedEntities().add(trader.getId());
        trader.level().addFreshEntity(trader, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    public void despawn() {
        if (trader == null) return;

        trader.discard();
        trader = null;
    }

    public void reset() {
        despawn();

        items.clear();
        spawned = false;
        refreshed = false;
        warned = false;
        nextVisit = 0;
        boughtEverything = false;
    }

    private void openMenu() {
        InventoryUI inventoryUI = new InventoryUI("   MineClick Wandering Trader", 27);
        inventoryUI.setItem(0, MenuUtil.getCloseMenu());

        // refresh
        ItemUI refreshItem = new ItemUI(Material.BOOK);
        refreshItem.setUpdateConsumer(itemUI -> {
            // check if villager dispawned
            if (!spawned) {
                player.getPlayer().getOpenInventory().close();
                return;
            }

            if (refreshed) {
                itemUI.setMaterial(Material.AIR);
                return;
            }
            itemUI.setTitle(ChatColor.GOLD + "Refresh items");
            itemUI.setLore(" ");
            itemUI.addLore(ChatColor.GREEN + "Get a new set of items!");
            itemUI.addLore(ChatColor.GRAY + "Can only be used once");

            if (!player.getRank().isAtLeast(Rank.PAID)) {
                itemUI.addLore(" ");
                itemUI.addLore(ChatColor.RED + "Requires Premium Membership");
                itemUI.addLore(ChatColor.GRAY + "Get yours at " + ChatColor.AQUA + "store.mineclick.net");
            }
        });
        refreshItem.setClickConsumer(inventoryClickPack -> {
            if (refreshed || !player.isRankAtLeast(Rank.PAID)) {
                return;
            }

            refreshed = true;
            player.playSound(Sound.ENTITY_WANDERING_TRADER_TRADE);
            buildItems();
        });
        inventoryUI.setItem(8, refreshItem);

        // items
        for (int i = 10; i <= 16; i += 2) {
            ItemUI itemUI = new ItemUI(Material.AIR);
            int pos = i;
            itemUI.setUpdateConsumer(uiItem -> {
                Item item = items.get(pos);
                if (item == null) {
                    uiItem.setMaterial(Material.AIR);
                    return;
                }

                boolean canBuy = player.getGold().greaterThanOrEqual(item.getPrice());
                boolean discounted = SkillsService.i().has(player, SkillType.TRADER_4);

                uiItem.setMaterial(item.type.getMaterial());
                uiItem.setTitle(ChatColor.GREEN + "x" + item.getAmount() + " " + ChatColor.YELLOW + item.getType().getName());
                uiItem.setLore(" ");
                uiItem.addLore(MenuUtil.prerequisiteGold(player, item.getPrice(), "Price: ") + (discounted ? ChatColor.GRAY + " (20% off)" : ""));
                uiItem.addLore(" ");
                uiItem.addLore((canBuy ? ChatColor.GREEN + "Click to purchase" : ChatColor.RED + "Not enough gold"));
            });
            itemUI.setClickConsumer(inventoryClickPack -> {
                Item item = items.get(pos);
                if (item == null) return;

                if (player.chargeGold(item.getPrice())) {
                    item.purchase(player);

                    AchievementsService.i().incrementProgress(player, "trader", 1);
                    QuestsService.i().incrementProgress(player, "dailyTrader", 0, 1);

                    items.remove(pos);
                    if (items.isEmpty()) {
                        boughtEverything = true;
                    }
                    player.playSound(Sound.ENTITY_WANDERING_TRADER_YES);
                }
            });
            inventoryUI.setItem(pos, itemUI);
        }

        inventoryUI.open(player.getPlayer());
    }

    private void buildItems() {
        if (boughtEverything) return;

        int count = SkillsService.i().has(player, SkillType.TRADER_3) ? 4 : 2;
        if (!items.isEmpty()) {
            count = items.size();
        }
        items.clear();

        List<TraderItemType> types = TraderItemType.getRandom(count);
        int pos = count <= 2 ? 12 : 10;
        for (TraderItemType type : types) {
            double percent = Game.getRandom().nextDouble();
            BigNumber price = type.getPrice().apply(type.getAmount().apply(percent), player);
            if (SkillsService.i().has(player, SkillType.TRADER_4)) {
                price = price.multiply(new BigNumber(0.8));
            }

            items.put(pos, new Item(type, percent, price));
            pos += 2;
        }
    }

    @Value
    public static class Item {
        TraderItemType type;
        double percent;
        BigNumber price;

        public int getAmount() {
            return type.getAmount().apply(percent);
        }

        public void purchase(GamePlayer player) {
            type.getOnPurchase().accept(getAmount(), player);
        }
    }
}
