package net.mineclick.game.ai.movables;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.ai.Robot;
import net.mineclick.game.util.ArmorStandUtil;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class Oscillate implements Consumer<ArmorStand> {
    private final Set<Oscillation> oscillations = new LinkedHashSet<>();
    private final Robot monster;

    @Override
    public void accept(ArmorStand stand) {
        oscillations.forEach(o -> o.tick(stand));
    }

    public Oscillate robotSpeed(ArmorStandUtil.Part part, ArmorStandUtil.Axis axis, double from, double to, boolean linear, double coefficient) {
        dynamicSpeed(part, axis, from, to, linear, () -> monster.getSpeed() * coefficient);
        return this;
    }

    public Oscillate dynamicSpeed(ArmorStandUtil.Part part, ArmorStandUtil.Axis axis, double from, double to, boolean linear, Supplier<Double> speed) {
        oscillations.add(new Oscillation(part, axis, from, to, linear, speed));
        return this;
    }

    static class Oscillation {
        private final ArmorStandUtil.Part part;
        private final ArmorStandUtil.Axis axis;
        private final double from;
        private final double to;
        private final boolean linear; //TODO add sinusoidal
        private final Supplier<Double> speed;
        private boolean rounded;

        private Double current = null;
        private int dir = 1;

        public Oscillation(ArmorStandUtil.Part part, ArmorStandUtil.Axis axis, double from, double to, boolean linear, Supplier<Double> speed) {
            this.part = part;
            this.axis = axis;
            this.from = from;
            this.linear = linear;
            this.speed = speed;
            if (from > to) {
                to += 360;
                rounded = true;
            }
            this.to = to;
        }

        void tick(ArmorStand stand) {
            if (current == null) {
                current = axis.getFromVector(part.getVector(stand));
                if (rounded && current < from) {
                    current += 360;
                }
            }

            if (current >= to) {
                dir = -1;
            } else if (current <= from) {
                dir = 1;
            }
            current += dir * speed.get();

            Rotations currentVector = part.getVector(stand);
            double x = axis.equals(ArmorStandUtil.Axis.X) ? current % 360 : currentVector.getX();
            double y = axis.equals(ArmorStandUtil.Axis.Y) ? current % 360 : currentVector.getY();
            double z = axis.equals(ArmorStandUtil.Axis.Z) ? current % 360 : currentVector.getZ();
            ArmorStandUtil.rotate(stand, part, x, y, z, false);
        }
    }
}
