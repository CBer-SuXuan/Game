package net.mineclick.game.menu;

import net.mineclick.core.messenger.Action;
import net.mineclick.game.messenger.ReportsHandler;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.service.OffencesService;
import net.mineclick.global.type.OffenceType;
import net.mineclick.global.type.PunishmentType;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.MessageType;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;

class ReportMenu extends InventoryUI {
    ReportMenu(GamePlayer viewer, GamePlayer player) {
        super("Report " + player.getName(), 36);

        int i = 18;
        for (OffenceType offenceType : OffenceType.values()) {
            String reason = offenceType.getReason();

            ItemStack stack = ItemBuilder.builder()
                    .material(Material.YELLOW_STAINED_GLASS_PANE)
                    .title(ChatColor.GOLD + StringUtils.capitalize(reason))
                    .lore(ChatColor.GRAY + "Click to report for " + reason)
                    .build().toItem();
            ItemUI itemUI = new ItemUI(stack, click -> MenuUtil.openConfirmationMenu(
                    viewer,
                    confirm -> {
                        if (!confirm) {
                            new PlayerInfoMenu(viewer, player);
                            return;
                        }

                        viewer.getActivityData().setLastCreatedReport(Instant.now());
                        viewer.getPlayer().closeInventory();

                        if (viewer.getRank().isAtLeast(Rank.STAFF)) {
                            Pair<PunishmentType, Integer> suggestion = OffencesService.i().getSuggestion(player, offenceType);

                            new PunishmentMenu(viewer, player, suggestion, punishment -> {
                                OffencesService.i().punish(viewer, player, offenceType, punishment);
                                notifyDiscord(viewer, player, reason, punishment.key().toString().toLowerCase() + " (" + punishment.value() + ")");
                            });
                        } else {
                            viewer.sendMessage("Successfully reported " + player.getName() + " for: " + ChatColor.GRAY + reason, MessageType.INFO);
                            notifyDiscord(viewer, player, reason, null);
                        }
                    },
                    ChatColor.DARK_RED + "Reporting " + player.getName() + " for:",
                    ChatColor.RED + reason,
                    ChatColor.GRAY + (!viewer.getRank().isAtLeast(Rank.STAFF) ? "You may report once every 3 min" : "")
            ));
            setItem(i++, itemUI);
        }

        setItem(0, MenuUtil.getCloseMenu(p -> new PlayerInfoMenu(viewer, player)));
        open(viewer.getPlayer());
    }

    private void notifyDiscord(GamePlayer reporter, GamePlayer offender, String reason, String punishment) {
        ReportsHandler handler = new ReportsHandler();
        handler.setReporterName(reporter.getName());
        if (reporter.isRankAtLeast(Rank.STAFF) && reporter.getDiscordId() != null) {
            handler.setReporterDiscordId(reporter.getDiscordId());
        }
        handler.setReason(reason);
        handler.setPunishment(punishment);
        handler.setOffenderName(offender.getName());
        handler.setOffenderUuid(offender.getUuid().toString());

        handler.send(Action.POST);
    }
}
