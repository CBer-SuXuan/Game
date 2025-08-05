package net.mineclick.game.model;

import lombok.Data;

@Data
public class TransactionItem {
    private String id;
    private String description;
    private boolean subscription;
    private int quantity;
    private String price;
}
