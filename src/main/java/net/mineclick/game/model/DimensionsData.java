package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.global.config.DimensionConfig;

import java.util.HashMap;
import java.util.Map;

@Data
public class DimensionsData {
    private int currentDimensionId = 0;
    private Map<Integer, Integer> ascensions = new HashMap<>();

    public DimensionConfig getDimension() {
        return DimensionConfig.getById(currentDimensionId);
    }

    public int getAscensionsIn(int id) {
        return ascensions.getOrDefault(id, 0);
    }

    public int getAscensionsTotal() {
        return ascensions.values().stream().mapToInt(i -> i).sum();
    }

    public void incrementCurrentAscensions() {
        ascensions.compute(currentDimensionId, (key, value) -> value == null ? 1 : value + 1);
    }
}
