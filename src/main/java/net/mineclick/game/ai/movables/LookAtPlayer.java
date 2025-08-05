package net.mineclick.game.ai.movables;

import lombok.RequiredArgsConstructor;
import net.mineclick.game.ai.Robot;
import net.mineclick.game.model.GamePlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class LookAtPlayer implements Consumer<ArmorStand> {
    private final Robot monster;

    @Override
    public void accept(ArmorStand stand) {
        GamePlayer player = monster.getPlayers().stream()
                .filter(gamePlayer -> !gamePlayer.isOffline())
                .min(Comparator.comparingDouble(p -> p.getPlayer().getLocation().distanceSquared(monster.getLocation())))
                .orElse(null);
        if (player != null) {
            Location pLoc = player.getPlayer().getLocation();
            Vector dir = pLoc.toVector().subtract(monster.getLocation().toVector()).normalize();

            double theta = Math.atan2(-dir.getX(), dir.getZ());
            stand.setYRot((float) Math.toDegrees((theta + 6.283185307179586D) % 6.283185307179586D));
        }
    }
}
