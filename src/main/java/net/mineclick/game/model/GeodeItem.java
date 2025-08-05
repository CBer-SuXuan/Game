package net.mineclick.game.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import net.mineclick.game.type.Rarity;
import net.mineclick.global.util.ItemBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

@Value
@AllArgsConstructor
public class GeodeItem {
    String name;
    Rarity rarity;
    Consumer<GamePlayer> consumer;
    Function<GamePlayer, Boolean> duplicateFunction;
    double rarityOffset;
    ItemBuilder.ItemBuilderBuilder item;

    public GeodeItem(String name, Rarity rarity, Consumer<GamePlayer> consumer, Function<GamePlayer, Boolean> duplicateFunction) {
        this.name = name;
        this.rarity = rarity;
        this.consumer = consumer;
        this.duplicateFunction = duplicateFunction;
        rarityOffset = 1;
        item = null;
    }

    public GeodeItem(String name, Rarity rarity, Consumer<GamePlayer> consumer, Function<GamePlayer, Boolean> duplicateFunction, double rarityOffset) {
        this.name = name;
        this.rarity = rarity;
        this.consumer = consumer;
        this.duplicateFunction = duplicateFunction;
        this.rarityOffset = rarityOffset;
        item = null;
    }

    public GeodeItem(String name, Rarity rarity, Consumer<GamePlayer> consumer, Function<GamePlayer, Boolean> duplicateFunction, ItemBuilder.ItemBuilderBuilder item) {
        this.name = name;
        this.rarity = rarity;
        this.consumer = consumer;
        this.duplicateFunction = duplicateFunction;
        this.item = item;
        rarityOffset = 1;
    }
}
