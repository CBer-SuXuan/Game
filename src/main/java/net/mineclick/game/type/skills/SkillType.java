package net.mineclick.game.type.skills;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.mineclick.game.menu.WorkerCookiesMenu;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.pickaxe.PickaxeConfiguration;
import net.mineclick.game.service.PickaxeService;
import net.mineclick.global.util.Formatter;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
public enum SkillType {
    // Clicking
    PICKAXE_1("I", Category.PICKAXE, clickingDescription("stone"), 50, 1, player -> PickaxeService.i().upgradePickaxe(player, "stone")),
    PICKAXE_2("II", Category.PICKAXE, clickingDescription("iron"), 150, 2, player -> PickaxeService.i().upgradePickaxe(player, "iron")),
    PICKAXE_3("III", Category.PICKAXE, clickingDescription("gold"), 400, 3, player -> PickaxeService.i().upgradePickaxe(player, "gold")),
    PICKAXE_4("IV", Category.PICKAXE, clickingDescription("diamond"), 800, 5, player -> PickaxeService.i().upgradePickaxe(player, "diamond")),
    PICKAXE_5("V", Category.PICKAXE, clickingDescription("netherite"), 2000, 7, player -> PickaxeService.i().upgradePickaxe(player, "netherite")),
    PICKAXE_6("VI", Category.PICKAXE, () -> "Pickaxe income will grow exponentially\nup to x1,000 at level 1,000", 12500, 11, GamePlayer::recalculateGoldRate),
    // Workers
    WORKERS_1("I", Category.WORKERS, () -> "Workers are 100% more productive\nevery 50 levels", 380, 2),
    WORKERS_2("II", Category.WORKERS, () -> "10% discount for\nworkers over level 50", 550, 3),
    WORKERS_3("III", Category.WORKERS, () -> "x2 income for all\nworkers over level 50", 2100, 5),
    WORKERS_4("IV", Category.WORKERS, () -> "x5 income for all\nworkers over level 100", 4200, 6),
    WORKERS_5("V", Category.WORKERS, () -> "x10 income for all\nworkers over level 250", 8600, 8),
    WORKERS_6("VI", Category.WORKERS, () -> "x10 income for all workers", 9000, 10),
    // Ascension
    ASCENSION_1("I", Category.ASCENSION, () -> "Unlock Zombie on ascension", 300, 2),
    ASCENSION_2("II", Category.ASCENSION, () -> "Get 2 extra pickaxes\non ascension", 450, 2),
    ASCENSION_3("III", Category.ASCENSION, () -> "Receive 10,000 gold\non ascension", 640, 3),
    ASCENSION_4("IV", Category.ASCENSION, () -> "Unlock Skeleton on ascension", 800, 4),
    ASCENSION_5("V", Category.ASCENSION, () -> "Unlock Spider on ascension", 1200, 5),
    ASCENSION_6("VI", Category.ASCENSION, () -> "x2 ascension exp and\nschmepls rewards", 4000, 6),
    // Powerup
    POWERUP_1("I", Category.POWERUP, () -> "Powerup is 10% faster to charge", 250, 1),
    POWERUP_2("II", Category.POWERUP, () -> "Powerup discharge is 50% slower", 430, 2),
    POWERUP_3("III", Category.POWERUP, () -> "Super blocks charge up\nthe powerup by 5%", 1000, 3),
    POWERUP_4("IV", Category.POWERUP, () -> "Powerup is 25% faster to charge", 2000, 4),
    POWERUP_5("V", Category.POWERUP, () -> "10% chance to collect x5 gold", 6000, 6),
    POWERUP_6("VI", Category.POWERUP, () -> "0.5% chance to fully charge\nthe powerup with one click", 9000, 8),
    // Super Block
    SUPERBLOCK_1("I", Category.SUPER_BLOCK, () -> "Super blocks appear 10% more often", 300, 1),
    SUPERBLOCK_2("II", Category.SUPER_BLOCK, () -> "Super blocks give 20% extra gold", 1000, 3),
    SUPERBLOCK_3("III", Category.SUPER_BLOCK, () -> "Super blocks appear when a Powerup is active.\nAt most 1 super block per second", 2500, 4),
    SUPERBLOCK_4("IV", Category.SUPER_BLOCK, () -> "Super blocks appear 20% more often", 3000, 5),
    SUPERBLOCK_5("V", Category.SUPER_BLOCK, () -> "Super blocks give 50% extra gold", 7500, 6),
    SUPERBLOCK_6("VI", Category.SUPER_BLOCK, () -> "+2% chance of getting a super block for\nevery unlocked island", 12500, 8),
    // Bat
    BAT_1("I", Category.BAT, () -> "Receive a bow and 3 arrows\nto help catch the golden bat", 150, 2),
    BAT_2("II", Category.BAT, () -> "Bats give 50% extra gold", 500, 3),
    BAT_3("III", Category.BAT, () -> "Receive 5 arrows.", 1300, 4),
    //    BAT_3("III", Category.BAT, () -> "Receive a crossbow with Quick Charge\nand Multishot instead of bow.", 1300, 4),
    BAT_4("IV", Category.BAT, () -> "Additional bat may spawn nearby", 2800, 5),
    BAT_5("V", Category.BAT, () -> "Bats may spawn in a swarm", 5000, 6),
    // Cookie
    COOKIE_1("I", Category.COOKIE, () -> "Collect cookies from mining.\nWorkers will be 50% more\nproductive for 15 sec\nwhen given the cookie", 150, 2),
    COOKIE_2("II", Category.COOKIE, () -> "Cookie effect will last\nfor 30 seconds", 300, 3),
    COOKIE_3("III", Category.COOKIE, () -> "Workers are 100% more\nproductive when given a cookie", 1000, 4),
    COOKIE_4("IV", Category.COOKIE, () -> "Double the chance to\nreceive a cookie", 1200, 5),
    COOKIE_5("V", Category.COOKIE, () -> "Automatically give cookies\nto workers", 5300, 6, null, WorkerCookiesMenu::new),
    // Trader
    TRADER_1("I", Category.TRADER, () -> "Get a wandering trader\nto visit your island", 100, 3),
    TRADER_2("II", Category.TRADER, () -> "The wandering trader will\nvisit more frequently", 500, 5),
    TRADER_3("III", Category.TRADER, () -> "Trader has x2 more selection", 650, 6),
    TRADER_4("IV", Category.TRADER, () -> "Lower trader's prices by 20%", 800, 7),
    // Daily Chest
    DAILY_CHEST_1("I", Category.DAILY_CHEST, () -> "Gain 25% more rewards", 150, 1),
    DAILY_CHEST_2("II", Category.DAILY_CHEST, () -> "Daily chest has 50% more clicks", 300, 2),
    DAILY_CHEST_3("III", Category.DAILY_CHEST, () -> "Daily chest may drop a geode", 600, 3),
    DAILY_CHEST_4("IV", Category.DAILY_CHEST, () -> "Daily chest gives x2 exp and schmepls", 800, 5),
    // Misc
    MISC_1("I", Category.MISC, () -> "Voting is x2 more rewarding", 100, 2),
    MISC_2("II", Category.MISC, () -> "Get extra 4 vaults", 200, 3),
    MISC_3("III", Category.MISC, () -> "Don't lose your daily streak\nif you miss one day", 400, 4),
    // Parkour
    PARKOUR_1("I", Category.PARKOUR, () -> "Receive x2 parkour gold", 110, 1),
    PARKOUR_2("II", Category.PARKOUR, () -> "Get 2 extra parkour checkpoints", 450, 3, player -> player.getParkour().setCheckpoints(player.getParkour().getCheckpoints() + 2)),
    ;

