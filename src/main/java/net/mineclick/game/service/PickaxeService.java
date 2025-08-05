package net.mineclick.game.service;

import lombok.Getter;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.pickaxe.Pickaxe;
import net.mineclick.game.model.pickaxe.PickaxeConfiguration;
import net.mineclick.global.service.ConfigurationsService;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.SingletonInit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

@SingletonInit
public class PickaxeService {
    private static PickaxeService i;

    @Getter
    private final Map<String, PickaxeConfiguration> configurations = new LinkedHashMap<>();

    private PickaxeService() {
        ConfigurationsService.i().onUpdate("pickaxe", this::load);
    }

    public static PickaxeService i() {
        return i == null ? i = new PickaxeService() : i;
    }

    private void load() {
        String baseCost = ConfigurationsService.i().get().getString("pickaxeBaseCost", "0");
        double costMultiplier = ConfigurationsService.i().get().getDouble("pickaxeCostMultiplier", 0);

        ConfigurationSection pickaxeSection = ConfigurationsService.i().get("pickaxe");
        if (pickaxeSection != null) {
            for (String id : pickaxeSection.getKeys(false)) {
                ConfigurationSection section = pickaxeSection.getConfigurationSection(id);
                PickaxeConfiguration config = configurations.computeIfAbsent(id, s -> new PickaxeConfiguration());

                config.setId(id);
                config.setName(section.getString("name", ""));
                config.setBaseCost(new BigNumber(baseCost));
                config.setCostMultiplier(costMultiplier);
                config.setBaseIncome(new BigNumber(section.getString("baseIncome", "0")));
                config.setMaterial(Material.valueOf(section.getString("material", "AIR")));
                config.setSpeed(section.getDouble("speed", 0));
                config.setGlow(section.getBoolean("glow", false));
                config.setMinLevel(section.getInt("minLevel", 0));
            }
        }
    }

    /**
     * Get a pickaxe configuration by id
     *
     * @param id The pickaxe id
     * @return The pickaxe configuration or null if not found
     */
    public PickaxeConfiguration getConfiguration(String id) {
        return configurations.get(id);
    }

    /**
     * Load and set player's pickaxe.
     * Will fallback to default, level 1 pickaxe
     *
     * @param player The player
     */
    public void loadPlayerPickaxe(GamePlayer player) {
        Pickaxe pickaxe = player.getPickaxe();
        if (pickaxe == null) {
            pickaxe = new Pickaxe();
            player.setPickaxe(pickaxe);
        }

        String id = player.getPickaxe().getId();
        PickaxeConfiguration configuration = configurations.get(id);
        if (configuration == null && !configurations.isEmpty()) {
            id = configurations.keySet().iterator().next();
        }

        pickaxe.setPlayer(player);
        pickaxe.setId(id);
        pickaxe.updateConfiguration();
    }

    /**
     * Upgrade (if possible) the player's pickaxe to the next level
     *
     * @param player The player
     * @param id     Pickaxe id
     */
    public void upgradePickaxe(GamePlayer player, String id) {
        Pickaxe pickaxe = player.getPickaxe();

        PickaxeConfiguration next = getConfiguration(id);
        if (next == null) return;

        pickaxe.setId(next.getId());
        pickaxe.updateConfiguration();
        pickaxe.updateItem(0);
    }
}
