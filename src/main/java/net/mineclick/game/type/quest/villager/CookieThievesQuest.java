package net.mineclick.game.type.quest.villager;

import lombok.Getter;
import net.mineclick.game.Game;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.QuestProgress;
import net.mineclick.game.service.LobbyService;
import net.mineclick.game.service.QuestsService;
import net.mineclick.game.type.quest.QuestObjective;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ParticlesUtil;
import net.mineclick.global.util.Triple;
import net.mineclick.global.util.location.LocationParser;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

public class CookieThievesQuest extends VillagerQuest {
    private static final List<Location> LOCATIONS = List.of(
            LocationParser.parse("128.5 106 -1015.5"),
            LocationParser.parse("63.5 94 -1041.5"),
            LocationParser.parse("34.5 94 -1089.5"),
            LocationParser.parse("-16.5 103 -1077.5"),
            LocationParser.parse("-21.5 106 -1014.5"),
            LocationParser.parse("-84.5 97 -971.5"),
            LocationParser.parse("70.5 98 -961.5"),
            LocationParser.parse("98.5 95 -912.5"),
            LocationParser.parse("46.5 103 -923.5"),
            LocationParser.parse("119.5 97 -968.5")
    );

    @Getter
    public List<QuestObjective> objectives = List.of(
            new QuestObjective.Talk(getVillagerName(), true) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        sendVillagerMessage(player, "Quick! Go after them");
                        return;
                    }

                    for (int i = 0; i < LOCATIONS.size(); i++) {
                        spawnZombie(player, getVillagerLocation(), true);
                    }

                    sendVillagerMessage(player, "Nooo! They did it again!");
                    sendVillagerMessage(player, 60, "Those little zombies keep stealing my cookies...");
                    sendVillagerMessage(player, 160, "Please help me find them, they're probably hiding in someone's house");