    private final String number;
    private final Category category;
    private final Supplier<String> description;
    private final int cost;
    private final int minLevel;
    private final Consumer<GamePlayer> onPurchase;
    private final Consumer<GamePlayer> onRightClick;

    SkillType(String number, Category category, Supplier<String> description, int cost, int minLevel, Consumer<GamePlayer> onPurchase) {
        this.number = number;
        this.category = category;
        this.description = description;
        this.cost = cost;
        this.minLevel = minLevel;
        this.onPurchase = onPurchase;
        this.onRightClick = null;
    }

    SkillType(String number, Category category, Supplier<String> description, int cost, int minLevel) {
        this(number, category, description, cost, minLevel, null);
    }

    private static Supplier<String> clickingDescription(String id) {
        return () -> {
            PickaxeConfiguration configuration = PickaxeService.i().getConfiguration(id);
            if (configuration == null) return "";

            double speed = (180D / configuration.getSpeed()) / 20D;
            return ChatColor.GRAY + "Get the " + configuration.getName() + "\n" +
                    ChatColor.GRAY + "when it's at level " + configuration.getMinLevel() + "\n \n" +
                    ChatColor.GRAY + "Income: " + ChatColor.GREEN + Formatter.format(configuration.getBaseIncome().longValue()) + "x\n" +
                    ChatColor.GRAY + "Speed: " + ChatColor.GREEN + Formatter.format(speed) + " sec\n" +
                    ChatColor.GRAY + "Min pickaxe level: " + ChatColor.GREEN + configuration.getMinLevel();
        };
    }

    public SkillType getPrevious() {
        int ordinal = ordinal();
        if (ordinal <= 0) return null;

        SkillType previous = values()[ordinal - 1];
        return previous.getCategory().equals(category) ? previous : null;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Category {
        PICKAXE("Pickaxe", Material.WOODEN_PICKAXE),
        WORKERS("Workers", Material.ZOMBIE_HEAD),
        ASCENSION("Ascension", Material.END_PORTAL_FRAME),
        POWERUP("Powerup", Material.ENCHANTING_TABLE),
        SUPER_BLOCK("Super Block", Material.GOLDEN_PICKAXE),
        BAT("Bat", Material.BLACK_DYE),
        COOKIE("Cookie", Material.COOKIE),
        TRADER("Trader", Material.EMERALD),
        DAILY_CHEST("Daily Chest", Material.CHEST),
        MISC("Misc", Material.FIREWORK_ROCKET),
        PARKOUR("Parkour", Material.LEATHER_BOOTS),
        ;

        private final String name;
        private final Material material;

        public int getCount() {
            return (int) Arrays.stream(SkillType.values())
                    .filter(skillType -> skillType.getCategory().equals(this))
                    .count();
        }
    }
}
