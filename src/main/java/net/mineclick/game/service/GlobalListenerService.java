package net.mineclick.game.service;

import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Runner;
import net.mineclick.global.util.SingletonInit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.EntityHitResult;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftLivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@SingletonInit
public class GlobalListenerService implements Listener {
    private static GlobalListenerService i;

    private GlobalListenerService() {
        Bukkit.getPluginManager().registerEvents(this, Game.i());
    }

    public static GlobalListenerService i() {
        return i == null ? i = new GlobalListenerService() : i;
    }

    @EventHandler
    public void on(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void on(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void on(EntityDamageEvent event) {
        boolean inMineshaft = MineshaftService.i().isInMineshaft(event.getEntity().getLocation());
        event.setCancelled(!inMineshaft);

        if (event.getEntity() instanceof Player) {
            if (event.getDamage() >= ((Player) event.getEntity()).getHealth()) {
                event.setCancelled(true);
                ((Player) event.getEntity()).setHealth(((Player) event.getEntity()).getMaxHealth());

                if (inMineshaft) {
                    PlayersService.i().<GamePlayer>get(event.getEntity().getUniqueId(), player -> MineshaftService.i().onDeath(player));
                }
            }

            Runner.sync(() -> {
                event.getEntity().setFireTicks(0);

                if (event.getCause().equals(EntityDamageEvent.DamageCause.VOID)) {
                    respawn(event.getEntity().getUniqueId());
                }
            });
        }
    }

    @EventHandler
    public void on(PlayerMoveEvent event) {
        if (event.getTo().getY() < 0) {
            respawn(event.getPlayer().getUniqueId());
        }
    }

    private void respawn(UUID uuid) {
        PlayersService.i().<GamePlayer>get(uuid, player -> {
            if (LobbyService.i().isInLobby(player)) {
                LobbyService.i().spawn(player);
            } else if (MineshaftService.i().isInMineshaft(player)) {
                MineshaftService.i().respawn(player);
            } else if (player.getCurrentIsland() != null) {
                player.tpToIsland(player.getCurrentIsland(), false);
            } else {
                Player bukkitPlayer = player.getPlayer();
                bukkitPlayer.teleport(Game.i().getSpawn());
            }
        });
    }

    @EventHandler
    public void on(PlayerAnimationEvent event) {
        PlayersService.i().get(event.getPlayer().getUniqueId(), GamePlayer::onSwing);
    }

    @EventHandler
    public void on(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType().name().endsWith("_BUTTON")) {
                PlayersService.i().<GamePlayer>get(
                        event.getPlayer().getUniqueId(),
                        player -> AchievementsService.i().incrementProgress(player, "button", 1)
                );
            } else if (event.getClickedBlock().getType().equals(Material.STONECUTTER)) {
                PlayersService.i().<GamePlayer>get(event.getPlayer().getUniqueId(), player ->
                        LobbyService.i().checkGeodeCrusherClick(player, event.getClickedBlock()));
            }
        }

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            PlayersService.i().<GamePlayer>get(event.getPlayer().getUniqueId(), player -> {
                if (event.getClickedBlock() != null) {
                    player.getIgnoreClicks().incrementAndGet();

                    // clicking a chest will fire two swing events
                    Material type = event.getClickedBlock().getType();
                    if (type.equals(Material.CHEST) || type.equals(Material.TRAPPED_CHEST)) {
                        player.getIgnoreClicks().incrementAndGet();
                    }
                }

                if (event.getItem() != null) {
                    if (ItemBuilder.isSameTitle(event.getItem(), ParkourService.ITEM)) {
                        ParkourService.i().handleClick(player);
                    } else {
                        player.onItemClick(event.getItem());
                    }
                }
            });
        }

        if (event.getItem() != null && (event.getItem().getType().equals(Material.CROSSBOW) || event.getItem().getType().equals(Material.BOW))) {
            return;
        }
        event.setCancelled(event.getItem() == null || !event.getAction().equals(Action.RIGHT_CLICK_AIR));
    }

    @EventHandler
    public void on(EntityShootBowEvent e) {
        if (MineshaftService.i().isInMineshaft(e.getEntity().getLocation())) {
            MineshaftService.i().handleArrowSpawn(e);
            return;
        }

        e.setCancelled(true);
        if (e.getForce() < 0.1) return;

        if (e.getEntity() instanceof Player) {
            PlayersService.i().<GamePlayer>get(e.getEntity().getUniqueId(), playerModel -> {
                playerModel.playSound(Sound.ENTITY_ARROW_SHOOT, e.getForce(), 1);
                playerModel.setArrows(playerModel.getArrows() - 1);
                playerModel.updateInventory();

                Runner.sync(() -> {
                    LivingEntity player = ((CraftLivingEntity) e.getEntity()).getHandle();
                    Arrow arrow = new Arrow(((CraftWorld) e.getEntity().getWorld()).getHandle(), player) {
                        @Override
                        public void tick() {
                            if (inGround) {
                                discard();
                                return;
                            }

                            super.tick();
                        }

                        @Override
                        protected void onHitEntity(EntityHitResult movingobjectpositionentity) {
                            movingobjectpositionentity.getEntity().hurt(damageSources().arrow(this, this.getOwner()), 1F);
                        }
                    };
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    arrow.setSilent(true);
                    arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, e.getForce() * 3.0F, 1.0F);

                    playerModel.getAllowedEntities().add(arrow.getId());
                    arrow.level().addFreshEntity(arrow);
                });
            });
        }
    }

    @EventHandler
    public void on(EntityPickupItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void on(PlayerDropItemEvent event) {
        event.getItemDrop().remove();

        PlayersService.i().<GamePlayer>get(event.getPlayer().getUniqueId(),
                playerModel -> playerModel.onItemDrop(event.getItemDrop().getItemStack()));
    }

    @EventHandler
    public void on(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void on(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item != null) {
            PlayersService.i().<GamePlayer>get(event.getWhoClicked().getUniqueId(), player -> {
                if (ItemBuilder.isSameTitle(item, ParkourService.RUNNING_SHOES)) {
                    player.getParkour().setShoesRemoved(!player.getParkour().isShoesRemoved());
                } else if (ItemBuilder.isSameTitle(item, ParkourService.ELYTRA)) {
                    player.getParkour().setElytraRemoved(!player.getParkour().isElytraRemoved());
                }

                // holding Q with inventory open exploit fix
                if (event.getAction().equals(InventoryAction.DROP_ONE_SLOT)) {
                    player.getIgnoreClicks().incrementAndGet();
                }
                player.updateInventory();
            });
        }
    }

    @EventHandler
    public void on(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void on(HangingBreakEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void on(BlockFadeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void on(PlayerCommandPreprocessEvent e) {
        PlayersService.i().<GamePlayer>get(e.getPlayer().getUniqueId(), playerModel -> {
            switch (e.getMessage()) {
                case "/discord":
                    AchievementsService.i().incrementProgress(playerModel, "discord", 1);
                    break;
                case "/wiki":
                    AchievementsService.i().incrementProgress(playerModel, "wiki", 1);
                    break;
            }
        });
    }
}
