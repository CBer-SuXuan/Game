package net.mineclick.game.ai.movables;

import net.mineclick.game.ai.Robot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

public class LookAtTarget implements Consumer<ArmorStand> {
    private final Robot robot;
    private final Location target;

    public LookAtTarget(Robot robot, Location target) {
        this.robot = robot;
        this.target = target;
    }

    @Override
    public void accept(ArmorStand stand) {
        Vector dir = target.toVector().subtract(robot.getLocation().toVector()).normalize();

        double theta = Math.atan2(-dir.getX(), dir.getZ());
        stand.setYRot((float) Math.toDegrees((theta + 6.283185307179586D) % 6.283185307179586D));
    }
}
