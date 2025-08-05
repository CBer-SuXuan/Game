package net.mineclick.game.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.mineclick.game.Game;
import net.mineclick.global.config.IslandConfig;
import net.mineclick.global.config.field.MineRegionConfig;
import net.mineclick.global.util.location.ConfigLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class IslandModel {
    private int id;
    private List<BuildingModel> buildings = new ArrayList<>();

    private transient IslandConfig config;
    private transient GamePlayer player;
    private transient Set<GamePlayer> visitors = new HashSet<>();
    private transient Location teleporterLocation;

    public IslandModel(int id) {
        this.id = id;
    }

    public Location getTeleporterLocation() {
        if (teleporterLocation == null) {
            teleporterLocation = config.getTeleporter().toLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        }

        return teleporterLocation;
    }

    /**
     * Get owner and visitors
     *
     * @return A set of all players on the island (owner and visitors)
     */
    public Set<GamePlayer> getAllPlayers() {
        Set<GamePlayer> players = new HashSet<>(visitors);
        players.add(player);
        return players;
    }

    /**
     * Get unlocked npc spawn locations
     *
     * @return A list of unlocked npc spawn locations
     */
    public List<ConfigLocation> getNpcSpawns() {
        List<ConfigLocation> locations = new ArrayList<>(config.getNpcSpawns());
        locations.addAll(buildings.stream()
                .filter(buildingModel -> buildingModel.getLevel() > 0)
                .flatMap(b -> b.getConfig().getExtraNpcSpawns().stream())
                .toList());

        return locations;
    }

    /**
     * Get the mine region by the click location
     *
     * @param loc The click location
     * @return The mine region if matched by location, null otherwise
     */
    public List<MineRegionConfig> getMineRegions(Location loc) {
        return getMineRegions().stream()
                .filter(r -> r.getRegion().isIn(loc))
                .collect(Collectors.toList());
    }

    /**
     * Get all the mine regions
     *
     * @return A list of mine regions
     */
    public List<MineRegionConfig> getMineRegions() {
        List<MineRegionConfig> regions = new ArrayList<>(config.getMineRegions());
        regions.addAll(buildings.stream()
                .filter(buildingModel -> buildingModel.getLevel() > 0)
                .flatMap(b -> b.getConfig().getExtraMineRegions().stream())
                .toList());

        return regions;
    }

    /**
     * Get a random mine region
     *
     * @return A random mine region
     */
    public MineRegionConfig getRandomMineRegion() {
        List<MineRegionConfig> regions = getMineRegions();

        return !regions.isEmpty() ? regions.get(Game.getRandom().nextInt(regions.size())) : null;
    }

    /**
     * Get a random npc spawn
     *
     * @return A random npc spawn
     */
    public Location getRandomNpcSpawn() {
        List<ConfigLocation> locations = getNpcSpawns();

        return !locations.isEmpty() ? locations.get(Game.getRandom().nextInt(locations.size())).toLocation() : null;
    }

    /**
     * Update the island
     */
    public void update() {
        buildings.forEach(BuildingModel::build);
    }

    public void updateParkour() {
        if (config.getParkour() == null || player.isOffline()) return;

        Location location = config.getParkour().getNpcSpawnLocation().toLocation();
        if (player.getParkour().isStarted()) {
            player.sendBlockChange(location, Bukkit.createBlockData(Material.LIGHT_WEIGHTED_PRESSURE_PLATE));
        } else {
            player.sendBlockChange(location.getBlock());
        }
    }

    /**
     * Clear up the island
     */
    public void clear() {
    }

    /**
     * Remove the player from visitors
     *
     * @param player The player to remove
     */
    public void removeVisitor(GamePlayer player) {
        visitors.remove(player);
    }

    /**
     * Resend all building schematics
     */
    public void resendSchematics() {
        buildings.forEach(BuildingModel::build);
    }
}
