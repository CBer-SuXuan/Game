package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.*;
import net.mineclick.game.type.BoosterType;
import net.mineclick.game.type.PermanentMultiplier;
import net.mineclick.game.type.StatisticType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Strings;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class MainMenu extends InventoryUI {
    public static final ItemStack MENU_ITEM = ItemBuilder.builder()
            .material(Material.CRAFTING_TABLE)
            .title(ChatColor.YELLOW + "Main Menu" + ChatColor.GRAY + "" + ChatColor.ITALIC + " right-click")
            .build().toItem();

    private boolean boosterCooldown;
    private boolean achievementsCheckCooldown;

    public MainMenu(GamePlayer player) {
        super("        MineClick Main Menu", 54);

        // Profile info
        ItemUI profileItem = new ItemUI(player.getTexture(), clickPack -> {
        });
        AtomicInteger profileUpdateCounter = new AtomicInteger(100);
        profileItem.setUpdateConsumer(item -> {
            if (profileUpdateCounter.incrementAndGet() < 100)
                return;
            profileUpdateCounter.set(0);

            item.setTitle(ChatColor.GOLD + "Profile info: " + ChatColor.YELLOW + player.getName());
            item.setLore(" ");
            item.addLore(ChatColor.DARK_GREEN + "Level: " + ChatColor.YELLOW + (LevelsService.i().getLevel(player.getExp())) + ChatColor.GOLD + " (" + player.getExp() + " EXP)");
            item.addLore(ChatColor.DARK_GREEN + "Schmepls: " + ChatColor.YELLOW + Formatter.format(player.getSchmepls()));
            item.addLore(ChatColor.DARK_GREEN + "Gold: " + player.getGold().print(player));
            item.addLore(ChatColor.DARK_GREEN + "Gold income: " + new BigNumber(player.getGoldRate().multiply(new BigNumber(20))).print(player));
            item.addLore(ChatColor.DARK_GREEN + "Gold made so far: " + player.getLifelongGold().print(player));
            int parts = (int) PowerupService.i().getTotalParts(player);
            if (parts > 0) {
                item.addLore(ChatColor.DARK_GREEN + "Total powerup parts: " + ChatColor.YELLOW + parts);
            }
            item.addLore(ChatColor.DARK_GREEN + "Super Block: " + ChatColor.YELLOW + (int) (player.computeSuperBlockPercent() * 100) + "%");
            if (player.getDimensionsData().getAscensionsTotal() > 0) {
                //Meaning ascended at least once
                item.addLore(ChatColor.DARK_GREEN + "Ascends: " + ChatColor.YELLOW + player.getDimensionsData().getAscensionsTotal());
                item.addLore(ChatColor.DARK_GREEN + "Dimension: " + ChatColor.YELLOW + player.getDimensionsData().getDimension().getName());
            }
            item.addLore(ChatColor.DARK_GREEN + "Clicks: " + ChatColor.YELLOW + Formatter.format((int) StatisticsService.i().get(player.getUuid(), StatisticType.CLICKS).getScore()));
            item.addLore(ChatColor.DARK_GREEN + "Server votes: " + ChatColor.YELLOW + ((int) StatisticsService.i().get(player.getUuid(), StatisticType.VOTES).getScore()));
            item.addLore(ChatColor.DARK_GREEN + "Time played: " + ChatColor.YELLOW + Formatter.duration(player.getActivityData().getPlayTime() * 1000));
            item.addLore(ChatColor.DARK_GREEN + "Daily login streak: " + ChatColor.YELLOW + player.getDailyStreak() + (player.getDailyStreak() >= DailyServices.i().getBestDailyStreak(player) ? "" : " (" + DailyServices.i().getBestDailyStreak(player) + " highest)"));
            item.addLore(ChatColor.DARK_GREEN + "Offline vaults/hours: " + ChatColor.YELLOW + player.getVaults() + " (" + player.getGoldRate().multiply(new BigNumber(20 * 60 * 60 * player.getVaults())).print(player) + ChatColor.YELLOW + " gold)");
            if (player.getRank().isAtLeast(Rank.PAID)) {
                item.addLore(ChatColor.LIGHT_PURPLE + " ");
                item.addLore(ChatColor.LIGHT_PURPLE + "Premium Membership");
            }
        });
        setItem(3, profileItem);

        // Friends
        ItemUI friendsItem = new ItemUI(ItemBuilder.builder()
                .material(Material.LEATHER_HELMET)
                .title(ChatColor.YELLOW + "Friends")
                .lore(" ")
                .lore(ChatColor.GRAY + "Click to see Friends")
                .build().toItem(),
                clickPack -> {
                    player.getFriendsMenu().open(FriendsMenu.Sort.ALL, 0, p -> open(p.getPlayer()));
                    player.clickSound();
                });
        friendsItem.setUpdateConsumer(item -> {
            int amount = player.getFriendsData().getReceivedRequests().size();
            if (amount > 0) {
                item.setGlowing();
                item.setAmount(Math.min(64, amount));
            } else {
                item.removeGlowing();
                item.setAmount(1);
            }
        });
        setItem(5, friendsItem);

        // Settings
        setItem(8, new ItemUI(ItemBuilder.builder()
                .material(Material.COMMAND_BLOCK)
                .title(ChatColor.YELLOW + "Settings")
                .lore(" ")
                .lore(ChatColor.GRAY + "Click to see Settings")
                .build().toItem(),
                clickPack -> {
                    player.getSettingsMenu().open(player.getPlayer());
                    player.clickSound();
                }));

        // Multiplier
        ItemUI multiplierItem = new ItemUI(player.getMultiplier().getMaterial(), clickPack -> {
            PermanentMultiplier next = PermanentMultiplier.next(player.getMultiplier());
            if (next != null && player.chargeSchmepls(next.getCost())) {
                player.setMultiplier(next);

                player.sendMessage(ChatColor.YELLOW + "You now get " + ChatColor.GREEN + next.getMultiplier() + "x" + ChatColor.YELLOW + " gold for every block!");
                player.levelUpSound();
                player.recalculateGoldRate();
            }
        });
        multiplierItem.setUpdateConsumer(itemUI -> {
            PermanentMultiplier next = PermanentMultiplier.next(player.getMultiplier());
            if (next == null) {
                itemUI.setMaterial(Material.COBWEB);
                itemUI.setTitle(ChatColor.GRAY + "No more multipliers!");
                itemUI.setLore(" ");
                itemUI.addLore(ChatColor.GRAY + "Current multiplier: " + ChatColor.GREEN + player.getMultiplier().getMultiplier() + ChatColor.YELLOW + "x");
            } else {
                itemUI.setMaterial(next.getMaterial());
                boolean canAfford = player.getSchmepls() >= next.getCost();
                itemUI.setTitle((canAfford ? ChatColor.YELLOW : ChatColor.RED) + next.getName() + " multiplier");
                itemUI.addLore(ChatColor.GRAY + "Current multiplier: " + ChatColor.GREEN + "x" + player.getMultiplier().getMultiplier());
                itemUI.setLore(" ");
                itemUI.addLore(ChatColor.GRAY + "Next multiplier: " + ChatColor.GREEN + "x" + next.getMultiplier());
                itemUI.addLore(cost(next.getCost()));
                itemUI.addLore(" ");
                itemUI.addLore(canAfford(canAfford));
            }
        });
        setItem(22, multiplierItem);

        // Skills menu
        ItemUI skillTreeItem = new ItemUI(ItemBuilder.builder()
                .title(ChatColor.YELLOW + "Skills")
                .lore(" ")
                .lore(ChatColor.GRAY + "Click to open Skills menu")
                .material(Material.LECTERN));
        skillTreeItem.setClickConsumer(click -> {
            player.getSkillsMenu().open();
            player.clickSound();
        });
        skillTreeItem.setUpdateConsumer(itemUI -> {
            if (SkillsService.i().hasAnythingToUnlock(player)) {
                itemUI.setGlowing();
            } else {
                itemUI.removeGlowing();
            }
        });
        setItem(28, skillTreeItem);

        // Powerups and geodes
        ItemUI powerupsItem = new ItemUI(Material.ENCHANTING_TABLE);
        powerupsItem.setClickConsumer(click -> {
            PowerupService.i().openMenu(player);
            player.clickSound();
        });
        powerupsItem.setUpdateConsumer(itemUI -> {
            itemUI.setTitle(ChatColor.YELLOW + "Powerups and Geodes");
            itemUI.setLore(" ");
            itemUI.addLore(ChatColor.GRAY + "Click to open");
            itemUI.addLore(ChatColor.GRAY + "Powerups and Geodes menu");
        });
        setItem(30, powerupsItem);

        // Quests
        ItemUI questsItem = new ItemUI(ItemBuilder.builder().material(Material.BOOK).title(ChatColor.YELLOW + "Quests").lore(" ").lore(ChatColor.GRAY + "Click to see Quests").build().toItem(), clickPack -> {
            new QuestsMenu(player);
            player.clickSound();
        });
        setItem(32, questsItem);

        // Achievements
        ItemUI achItem = new ItemUI(ItemBuilder.builder().material(Material.BOOKSHELF).title(ChatColor.YELLOW + "Achievements").lore(" ").lore(ChatColor.GRAY + "Click to see Achievements").build().toItem(), clickPack -> {
            player.getAchievementsMenu().open(player.getPlayer());
            player.clickSound();
        });
        achItem.setUpdateConsumer(item -> {
            if (achievementsCheckCooldown) return;
            achievementsCheckCooldown = true;
            schedule(100, () -> achievementsCheckCooldown = false);

            if (AchievementsService.i().hasUncollected(player)) {
                item.setGlowing();
            } else {
                item.removeGlowing();
            }
        });
        setItem(34, achItem);

        // Boosters
        int boosterIndex = 0;
        for (int col = 1; col < 8; col++) {
            if (col == 4) col++;

            BoosterType booster = BoosterType.values()[boosterIndex++];
            ItemUI itemUI = new ItemUI(ItemBuilder.builder().material(Material.GLASS_BOTTLE).title(ChatColor.GRAY + booster.getName()).build().toItem(), clickPack -> {
                if (player.getBoosters().getOrDefault(booster, 0) > 0) {
                    if (boosterCooldown) {
                        player.getPlayer().sendMessage(ChatColor.RED + "Please wait 3 seconds");
                    } else {
                        boosterCooldown = true;
                        schedule(60, () -> boosterCooldown = false);

                        BoostersService.i().activateBooster(player, booster);
                        player.popSound();
                    }
                } else {
                    Strings.sendStore(player.getPlayer());
                }
            });
            itemUI.setUpdateConsumer(item -> {
                if (player.getBoosters().getOrDefault(booster, 0) > 0) {
                    item.setTitle(ChatColor.GOLD + booster.getName());
                    item.setMaterial(Material.POTION);
                    item.setAmount(Math.min(64, player.getBoosters().get(booster)));

                    PotionMeta meta = (PotionMeta) item.getItem().getItemMeta();
                    Objects.requireNonNull(meta).setColor(booster.getColor());
                    item.getItem().setItemMeta(meta);

                    item.setLore(" ");
                    item.addLore(ChatColor.YELLOW + "Click to activate!");
                } else {
                    item.setTitle(ChatColor.RED + booster.getName());
                    item.setMaterial(Material.GLASS_BOTTLE);

                    item.setLore(" ");
                    item.addLore(ChatColor.GRAY + "Purchase this booster at " + ChatColor.DARK_AQUA + "store.mineclick.net");
                }
                item.addLore(ChatColor.GRAY + booster.getDescription());
                item.addLore(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + booster.getDurationMin() + " minutes");
            });
            setItem(5 * 9 + col, itemUI);
        }

        setItem(0, MenuUtil.getCloseMenu());
        setDestroyOnClose(false);
    }

    private String canAfford(boolean can) {
        return can ? (ChatColor.GOLD + "Click to purchase!") : (ChatColor.RED + "Not enough schmepls");
    }

    private String cost(long cost) {
        return ChatColor.GRAY + "Cost: " + ChatColor.AQUA + cost + ChatColor.YELLOW + " schmepls";
    }

    private String ascensionOnly() {
        return ChatColor.GRAY + "Only lasts for current ascension!";
    }
}
