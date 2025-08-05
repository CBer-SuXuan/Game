package net.mineclick.game.model;

import lombok.Data;
import net.minecraft.world.entity.monster.Silverfish;
import org.bukkit.Location;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class CampfireData {
    private final transient Map<Location, Integer> fightingCampfires = new HashMap<>();
    private final transient Set<Silverfish> silverfish = new HashSet<>();
    private transient boolean silverfishTalking;
    private transient boolean silverfishFinishedTalking;

    private boolean silverfishDeal;
    private Instant nextSilverfishDelivery = Instant.now();
    private Set<Location> litCampfires = new HashSet<>();

    public Set<Silverfish> getSilverfish(Location location) {
        silverfish.removeIf(silverfish -> {
            boolean tooFar = silverfish.getBukkitEntity().getLocation().distanceSquared(location) > 100; // 10
            if (tooFar) {
                silverfish.discard();
            }

            return !silverfish.isAlive();
        });

        return silverfish;
    }
}
