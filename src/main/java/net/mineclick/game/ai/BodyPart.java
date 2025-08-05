package net.mineclick.game.ai;

import lombok.Getter;
import net.mineclick.game.util.ArmorStandUtil;
import org.bukkit.Location;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

@Getter
public class BodyPart {
    private static final Vector UP = new Vector(0, 1, 0);
    private static final Vector FORWARD = new Vector(0, 0, 1);

    private final ArmorStandUtil armorStand;
    private final Robot monster;
    private final double yawOffset;
    private final Vector centerOffset;
    private final double yOffset;
    private final double offsetRadius;
    private final double offsetDegree;
    private boolean spawned;
    private boolean rotateWithRobot = true;

    public BodyPart(Robot monster, Vector centerOffset, double yawOffset, ArmorStandUtil.ArmorStandUtilBuilder builder) {
        this.monster = monster;
        this.centerOffset = centerOffset;
        yOffset = centerOffset.getY();
        if (centerOffset.getX() == 0 && centerOffset.getZ() == 0) {
            offsetRadius = 0;
            offsetDegree = 0;
        } else {
            offsetRadius = Math.sqrt(NumberConversions.square(centerOffset.getX()) + NumberConversions.square(centerOffset.getZ()));
            offsetDegree = Math.toDegrees((Math.atan2(-centerOffset.getX() / offsetRadius, centerOffset.getZ() / offsetRadius) + 6.283185307179586D) % 6.283185307179586D);
        }
        this.yawOffset = yawOffset;

        Location loc = calculateRelativeLocation();
        armorStand = builder
                .viewers(monster.getPlayers())
                .location(loc)
                .build();
    }

    public BodyPart rotateWithRobot(boolean rotate) {
        rotateWithRobot = rotate;
        return this;
    }

    public void move() {
        if (!spawned) {
            spawned = true;
            armorStand.spawn();
        }

        armorStand.move(calculateRelativeLocation(), rotateWithRobot);
    }

    private Location calculateRelativeLocation() {
        Vector vector = new Vector();
        if (offsetRadius != 0) {
            double rotX = Math.toRadians(offsetDegree + (double) monster.getLocation().getYaw());
            vector.setX(-offsetRadius * Math.sin(rotX));
            vector.setZ(offsetRadius * Math.cos(rotX));
        }
        vector.setY(yOffset);

        Location location = monster.getLocation().clone();
        location.setYaw(location.getYaw() + (float) yawOffset);
        return location.add(vector);
    }
}
