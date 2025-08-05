package net.mineclick.game.gadget.generic;

import com.google.common.collect.EvictingQueue;
import net.mineclick.game.Game;
import net.mineclick.game.gadget.Gadget;
import net.mineclick.game.minigames.spleef.SpleefService;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.service.LobbyService;
import net.mineclick.game.util.VectorUtil;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Runner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PortalGun extends Gadget {
    private static final int FIRE_BALL_STEP = 5;
    private static final double FIRE_BALL_RES = 1;
    private static final double OVAL_RES = Math.PI / 10;
    private static final Color BLUE = new Color(25, 118, 210);
    private static final Color ORANGE = new Color(230, 74, 25);
    private static final Color GRAY = new Color(98, 98, 98);

    private static final Map<GamePlayer, Pair<Portal, Portal>> portals = new HashMap<>();
    private static final Map<GamePlayer, Portal> entered = new HashMap<>();
    private static final Map<GamePlayer, Vector> lastVelocity = new HashMap<>();
    private static final Map<GamePlayer, AtomicInteger> cooldown = new HashMap<>();

    static {
        Runner.sync(0, 1, state -> {
            Set<GamePlayer> players = new HashSet<>();
            players.addAll(portals.keySet());
            players.addAll(entered.keySet());
            players.addAll(lastVelocity.keySet());

            for (GamePlayer player : players) {
                if (!LobbyService.i().isInLobby(player) || !player.getLobbyData().getCurrentGadget().equals("portalGun")) {
                    portals.remove(player);
                    entered.remove(player);
                    lastVelocity.remove(player);
                }
            }

            cooldown.values().removeIf(i -> i.decrementAndGet() <= 0);
            portals.values().forEach(p -> {
                if (p.key() != null) {
                    p.key().tick();
                }
                if (p.value() != null) {
                    p.value().tick();
                }
            });
        });
    }

    @Override
    public boolean canRun(GamePlayer player, Action action) {
        return !action.equals(Action.PHYSICAL);
    }

    @Override
    public void run(GamePlayer player, Action action) {
        boolean left = action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK);
        Color color = left ? BLUE : ORANGE;

        Location loc = player.getPlayer().getEyeLocation();
        Vector dir = loc.getDirection();
        loc.add(dir);

        dir.multiply(FIRE_BALL_RES);
        EvictingQueue<Location> queue = EvictingQueue.create(FIRE_BALL_STEP * 3);
        Runner.sync(0, 1, state -> {
            if (state.getTicks() > 50 || !LobbyService.i().isInLobby(player)) {
                state.cancel();
                return;
            }

            boolean hit = false;
            for (int i = 0; i < FIRE_BALL_STEP; i++) {
                Location l = loc.add(dir).clone();
                queue.add(l);

                if (!hit && l.getBlock().getType().isSolid()) {
                    hit = true;
                    Block block = l.getBlock();

                    BlockFace face = block.getFace(l.add(dir.clone().multiply(-1)).getBlock());
                    if (face != null) {
                        Pair<Location, BlockFace> locationFace = findLocation(block, face, player.getPlayer().getLocation().getYaw());
                        if (locationFace != null) {
                            Pair<Portal, Portal> pair = portals.getOrDefault(player, Pair.of(null, null));
                            if (pair.key() != null && pair.key().isIn(locationFace.key().getBlock()) || pair.value() != null && pair.value().isIn(locationFace.key().getBlock())) {
                                player.noSound();
                            } else {
                                Portal portal = new Portal(player, locationFace.key(), locationFace.value(), color);
                                portals.put(player, Pair.of(left ? portal : pair.key(), left ? pair.value() : portal));

                                player.popSound();
                            }
                        } else {
                            player.noSound();
                        }
                    } else {
                        player.noSound();
                    }

                    state.cancel();
                }
            }
            queue.forEach(l -> ParticlesUtil.sendColor(l, color, player));
        });
    }

    private Pair<Location, BlockFace> findLocation(Block block, BlockFace face, float playerYaw) {
        if (face.ordinal() > 9 || SpleefService.i().isAboveArena(block.getLocation())) {
            return null;
        }

        if (face.ordinal() > 5) {
            switch (face) {
                case NORTH_EAST:
                case NORTH_WEST:
                    if (checkLocation(block, BlockFace.NORTH, BlockFace.UP)) {
                        face = BlockFace.NORTH;
                    } else {
                        BlockFace blockFace = face.equals(BlockFace.NORTH_EAST) ? BlockFace.EAST : BlockFace.WEST;
                        if (checkLocation(block, blockFace, BlockFace.UP)) {
                            face = blockFace;
                        }
                    }
                    break;
                case SOUTH_EAST:
                case SOUTH_WEST:
                    if (checkLocation(block, BlockFace.SOUTH, BlockFace.UP)) {
                        face = BlockFace.SOUTH;
                    } else {
                        BlockFace blockFace = face.equals(BlockFace.SOUTH_EAST) ? BlockFace.EAST : BlockFace.WEST;
                        if (checkLocation(block, blockFace, BlockFace.UP)) {
                            face = blockFace;
                        }
                    }
                    break;
            }

            if (face.ordinal() > 5) {
                return null;
            }
        }

        BlockFace dirFace;
        if (face.getModY() == 0) {
            dirFace = BlockFace.UP;
        } else {
            dirFace = yawToFace(playerYaw);
        }
        Vector dir = faceToVector(dirFace);
        if (!checkLocation(block, face, dirFace))
            return null;

        Location location = block.getLocation()
                .add(0.5, 0.5, 0.5)
                .add(face.getModX() * 0.6, face.getModY() * 0.6, face.getModZ() * 0.6);
        location.setDirection(dir);

        return Pair.of(location, face);
    }

    private boolean checkLocation(Block block, BlockFace forwardFace, BlockFace dirFace) {
        Location location = block.getLocation()
                .add(0.5, 0.5, 0.5)
                .add(forwardFace.getModX(), forwardFace.getModY(), forwardFace.getModZ());

        block = location.getBlock();
        return block.getType().equals(Material.AIR)
                && block.getRelative(dirFace).getType().equals(Material.AIR);
    }

    @Override
    public ItemBuilder.ItemBuilderBuilder getBaseItem() {
        return ItemBuilder.builder().material(Material.IRON_HOE);
    }

    @Override
    public String getImmutableName() {
        return "portalGun";
    }

    @Override
    public String getName() {
        return "Portal Gun";
    }

    @Override
    public String getDescription() {
        return "Make yourself some portals";
    }

    @Override
    public int getCooldown() {
        return 1;
    }

    @EventHandler
    public void on(PlayerMoveEvent e) {
        PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), player -> {
            if (entered.containsKey(player)) {
                Portal portal = entered.get(player);
                Block block = e.getTo().getBlock();
                if (block.equals(portal.block1) || block.equals(portal.block2))
                    return;
            }
            entered.remove(player);

            if (portals.containsKey(player)) {
                Pair<Portal, Portal> pair = portals.get(player);
                if (pair.key() != null && pair.value() != null) {
                    if (pair.key().isIn(e.getTo().getBlock())) {
                        tp(player, e.getPlayer(), pair.key(), pair.value());
                    } else if (pair.value().isIn(e.getTo().getBlock())) {
                        tp(player, e.getPlayer(), pair.value(), pair.key());
                    }

                    lastVelocity.put(player, e.getPlayer().getVelocity());
                }
            }
        });
    }

    private void tp(GamePlayer player, Player p, Portal from, Portal to) {
        if (cooldown.containsKey(player)) {
            portals.remove(player);
            entered.remove(player);
            lastVelocity.remove(player);
            return;
        }

        Vector dirFrom = faceToVector(from.face);
        Vector dirTo = faceToVector(to.face);
        Vector velocity = lastVelocity.getOrDefault(player, p.getVelocity());
        Vector direction = p.getLocation().getDirection();

        //One of them is facing UP/DOWN so first rotate on the Z-axis
        if (Math.abs(from.face.getModY()) != Math.abs(to.face.getModY())) {
            Vector axis = faceToVector(BlockFace.NORTH);
            VectorUtil.rotateOnVector(axis, dirFrom, Math.PI / 2);
            VectorUtil.rotateOnVector(axis, velocity, Math.PI / 2);
//            VectorUtil.rotateOnVector(axis, direction, Math.PI / 2);
        }

        //Now find the angle between the two
        boolean up = ((int) dirFrom.getY()) != 0;
        double angle = dirFrom.angle(dirTo) + Math.PI;
        if (!up && dirFrom.getZ() * dirTo.getX() - dirFrom.getX() * dirTo.getZ() < 0) {
            angle = -angle;
        }
        Vector axis = up ? faceToVector(BlockFace.NORTH) : faceToVector(BlockFace.UP);

        VectorUtil.rotateOnVector(axis, velocity, angle);
//        VectorUtil.rotateOnVector(axis, direction, angle);

        p.teleport(to.spawn.clone().setDirection(direction));
        Runner.sync(1, () -> {
            p.setVelocity(velocity.multiply(0.95));
        });

        entered.put(player, to);
        player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT);

        cooldown.put(player, new AtomicInteger(10));
    }

    private Vector faceToVector(BlockFace face) {
        return new Vector(face.getModX(), face.getModY(), face.getModZ());
    }

    private BlockFace yawToFace(float yaw) {
        double rotation = (yaw - 180) % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }
        if (45 <= rotation && rotation < 135) {
            return BlockFace.EAST;
        } else if (135 <= rotation && rotation < 225) {
            return BlockFace.SOUTH;
        } else if (225 <= rotation && rotation < 315) {
            return BlockFace.WEST;
        }
        return BlockFace.NORTH;
    }

    private class Portal {
        private final BlockFace face;
        private final Color color;
        private final GamePlayer player;
        private final List<Location> points = new ArrayList<>();
        private final Block block1;
        private final Block block2;
        private final Location center;
        private final Location spawn;
        private final Vector dir;
        private double r = 0.1;

        Portal(GamePlayer player, Location location, BlockFace face, Color color) {
            this.player = player;
            dir = location.getDirection();
            block1 = location.getBlock();
            block2 = location.clone().add(dir).getBlock();
            this.face = face;
            this.color = color;
            center = location.add(dir.clone().multiply(0.5));
            spawn = center.clone().add(faceToVector(face).multiply(0.5));
        }

        boolean isIn(Block block) {
            return block1.equals(block) || block2.equals(block);
        }

        void tick() {
            if (r <= 1) {
                points.clear();
                for (double theta = 0; theta < Math.PI * 2; theta += OVAL_RES) {
                    double x = center.getX();
                    double y = center.getY();
                    double z = center.getZ();

                    if (face == BlockFace.UP || face == BlockFace.DOWN) {
                        x += r * Math.cos(theta) / (((int) dir.getX()) == 0 ? 2 : 1);
                        z += r * Math.sin(theta) / (((int) dir.getX()) == 0 ? 1 : 2);
                    } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                        x += r * Math.cos(theta) / 2;
                        y += r * Math.sin(theta);
                    } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
                        y += r * Math.sin(theta);
                        z += r * Math.cos(theta) / 2;
                    }

                    points.add(new Location(center.getWorld(), x, y, z));
                }
                r += 0.1;
            } else {
                Set<GamePlayer> players = getPlayersInLobby();
                players.remove(player);
                for (int i = 0; i < 3 && i < points.size(); i++) {
                    Location l = points.get(Game.getRandom().nextInt(points.size()));
                    ParticlesUtil.sendColor(l, GRAY, players);
                }
            }

            points.forEach(l -> ParticlesUtil.sendColor(l, color, player));
        }
    }
}
