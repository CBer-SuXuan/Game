package net.mineclick.game.service;

import net.mineclick.core.messenger.Action;
import net.mineclick.game.messenger.TransactionsHandler;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.Transaction;
import net.mineclick.game.model.TransactionItem;
import net.mineclick.game.type.BoosterType;
import net.mineclick.game.type.Rarity;
import net.mineclick.global.Constants;
import net.mineclick.global.service.PlayersService;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.SingletonInit;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SingletonInit
public class TransactionsService {
    private static TransactionsService i;

    private TransactionsService() {
    }

    public static TransactionsService i() {
        return i == null ? i = new TransactionsService() : i;
    }

    /**
     * Process transactions
     *
     * @param uuid         Player's uuid
     * @param transactions The list of transaction
     */
    public void processTransactions(UUID uuid, List<Transaction> transactions) {
        PlayersService.i().<GamePlayer>get(uuid, player -> {
            AtomicInteger boosters = new AtomicInteger();
            AtomicInteger geodes = new AtomicInteger();
            for (Transaction transaction : transactions) {
                if (transaction.get_id() == null) continue;

                for (TransactionItem item : transaction.getItems()) {
                    if (item.getId() == null) continue;

                    if (item.getId().equals(Constants.PREMIUM_STORE_ID)) {
                        if (transaction.isCancelled()) {
                            if (transaction.getCancelledAt() == null || !transaction.getCancelledAt().before(new Date())) {
                                player.setRank(Rank.DEFAULT);
                                player.sendImportantMessage(
                                        "Your Premium membership was cancelled",
                                        "Contact staff if you believe this is an error"
                                );
                            }
                        } else {
                            player.setRank(Rank.PAID);
                            player.sendImportantMessage(
                                    "Premium membership applied!",
                                    "Thank you for supporting MineClick"
                            );
                        }
                    } else if (!transaction.isCancelled()) {
                        // process boosters
                        Arrays.stream(BoosterType.values())
                                .filter(b -> item.getId().equals(b.getStoreId()))
                                .findFirst()
                                .ifPresent(type -> {
                                    boosters.addAndGet(item.getQuantity());
                                    BoostersService.i().addBooster(player, type, item.getQuantity());
                                });

                        // process geodes
                        Arrays.stream(Rarity.values())
                                .filter(rarity -> item.getId().equals(rarity.getStoreId()))
                                .findFirst()
                                .ifPresent(rarity -> {
                                    geodes.addAndGet(item.getQuantity() * 10); // geodes are sold in bags of 10x
                                    GeodesService.i().addGeode(player, rarity, item.getQuantity() * 10);
                                });
                    }
                }

                if (!transaction.isCancelled()) {
                    transaction.setProcessed(true);
                    transaction.setProcessedAt(new Date());
                }
            }

            if (boosters.get() > 0) {
                player.sendImportantMessage("Processed " + boosters.get() + " booster(s) from the store!", "Check the Main menu");
            }
            if (geodes.get() > 0) {
                player.sendImportantMessage("Processed " + geodes.get() + " geodes from the store!", "Check the Powerups and Geodes menu");
            }

            TransactionsHandler handler = new TransactionsHandler();
            handler.setTransactions(transactions);
            handler.setUuid(uuid);
            handler.send(Action.UPDATE);
        });
    }

    /**
     * Check for unprocessed transactions and process if any
     *
     * @param player The player to check
     */
    public void checkTransactions(GamePlayer player) {
        TransactionsHandler handler = new TransactionsHandler();
        handler.setIsProcessed("false");
        handler.setUuid(player.getUuid());
        handler.setResponseConsumer(message -> {
            if (message == null) return;

            List<Transaction> transactions = ((TransactionsHandler) message).getTransactions();
            if (transactions != null && !transactions.isEmpty()) {
                processTransactions(player.getUuid(), transactions);
            }
        });

        handler.send(Action.GET);
    }
}
