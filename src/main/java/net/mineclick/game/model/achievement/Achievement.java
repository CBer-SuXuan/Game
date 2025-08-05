package net.mineclick.game.model.achievement;

import lombok.Data;

@Data
public class Achievement {
    private final String name;
    private int level;
    private String description;
    private double score;
    private long schmepls;
    private long exp;
}
