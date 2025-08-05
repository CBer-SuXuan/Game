package net.mineclick.game.type.powerup;

import lombok.AccessLevel;
import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.type.Holiday;
import net.mineclick.game.type.Rarity;
import net.mineclick.game.type.powerup.orb.*;
import net.mineclick.game.type.powerup.perks.*;
import net.mineclick.game.type.powerup.pickaxe.*;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.global.util.Skins;
import org.bukkit.Material;

import java.util.function.BiFunction;

@Getter
public enum PowerupType {
    AUTOCLICKER(Autoclicker::new, new PickaxePerk(0.015), "Autoclicker", PowerupCategory.PICKAXE, Material.WOODEN_PICKAXE, Rarity.COMMON),
    ANVIL(Anvil::new, new BuildingsPerk(0.12), "Anvil", PowerupCategory.PICKAXE, Material.ANVIL, Rarity.UNCOMMON),
    BOUNCY_BLOCKS(BouncyBlocks::new, new ParkourPerk(0.02), "Bouncy Blocks", PowerupCategory.PICKAXE, Material.SLIME_BLOCK, Rarity.UNCOMMON),
    TNT(Tnt::new, new DimensionPerk(0, 0.5), "TNT", PowerupCategory.PICKAXE, Material.TNT, Rarity.UNCOMMON),
    CREEPER(Creeper::new, new WorkerPerk(WorkerType.CREEPER, 0.02), "Creeper", PowerupCategory.PICKAXE, Material.CREEPER_HEAD, Rarity.RARE),
    SILVERFISH(Silverfish::new, new DimensionPerk(0, 0.1), "Silverfish", PowerupCategory.PICKAXE, Material.INFESTED_MOSSY_STONE_BRICKS, Rarity.RARE),
    BEES(Bees::new, new DimensionPerk(2, 0.1), "Bees", PowerupCategory.PICKAXE, Material.BEEHIVE, Rarity.RARE),
    LIGHTNING(Lightning::new, new DimensionPerk(1, 0.1), "Lightning", PowerupCategory.PICKAXE, Material.END_ROD, Rarity.VERY_RARE),
    FIREBALL(Fireball::new, new WorkerPerk(WorkerType.BLAZE, 0.025), "Fireball", PowerupCategory.PICKAXE, Material.FIRE_CHARGE, Rarity.VERY_RARE),
    SWORDS(Swords::new, new WorkerPerk(WorkerType.ZOMBIE, 0.015), "Swords", PowerupCategory.PICKAXE, Material.DIAMOND_SWORD, Rarity.VERY_RARE),
    ICE(Ice::new, new WorkerPerk(WorkerType.SNOWMAN, 0.025), "Ice", PowerupCategory.PICKAXE, Material.PACKED_ICE, Rarity.VERY_RARE),
    BLACK_HOLE(BlackHole::new, new WorkerPerk(WorkerType.ENDERMAN, 0.03), "Black Hole", PowerupCategory.PICKAXE, Material.HOPPER, Rarity.LEGENDARY),
    ROBOT(Robot::new, new WorkerPerk(WorkerType.GOLEM, 0.03), "Robot", PowerupCategory.PICKAXE, Robot.HEAD, Rarity.LEGENDARY),
    BLAZING_WHEEL(BlazingWheel::new, new DimensionPerk(1, 0.2), "Blazing Wheel", PowerupCategory.PICKAXE, Material.BLAZE_ROD, Rarity.LEGENDARY),
    UFO(UFO::new, new DimensionPerk(2, 0.3), "U.F.O.", PowerupCategory.PICKAXE, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2RlN2I4YmFmNzg1ODdiNzQ0MjdlYjBjNzMxMzc0N2Y1OGM0MTgzMDVhMTQ3Mjc4Yjc3MzE1YTljOTdmZDkifX19"), Rarity.LEGENDARY),

    //TODO
    //  - lightning
    //  - laser
    // For summer:
    //  - sand
    //  - sun
    //  - beach ball
    // For halloween:
    //  - jack o lantern
    //  - skeleton
    //  - candle
    //  - web
    //  - zombie head
    //  - bats

    DIRT_ORB(DirtOrb::new, new DimensionPerk(0, 0.05), "Dirt", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzdlOGNiNTdmZTc5MGU5NjVlM2NmYTZjNGZiYzE2ZTMyMjYyMTBkNjVmNTYxNGU4ODUzZmE5ZmI4NDA3NDQ0MSJ9fX0="), Rarity.COMMON),
    WATER_ORB(WaterOrb::new, new DimensionPerk(2, 0.05), "Water", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTY3OTliZmFhM2EyYzYzYWQ4NWRkMzc4ZTY2ZDU3ZDlhOTdhM2Y4NmQwZDlmNjgzYzQ5ODYzMmY0ZjVjIn19fQ=="), Rarity.COMMON),
    FIREBALL_ORB(FireballOrb::new, new DimensionPerk(1, 0.05), "Fireball", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzM2ODdlMjVjNjMyYmNlOGFhNjFlMGQ2NGMyNGU2OTRjM2VlYTYyOWVhOTQ0ZjRjZjMwZGNmYjRmYmNlMDcxIn19fQ=="), Rarity.UNCOMMON),
    ENDER_ORB(EnderOrb::new, new WorkerPerk(WorkerType.ENDERMAN, 0.02), "Ender", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmY3NzhmNzJlYWNhNDUyZWUwMTM4OTNlMmJjNTNkOGQ1YjFmY2E0NGNmZTI3MDM4NjViMDU0YzI4YTNkZDcifX19"), Rarity.UNCOMMON),
    BALL_8_ORB(Ball8Orb::new, new SuperBlockPerk(), "8 Ball", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTg0ZGJjNmExMzk5YThiYWVlZThkZWE5MmY5MGQzOTgzMjljNjVlY2Y1ODhjYmNhM2Y3MTdhMWY0NDAzNTgzIn19fQ=="), Rarity.UNCOMMON),
    TNT_ORB(TntOrb::new, new DimensionPerk(1, 0.08), "TNT", PowerupCategory.ORB, Material.TNT, Rarity.RARE),
    SLIME_ORB(SlimeOrb::new, new WorkerPerk(WorkerType.SLIME, 0.03), "Slime", PowerupCategory.ORB, Material.SLIME_BLOCK, Rarity.RARE),
    CANDY_CANE_ORB(CandyCane::new, new DimensionPerk(2, 0.08), "Candy Cane", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmIyMTYxN2QyNzU1YmMyMGY4ZjdlMzg4ZjQ5ZTQ4NTgyNzQ1ZmVjMTZiYjE0Yzc3NmY3MTE4Zjk4YzU1ZTgifX19"), Holiday.WINTER),
    PRESENT_ORB(Present::new, new HolidayPerk(Holiday.CHRISTMAS, 0.5), "Present", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ5N2Y0ZjQ0ZTc5NmY3OWNhNDMwOTdmYWE3YjRmZTkxYzQ0NWM3NmU1YzI2YTVhZDc5NGY1ZTQ3OTgzNyJ9fX0="), Holiday.WINTER),
    SNOWBALL_ORB(Snowball::new, new WorkerPerk(WorkerType.SNOWMAN, 0.07), "Snowball", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzAyOTFlOTMyNjI5NmFhYWYyN2YyNTVkZTk4MWU3ODc4NjhiYzYyZmU0NjYzOWVlODdiMjhhMTk1ZTlkOTliZiJ9fX0="), Holiday.WINTER),
    ICE_CUBE_ORB(IceCube::new, new HolidayPerk(Holiday.WINTER, 0.25), "Ice Cube", PowerupCategory.ORB, Skins.loadSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGRiYTY0MmVmZmZhMTNlYzM3MzBlYWZjNTkxNGFiNjgxMTVjMWY5OTg4MDNmNzQ0NTJlMmUwY2QyNmFmMGI4In19fQ=="), Holiday.WINTER),
    ;

    @Getter(AccessLevel.NONE)
    private final BiFunction<PowerupType, GamePlayer, ? extends Powerup> powerup;
    private final String name;
    private final PowerupCategory category;
    private final Material material;
    private final Rarity rarity;
    private final String skull;
    private final Holiday holiday;
    private final PowerupPerk perk;

    PowerupType(BiFunction<PowerupType, GamePlayer, ? extends Powerup> powerup, PowerupPerk perk, String name, PowerupCategory category, Material material, Rarity rarity) {
        this.powerup = powerup;
        this.perk = perk;
        this.name = name;
        this.category = category;
        this.material = material;
        this.rarity = rarity;
        this.skull = null;
        this.holiday = null;
    }

    PowerupType(BiFunction<PowerupType, GamePlayer, ? extends Powerup> powerup, PowerupPerk perk, String name, PowerupCategory category, String skull, Rarity rarity) {
        this.powerup = powerup;
        this.perk = perk;
        this.name = name;
        this.category = category;
        this.material = Material.PLAYER_HEAD;
        this.skull = skull;
        this.rarity = rarity;
        this.holiday = null;
    }

    PowerupType(BiFunction<PowerupType, GamePlayer, ? extends Powerup> powerup, PowerupPerk perk, String name, PowerupCategory category, Material material, Holiday holiday) {
        this.powerup = powerup;
        this.perk = perk;
        this.name = name;
        this.category = category;
        this.material = material;
        this.rarity = Rarity.SPECIAL;
        this.skull = null;
        this.holiday = holiday;
    }

    PowerupType(BiFunction<PowerupType, GamePlayer, ? extends Powerup> powerup, PowerupPerk perk, String name, PowerupCategory category, String skull, Holiday holiday) {
        this.powerup = powerup;
        this.perk = perk;
        this.name = name;
        this.category = category;
        this.material = Material.PLAYER_HEAD;
        this.skull = skull;
        this.rarity = Rarity.SPECIAL;
        this.holiday = holiday;
    }

    public Powerup run(GamePlayer player) {
        return powerup.apply(this, player);
    }
}