                    player.schedule(220, () -> completeObjective(getQuestProgress(player)));
                }
            },
            new QuestObjective("Find cookie thieves", LOCATIONS.size()) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    cooldown(player, 80);
                    sendVillagerMessage(player, "Quick! Go after them");

                    QuestProgress progress = getQuestProgress(player);
                    if (progress != null) {
                        sendVillagerMessage(player, 40, "You still need to catch " + (getValue(player) - progress.getTaskProgress()) + " baby zombies");
                    }
                }

                @Override
                public boolean sendCompleteObjective() {
                    return true;
                }
            },
            new QuestObjective.Talk(getVillagerName(), false) {
                @Override
                public void onVillagerClick(GamePlayer player, boolean talkedBefore) {
                    if (talkedBefore) {
                        return;
                    }

                    sendVillagerMessage(player, "Thank you so much! Hopefully they won't steal my cookies again");
                    sendVillagerMessage(player, 80, "By the way, come by once in a while and I'll give you some cookies");
                    sendVillagerMessage(player, 180, "See ya!");

                    player.schedule(220, () -> completeObjective(getQuestProgress(player)));
                }
            }
    );

    @Override
    public void onVillagerClickAfterComplete(GamePlayer player) {
        long now = System.currentTimeMillis();
        long next = player.getQuestsData().getCookieThievesData().getNextCookiesDelivery().toEpochMilli();
        if (now > next) {
            sendVillagerMessage(player, "Here ya go! " + ChatColor.AQUA + "+20 " + ChatColor.YELLOW + "cookies");
            player.addCookies(20);
            player.popSound();

            player.getQuestsData().getCookieThievesData().setNextCookiesDelivery(Instant.now().plus(2, ChronoUnit.HOURS));
        } else {
            cooldown(player, 100);

            sendVillagerMessage(player, "Sorry, cookies are still in the oven");
            sendVillagerMessage(player, 60, "Come back in " + Formatter.duration(next - now));

            player.noSound();
        }
    }

    public void checkZombies(GamePlayer player) {
        if (LobbyService.i().isInLobby(player)) {
            QuestProgress progress = getQuestProgress(player);
            if (progress == null || progress.getObjective() != 1) {
                return;
            }

            Location pLoc = player.getPlayer().getLocation();
            for (Location location : LOCATIONS) {
                if (player.getQuestsData().getCookieThievesData().getCaughtZombies().contains(location)) {
                    continue;
                }
                boolean closeBy = pLoc.distanceSquared(location) < 400;

                if (closeBy) {
                    if (!player.getQuestsData().getCookieThievesData().getZombies().containsKey(location)) {
                        Zombie zombie = spawnZombie(player, location, false);
                        player.getQuestsData().getCookieThievesData().getZombies().put(location, zombie);
                    }
                } else {
                    Zombie zombie = player.getQuestsData().getCookieThievesData().getZombies().remove(location);
                    if (zombie != null) {
                        zombie.discard();
                    }
                }
            }
        }
    }

    private Zombie spawnZombie(GamePlayer player, Location location, boolean moving) {
        Zombie zombie = new Zombie(EntityType.ZOMBIE, ((CraftWorld) location.getWorld()).getHandle()) {
            @Override
            public void tick() {
                if (player.isOffline() || (moving && tickCount > 60)) {
                    discard();
                    return;
                }

                if (!moving) {
                    if (tickCount % 60 == 0) {
                        player.playSound(Sound.ENTITY_GENERIC_EAT, location, 1, 1);
                    }

                    Location pLoc = player.getPlayer().getLocation();
                    Vector dir = pLoc.toVector().subtract(new Vector(getX(), getY(), getZ())).normalize();

                    double theta = Math.atan2(-dir.getX(), dir.getZ());
                    float yaw = (float) Math.toDegrees((theta + 6.283185307179586D) % 6.283185307179586D);
                    int i = Mth.floor(yaw * 256.0F / 360.0F);
                    player.sendPacket(new ClientboundRotateHeadPacket(this, (byte) i));
                }

                super.tick();
            }

            @Override
            public void remove(RemovalReason removalReason) {
                super.remove(removalReason);

                player.getAllowedEntities().remove(getId());
            }

            @Override
            protected void registerGoals() {
                if (moving) {
                    this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1, 1));
                }
            }

            @Override
            protected InteractionResult mobInteract(Player entityhuman, InteractionHand enumhand) {
                if (!moving) {
                    QuestsService.i().incrementProgress(player, CookieThievesQuest.this.getId(), 1, 1);
                    player.getQuestsData().getCookieThievesData().getCaughtZombies().add(location);
                    player.expSound();

                    ParticlesUtil.send(ParticleTypes.CLOUD, getBukkitEntity().getLocation(), Triple.of(0.1F, 0.5F, 0.1F), 0, 20, Collections.singleton(player));

                    discard();
                }

                return InteractionResult.PASS;
            }
        };
        zombie.setSilent(true);
        zombie.setBaby(true);
        zombie.setInvulnerable(true);
        zombie.setItemInHand(InteractionHand.MAIN_HAND, CraftItemStack.asNMSCopy(new ItemStack(Material.COOKIE)));
        if (moving) {
            zombie.moveTo(location.getX() + Game.getRandom().nextDouble() * 2 - 1, location.getY(), location.getZ() + Game.getRandom().nextDouble() * 2 - 1);
        } else {
            zombie.moveTo(location.getX(), location.getY(), location.getZ(), Game.getRandom().nextFloat() * 360, 0);
        }

        player.getAllowedEntities().add(zombie.getId());
        zombie.level().addFreshEntity(zombie, CreatureSpawnEvent.SpawnReason.CUSTOM);

        return zombie;
    }

    @Override
    public String getId() {
        return "cookieThieves";
    }

    @Override
    public String getName(GamePlayer player) {
        return "Cookie thieves";
    }

    @Override
    public int getExpReward(GamePlayer player) {
        return 140;
    }

    @Override
    public int getSchmeplsReward(GamePlayer player) {
        return 300;
    }

    @Override
    public String getVillagerName() {
        return "SpeedRider";
    }

    @Override
    public Pair<VillagerType, VillagerProfession> getVillagerType() {
        return Pair.of(VillagerType.SNOW, VillagerProfession.FLETCHER);
    }

    @Override
    public Location getVillagerLocation() {
        return LocationParser.parse("113.5 97 -970.5");
    }

    @Override
    public boolean isVisible(GamePlayer player) {
        return true;
    }
}
