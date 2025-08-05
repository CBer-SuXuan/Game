package net.mineclick.game.model;

import lombok.Data;

import java.util.Date;

@Data
public class Transaction {
    private String _id;
    private String email;
    private String uuid;
    private String name;
    private TransactionItem[] items;
    private boolean processed;
    private boolean cancelled;
    private Date createdAt;
    private Date nextPaymentAt;
    private Date processedAt;
    private Date cancelledAt;
}
