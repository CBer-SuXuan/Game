package net.mineclick.game.model;

import lombok.Data;
import net.mineclick.game.Game;
import net.mineclick.game.service.MineshaftService;
import net.mineclick.game.util.visual.DroppedItem;
import net.mineclick.global.model.PlayerId;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.ResponsiveScoreboard;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.Strings;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class Mineshaft {
    private static final long MAX_TIME = 10 * 60 * 1000;
    private static final int MIN_BLOCKS = 100;
    private static final int FIRST_WAVE_START = 60; // 600

    private long timer = MAX_TIME;
    private int blocks = 0;
    private int targetBlocks = MIN_BLOCKS;
    private boolean serverShutdown;
    private int waveCount = 0;

    private transient boolean started;
    private transient GamePlayer player;
    private transient String blockName = "";
    private transient Set<Entity> entities = new HashSet<>();
    private transient int nextWaveTicks = FIRST_WAVE_START;

    public void start() {
        serverShutdown = false;
        blockName = MineshaftService.i().getBlockMaterialName(player);
        started = true;

        int playerCount = getPlayers().size();
        targetBlocks = MIN_BLOCKS + (playerCount - 1) * 50;
    }

    public void tick() {
        if (!started) return;
        timer -= 50;

        // Check if left the mineshaft
        if (player.getTicks() % 20 == 0 && !MineshaftService.i().isInMineshaft(player)) {
            endGame();
            return;
        }

        // Spawn enemies
        if (nextWaveTicks-- < 0) {
            nextWaveTicks = 2400 + Game.getRandom().nextInt(200);
            waveCount++;

            for (GamePlayer pl : getPlayers()) {
                Player bukkitPlayer = pl.getPlayer();

                // Spawn phantoms
                for (int i = 0; i < waveCount && i < 2; i++) {
                    Phantom phantom = new Phantom(EntityType.PHANTOM, ((CraftWorld) bukkitPlayer.getWorld()).getHandle()) {
                        @Override
                        protected void registerGoals() {
                            super.registerGoals();

                            overrideTargetSelector(this);
                        }

                        @Override
                        public void playSound(SoundEvent soundeffect, float f, float f1) {
                            overridePlaySound(this, soundeffect, f, f1);
                        }

                        @Override
                        public void die(DamageSource damagesource) {
                            super.die(damagesource);

                            overrideDie(this, damagesource, Game.getRandom().nextBoolean() ? PotionEffectType.SPEED : null, 2400, 1);
                        }
                    };
                    spawnEntity(bukkitPlayer, phantom, true);
                }

                // Spawn pillagers/vindicators
                for (int i = 0; i < waveCount * 2 && i < 6; i++) {
                    Mob entity;
                    if (Game.getRandom().nextBoolean()) {
                        entity = new Pillager(EntityType.PILLAGER, ((CraftWorld) bukkitPlayer.getWorld()).getHandle()) {
                            @Override
                            protected void registerGoals() {
                                super.registerGoals();
                                overrideTargetSelector(this);
                            }

                            @Override
                            public void playSound(SoundEvent soundeffect, float f, float f1) {
                                overridePlaySound(this, soundeffect, f, f1);
                            }

                            @Override
                            public void die(DamageSource damagesource) {
                                super.die(damagesource);

                                overrideDie(this, damagesource, Game.getRandom().nextBoolean() ? PotionEffectType.DAMAGE_RESISTANCE : null, 2400, 1);
                            }
                        };
                        entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CROSSBOW));
                    } else {
                        entity = new Vindicator(EntityType.VINDICATOR, ((CraftWorld) bukkitPlayer.getWorld()).getHandle()) {
                            @Override
                            protected void registerGoals() {
                                super.registerGoals();
                                overrideTargetSelector(this);
                            }

                            @Override
                            public void playSound(SoundEvent soundeffect, float f, float f1) {
                                overridePlaySound(this, soundeffect, f, f1);
                            }

                            @Override
                            public void die(DamageSource damagesource) {
                                super.die(damagesource);

                                overrideDie(this, damagesource, Game.getRandom().nextBoolean() ? PotionEffectType.INCREASE_DAMAGE : null, 2400, 1);
                            }
                        };
                        entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                    }
                    spawnEntity(bukkitPlayer, entity, false);
                }
            }
        }

        for (GamePlayer pl : getPlayers()) {
            if (pl.isOffline()) continue;

            if (pl.getTicks() % 20 == 0) {
                // Update arrows
                ItemStack arrow = new ItemStack(Items.TIPPED_ARROW, 64);
                PotionUtils.setPotion(arrow, Potions.STRENGTH);
                pl.getPlayer().getInventory().setItem(9, CraftItemStack.asBukkitCopy(arrow));
            }

            // Update scoreboard
            ResponsiveScoreboard scoreboard = pl.getScoreboard();
            if (scoreboard != null) {
                ChatColor color = timer >= MAX_TIME / 2 ? ChatColor.GREEN : timer >= MAX_TIME / 4 ? ChatColor.GOLD : ChatColor.RED;
                scoreboard.setScore(0, color + "         " + Formatter.durationWithMilli(timer));

                scoreboard.setScore(2, ChatColor.GOLD + "Mine " + ChatColor.YELLOW + blockName + ChatColor.GOLD + " blocks");
                scoreboard.setScore(3, ChatColor.GREEN + "     " + (blocks < 10 ? "    " : blocks < 100 ? "  " : "") + blocks + "/" + MIN_BLOCKS);
                scoreboard.setScore(4, "     ");
            }
        }

        // Check game end
        if (timer <= 0 || blocks >= MIN_BLOCKS) {
            endGame();
        }
    }

    private void spawnEntity(Player player, Entity entity, boolean above) {
        Location loc = player.getLocation();
        List<Location> locations = new ArrayList<>();
        for (int y = (above ? 25 : 15); y >= (above ? 10 : 0); y--) {
            for (int x = -15; x <= 15; x++) {
                for (int z = -15; z <= 15; z++) {
                    if (Math.abs(x) == 15 || Math.abs(y) == 15 || Math.abs(z) == 15) {
                        Location l = player.getLocation().add(x, y, z);
                        if (l.getBlock().getType().equals(Material.AIR)) {
                            locations.add(l);
                        }
                    }
                }
            }
        }
        if (!locations.isEmpty()) {
            loc = locations.get(Game.getRandom().nextInt(locations.size()));
            if (!above) {
                Collections.shuffle(locations);
                for (Location location : locations) {
                    if (!location.getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
                        loc = location;
                        break;
                    }
                }
            }
        }
        entity.moveTo(loc.getX(), loc.getY(), loc.getZ());

        entities.add(entity);
        getPlayers().forEach(p -> p.getAllowedEntities().add(entity.getId()));
        entity.level().addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    public void reset() {
        // Reset variables
        timer = MAX_TIME;
        blocks = 0;
        started = false;
        waveCount = 0;
        nextWaveTicks = FIRST_WAVE_START;

        // Remove entities
        entities.forEach(Entity::kill);
        entities.clear();

        // Reset scoreboard
        for (GamePlayer pl : getPlayers()) {
            ResponsiveScoreboard scoreboard = pl.getScoreboard();
            if (scoreboard != null) {
                scoreboard.removeScore(0);
            }
            pl.updateScoreboard();
        }
    }

    public void endGame() {
        started = false;
        boolean leftGame = !MineshaftService.i().isInMineshaft(player);

        for (GamePlayer pl : getPlayers()) {
            pl.sendMessage(Strings.line());
            if (blocks >= targetBlocks) {
                pl.sendMessage(ChatColor.GREEN + Strings.middle("Mineshaft Complete!"));
                pl.sendMessage(ChatColor.GRAY + Strings.middle("You can now ascend in the lobby"));
            } else if (timer <= 0) {
                pl.sendMessage(ChatColor.RED + Strings.middle("Time's up. You lost"));
                pl.sendMessage(ChatColor.GRAY + Strings.middle("Try getting better next time"));
            } else {
                pl.sendMessage(ChatColor.RED + Strings.middle("Game over"));
                pl.sendMessage(ChatColor.GRAY + Strings.middle(pl == player ? "You left the game" : player.getName() + " left the game"));
            }
            pl.sendMessage(Strings.line());
        }

        Runner.sync(100, () -> {
            reset();

            if (leftGame) {
                List<GamePlayer> players = new ArrayList<>(getPlayers());
                if (!players.isEmpty()) {
                    if (player.isOnOwnIsland()) {
                        IslandModel island = player.getCurrentIsland(false);
                        for (int i = 1; i < players.size(); i++) {
                            players.get(i).tpToIsland(island, false);
                        }
                        players.get(0).tpToIsland(island, true);
                    }
                    // The case where the main player did not leave to their own island
                    //  is handled by the GamePlayer tick method
                }
            } else {
                player.tpToIsland(player.getCurrentIsland(false), true);
            }
        });
    }

    private Set<GamePlayer> getPlayers() {
        return player.getCurrentIsland().getAllPlayers();
    }

    public void onBreak() {
        if (blocks < targetBlocks) {
            blocks++;
        }
    }

    private void overrideDie(Mob entity, DamageSource damageSource, PotionEffectType additionalEffect, int duration, int amplitude) {
        if (damageSource.getEntity() != null) {
            GamePlayer damagePlayer = getPlayers().stream()
                    .filter(p -> p.getUuid().equals(damageSource.getEntity().getUUID()))
                    .findAny().orElse(null);
            if (damagePlayer != null) {
                damagePlayer.expSound();

                Player bukkitPlayer = damagePlayer.getPlayer();
                if (bukkitPlayer.getHealth() < 5) {
                    bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 40, 0, false, false, true));
                }
                bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 1, 0, false, false, true));

                if (additionalEffect != null) {
                    bukkitPlayer.addPotionEffect(new PotionEffect(additionalEffect, duration, amplitude, false, false, true));
                }
            }
        }

        int amount = Game.getRandom().nextInt(5) + 2;
        for (int i = 0; i < amount; i++) {
            DroppedItem.spawn(MineshaftService.i().getBlockMaterial(player), entity.getBukkitEntity().getLocation(), 40, getPlayers());
            onBreak();
        }
    }

    private void overrideTargetSelector(Mob entity) {
        entity.targetSelector.removeAllGoals((goal) -> true);
        entity.targetSelector.addGoal(1, new TargetSelector(entity));
    }

    private void overridePlaySound(Mob entity, SoundEvent soundeffect, float f, float f1) {
        if (!entity.isSilent()) {
            ;

            ClientboundSoundPacket packet = new ClientboundSoundPacket(Holder.direct(soundeffect), entity.getSoundSource(), entity.getX(), entity.getY(), entity.getZ(), f, f1, 0);
            getPlayers().stream().filter(p -> {
                Player player = p.getPlayer();
                if (player == null) return false;

                Location loc = player.getLocation();
                double x = entity.getX() - loc.getX();
                double y = entity.getY() - loc.getY();
                double z = entity.getZ() - loc.getZ();
                return x * x + y * y + z * z < f * f;
            }).forEach(p -> p.sendPacket(packet));
        }
    }

    class TargetSelector extends Goal {
        private final TargetingConditions targetCondition;
        private final Mob entity;
        private int timer;

        private TargetSelector(Mob entity) {
            this.entity = entity;
            this.targetCondition = TargetingConditions.forCombat().range(64);
            this.timer = 20;
        }

        @Override
        public boolean canUse() {
            if (this.timer > 0) {
                --this.timer;
            } else {
                this.timer = 60;
                List<net.minecraft.world.entity.player.Player> list = entity.level().getNearbyPlayers(targetCondition, entity, entity.getBoundingBox().expandTowards(16, 64, 16));
                if (!list.isEmpty()) {
                    Set<UUID> uuids = getPlayers().stream().map(PlayerId::getUuid).collect(Collectors.toSet());
                    List<net.minecraft.world.entity.player.Player> targets = list.stream()
                            .filter(e -> uuids.contains(e.getUUID()))
                            .sorted(Comparator.comparing(Entity::getBlockY).reversed())
                            .collect(Collectors.toList());

                    for (net.minecraft.world.entity.player.Player entityhuman : targets) {
                        if (entity.canAttack(entityhuman, TargetingConditions.DEFAULT)) {
                            entity.setTarget(entityhuman, EntityTargetEvent.TargetReason.CLOSEST_PLAYER, false);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = entity.getTarget();
            return target != null && entity.canAttack(target, TargetingConditions.DEFAULT);
        }
    }
}
