package net.mineclick.game.messenger;

import lombok.Getter;
import lombok.Setter;
import net.mineclick.core.messenger.Action;
import net.mineclick.core.messenger.Message;
import net.mineclick.core.messenger.MessageName;
import net.mineclick.game.model.Transaction;
import net.mineclick.game.service.TransactionsService;
import net.mineclick.global.service.ServersService;
import net.mineclick.global.util.Runner;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@MessageName("transactions")
public class TransactionsHandler extends Message {
    private UUID uuid;
    private String isProcessed;
    private List<Transaction> transactions;

    @Override
    public void onReceive() {
        if (getAction().equals(Action.POST)) {
            // Allow ender to save the initial transaction first
            Runner.sync(10, () -> {
                if (ServersService.i().isShuttingDown()) return;

                TransactionsService.i().processTransactions(uuid, transactions);
            });
        }
    }
}
