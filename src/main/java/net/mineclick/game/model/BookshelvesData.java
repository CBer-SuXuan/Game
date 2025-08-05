package net.mineclick.game.model;

import lombok.Data;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

@Data
public class BookshelvesData {
    private Set<Location> dustedLocations = new HashSet<>();
}
