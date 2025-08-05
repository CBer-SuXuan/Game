package net.mineclick.game.model;

import lombok.Data;
import net.minecraft.world.entity.monster.Zombie;
import org.bukkit.Location;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class CookieThievesData {
    private final transient Map<Location, Zombie> zombies = new HashMap<>();

    private Instant nextCookiesDelivery = Instant.now();
    private Set<Location> caughtZombies = new HashSet<>();
}
