package net.mineclick.game.menu;

import net.mineclick.game.model.GamePlayer;
import net.mineclick.global.type.PunishmentType;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.Pair;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.function.Consumer;

public class PunishmentMenu extends InventoryUI {
    PunishmentMenu(GamePlayer viewer, GamePlayer player, Pair<PunishmentType, Integer> suggestion, Consumer<Pair<PunishmentType, Integer>> consumer) {
        super("Punish " + player.getName(), 54);

        int row = 10;
        for (PunishmentType punishmentType : PunishmentType.values()) {
            int pos = row;
            ItemUI typeItem = new ItemUI(ItemBuilder.builder().material(Material.RED_WOOL).title(ChatColor.GOLD + punishmentType.toString()));
            setItem(pos, typeItem);

            pos++;
            for (int duration : punishmentType.getDurations()) {
                boolean isSuggested = suggestion.key().equals(punishmentType) && suggestion.value().equals(duration);
                Material material = isSuggested ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE;

                ItemBuilder.ItemBuilderBuilder builder = ItemBuilder.builder()
                        .material(material)
                        .title(ChatColor.YELLOW + punishmentType.toString());
                if (isSuggested) {
                    builder.lore(ChatColor.GREEN + "SUGGESTED");
                }
                builder.lore(" ");
                if (duration != 0) {
                    builder.lore(ChatColor.GRAY + punishmentType.toString().toLowerCase() + " this player for " + (duration == -1 ? "good" : duration + " minutes"));
                }

                ItemUI item = new ItemUI(builder);
                item.setClickConsumer(inventoryClickPack -> {
                    consumer.accept(Pair.of(punishmentType, duration));
                    viewer.clickSound();
                    viewer.getPlayer().closeInventory();
                });
                setItem(pos++, item);
            }

            row += 9;
        }

        open(viewer.getPlayer());
    }
}
