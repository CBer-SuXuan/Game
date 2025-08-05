package net.mineclick.game.util;

import net.mineclick.game.Game;
import org.bukkit.util.Vector;

public class VectorUtil {
    public static Vector rotateOnVector(Vector axisVector, Vector vector, double angle) {
        double u = axisVector.getX();
        double v = axisVector.getY();
        double w = axisVector.getZ();
        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();

        double d = u * x + v * y + w * z;
        double xPrime = u * d * (1D - Math.cos(angle)) + x * Math.cos(angle) + (-w * y + v * z) * Math.sin(angle);
        double yPrime = v * d * (1D - Math.cos(angle)) + y * Math.cos(angle) + (w * x - u * z) * Math.sin(angle);
        double zPrime = w * d * (1D - Math.cos(angle)) + z * Math.cos(angle) + (-v * x + u * y) * Math.sin(angle);

        return vector.setX(xPrime).setY(yPrime).setZ(zPrime);
    }

    public static Vector getPerpendicularTo(Vector origin, boolean random) {
        if (origin.getY() == 0 && origin.getZ() == 0) {
            return origin.getCrossProduct(new Vector(0, 1, 0));
        }

        Vector vector = origin.getCrossProduct(new Vector(1, 0, 0)).normalize();
        if (random) {
            rotateOnVector(origin, vector, Game.getRandom().nextDouble() * Math.PI * 2);
        }
        return vector;
    }
}
