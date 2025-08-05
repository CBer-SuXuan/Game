package net.mineclick.game.menu;

import lombok.Setter;
import net.mineclick.game.model.BuildingModel;
import net.mineclick.game.model.GamePlayer;
import net.mineclick.game.model.IslandModel;
import net.mineclick.game.model.pickaxe.Pickaxe;
import net.mineclick.game.model.pickaxe.PickaxeConfiguration;
import net.mineclick.game.model.worker.Worker;
import net.mineclick.game.model.worker.WorkerConfiguration;
import net.mineclick.game.service.AscensionServices;
import net.mineclick.game.service.IslandsService;
import net.mineclick.game.service.WorkersService;
import net.mineclick.game.type.worker.WorkerType;
import net.mineclick.game.util.MenuUtil;
import net.mineclick.global.config.IslandConfig;
import net.mineclick.global.config.field.IslandUnlockRequired;
import net.mineclick.global.type.Rank;
import net.mineclick.global.util.BigNumber;
import net.mineclick.global.util.Formatter;
import net.mineclick.global.util.ItemBuilder;
import net.mineclick.global.util.ui.InventoryUI;
import net.mineclick.global.util.ui.ItemUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

public class UpgradesMenu extends InventoryUI {
    public static final ItemStack MENU_ITEM = ItemBuilder.builder()
            .material(Material.CHEST)
            .title(ChatColor.YELLOW + "Upgrades Menu" + ChatColor.GRAY + "" + ChatColor.ITALIC + " right-click")
            .build().toItem();

    private int selectMax = 1;

    @Setter
    private boolean visiting;

    public UpgradesMenu(GamePlayer player) {
        super("         MineClick Upgrades", 54);

        for (int i = 0; i < 4; i++) {
            int pos;
            int max;
            String title;
            switch (i) {
                default:
                case 0:
                    pos = 8;
                    max = 1;
                    title = ChatColor.GREEN + "Buy 1 level";
                    break;
                case 1:
                    pos = 17;
                    max = 10;
                    title = ChatColor.GREEN + "Buy 10 levels at max";
                    break;
                case 2:
                    pos = 26;
                    max = 50;
                    title = ChatColor.GREEN + "Buy 50 levels at max";
                    break;
                case 3:
                    pos = 35;
                    max = -1;
                    title = ChatColor.GREEN + "Buy maximum levels";
                    break;
            }

            ItemUI itemUI = new ItemUI(ItemBuilder.builder().material(Material.GRAY_DYE).title(title).build().toItem(), pack -> {
                player.clickSound();
                selectMax = max;
            });
            itemUI.setUpdateConsumer(item -> {
                if (selectMax == max) {
                    item.setMaterial(Material.LIME_DYE);
                    item.setLore(ChatColor.GRAY + "Selected");
                } else {
                    item.setMaterial(Material.GRAY_DYE);
                    item.setLore();
                }
            });
            setItem(pos, itemUI);
        }

        //Pickaxe
        ItemUI pickaxeItemUI = new ItemUI(ItemBuilder.builder().material(Material.WOODEN_PICKAXE).build().toItem(), clickPack -> {
            Pickaxe pickaxe = player.getPickaxe();

            long amount = pickaxe.maxCanBuy(selectMax);
            if (selectMax > 0) {
                amount = Math.min(amount, selectMax);
            }

            if (amount > 0 && player.chargeGold(pickaxe.cost(amount))) {
                player.popSound();
                pickaxe.increaseLevel(amount);

                if (player.getTutorialVillager() != null) {
                    player.getTutorialVillager().onPickaxeUpgrade();
                }
            } else {
                player.noSound();
            }
        });
        pickaxeItemUI.setUpdateConsumer(item -> {
            Pickaxe pickaxe = player.getPickaxe();
            PickaxeConfiguration configuration = pickaxe.getConfiguration();

            long amount = pickaxe.maxCanBuy(selectMax);
            BigNumber cost = amount > 0 ? pickaxe.cost(amount) : pickaxe.cost(1);

            if (configuration.isGlow()) {
                item.setGlowing();
            } else {
                item.removeGlowing();
            }

            if (pickaxe.getJustUpgradedTicks() > 0) {
                item.setMaterial(Material.GREEN_STAINED_GLASS_PANE);
            } else {
                item.setMaterial(configuration.getMaterial());
            }
            item.setTitle((amount > 0 ? ChatColor.YELLOW : ChatColor.GOLD) + configuration.getName() + ChatColor.GREEN + " lvl " + pickaxe.getLevel());
            item.setLore();
            item.addLore(" ");

            String loreAdd = ChatColor.BOLD + (amount > 0 && selectMax != 1 ? " (✕" + amount + ")" : "");
            item.addLore(ChatColor.GRAY + "Upgrade:" + loreAdd);
            item.addLore(MenuUtil.prerequisiteGold(player, cost, " Cost: "));

            BigNumber diff = pickaxe.getIncomeDiff(Math.max(1, amount));
            item.addLore(ChatColor.GRAY + " Gold/click: " + ChatColor.GREEN + "+" + ChatColor.YELLOW + diff.print(player));

            item.addLore(" ");
            item.addLore(ChatColor.GRAY + "Pickaxe stats:");
            item.addLore(ChatColor.GRAY + " Gold/click: " + ChatColor.YELLOW + pickaxe.getIncome().print(player));
//            item.addLore(ChatColor.GRAY + " GPC multiplier: " + ChatColor.YELLOW + pickaxe.getGoldMultiplier());
            item.addLore(ChatColor.GRAY + " Amount: " + ChatColor.YELLOW + pickaxe.getAmount());
        });
        setItem(13, pickaxeItemUI);

        //Workers
        ListIterator<WorkerType> iterator = Arrays.asList(WorkerType.values()).listIterator();
        for (int row = 2; row < 4; row++) {
            for (int col = 2; col < 7; col++) {
                WorkerType type = iterator.hasNext() ? iterator.next() : null;
                if (type == null || !WorkersService.i().getConfigurations().containsKey(type))
                    break;

                ItemUI itemUI = new ItemUI(Material.PLAYER_HEAD, clickPack -> {
                    if ((!player.getTutorial().isComplete() && !player.getTutorial().isShowUpgradesMenuWorkers()) || visiting)
                        return;

                    int highest = player.getWorkers().isEmpty() ? 0 : player.getWorkers().size();
                    if (type.ordinal() > highest)
                        return;

                    Worker worker = player.getWorkers().get(type);
                    if (worker != null) {
                        long amount = worker.maxCanBuy(selectMax);
                        if (selectMax > 0) {
                            amount = Math.min(amount, selectMax);
                        }

                        if (amount > 0 && player.chargeGold(worker.cost(amount))) {
                            player.popSound();
                            worker.increaseLevel(amount);
                            player.recalculateGoldRate();

                            if (player.getTutorialVillager() != null) {
                                player.getTutorialVillager().onWorkerUpgrade();
                            }
                        } else {
                            player.noSound();
                        }
                    } else {
                        WorkerConfiguration configuration = WorkersService.i().getConfigurations().get(type);
                        if (player.chargeGold(configuration.getBaseCost())) {
                            WorkersService.i().unlockWorker(player, type);
                            schedule(2, () -> player.getWorkers().values().forEach(Worker::recalculateIncomePercent));
                            player.recalculateGoldRate();
                            player.levelUpSound();

                            if (player.getTutorialVillager() != null) {
                                player.getTutorialVillager().onWorkerUpgrade();
                            }
                        }
                    }
                });
                itemUI.setUpdateConsumer(item -> {
                    if (!player.getTutorial().isComplete() && !player.getTutorial().isShowUpgradesMenuWorkers()) {
                        item.setMaterial(Material.AIR);
                        return;
                    } else if (visiting) {
                        MenuUtil.setVisitingLockedSkull(item);
                        return;
                    } else {
                        MenuUtil.setLockedSkull(item);
                    }

                    Worker worker = player.getWorkers().get(type);
                    WorkerConfiguration configuration = WorkersService.i().getConfigurations().get(type);

                    int highest = player.getWorkers().isEmpty() ? 0 : player.getWorkers().size();
                    if (type.ordinal() <= highest) {
                        if (worker != null && worker.getJustUpgradedTicks() > 0) {
                            item.setMaterial(Material.GREEN_STAINED_GLASS_PANE);
                        } else {
                            configuration.setHeadItem(item);
                        }
                    } else {
                        return;
                    }

                    if (worker != null) {
                        long amount = worker.maxCanBuy(selectMax);
                        BigNumber cost = amount > 0 ? worker.cost(amount) : worker.cost(1);

                        item.setTitle((amount > 0 ? ChatColor.YELLOW : ChatColor.GOLD) + configuration.getName() + ChatColor.GREEN + " lvl " + worker.getLevel());
                        item.setLore(" ");

                        String loreAdd = ChatColor.BOLD + (amount > 0 && selectMax != 1 ? " (✕" + amount + ")" : "");
                        item.addLore(ChatColor.GRAY + "Upgrade:" + loreAdd);
                        item.addLore(MenuUtil.prerequisiteGold(player, cost, " Cost: "));
                        item.addLore(ChatColor.GRAY + " Gold/sec: " + ChatColor.GREEN + "+" + ChatColor.YELLOW + configuration.getBaseIncome().multiply(new BigNumber(String.valueOf(Math.max(1, amount) * worker.getTotalMultiplier()))).print(player));
                    } else {
                        item.setTitle(ChatColor.RED + configuration.getName());
                        boolean canBuy = player.getGold().greaterThanOrEqual(configuration.getBaseCost());

                        if (canBuy) {
                            item.setLore(ChatColor.GOLD + ChatColor.BOLD.toString() + "Click to unlock!");
                        } else {
                            item.setLore(ChatColor.GRAY + ChatColor.BOLD.toString() + "Locked!");
                        }
                        item.addLore(" ");
                        item.addLore(ChatColor.GRAY + "Required to unlock:");

                        item.addLore(MenuUtil.prerequisiteGold(player, configuration.getBaseCost(), " "));
                    }

                    BigNumber income = worker != null ? worker.getIncome() : configuration.getBaseIncome();
                    int percent = worker != null ? worker.getIncomePercent() : 0;
                    item.addLore(" ");
                    item.addLore(ChatColor.GRAY + "Worker stats:");
                    item.addLore(ChatColor.GRAY + " Gold/sec: " + income.print(player) + ChatColor.GRAY
                            + (worker != null ? " (" + (percent == 0 ? "<1" : percent) + "%)" : ""));
                });

                setItem(row * 9 + col, itemUI);
            }
        }

        //Islands
        List<IslandConfig> islands = player.getDimensionsData().getDimension().getIslands();
        int index = 0;
        for (int col = 2; col < 7; col++) {
            if (index >= islands.size()) continue;
            IslandConfig islandConfig = islands.get(index);

            int id = index;
            ItemStack stack = ItemBuilder.builder()
                    .material(Material.BLACK_WOOL)
                    .title(ChatColor.DARK_GRAY + "???")
                    .lore(ChatColor.GRAY + "Can't unlock yet")
                    .build().toItem();
            ItemUI itemUI = new ItemUI(stack, pack -> {
                if ((!player.getTutorial().isComplete() && !player.getTutorial().isShowUpgradesMenuIslands()) || visiting || id > player.getIslands().size())
                    return;

                if (pack.event().getClick().equals(ClickType.LEFT)) {
                    if (id == player.getIslands().size()) {
                        if (IslandsService.i().hasPrerequisites(player, islandConfig) && player.chargeGold(islandConfig.getBaseCost())) {
                            IslandsService.i().unlockIsland(player, id);
                            player.levelUpSound();
                            pack.itemUI().removeGlowing();
                        }
                    } else {
                        player.tpToIsland(player.getIslands().get(id), !player.isOnOwnIsland());
                    }
                }
            });
            itemUI.setUpdateConsumer(item -> {
                if (!player.getTutorial().isComplete() && !player.getTutorial().isShowUpgradesMenuIslands()) {
                    item.setMaterial(Material.AIR);
                    return;
                } else if (visiting) {
                    MenuUtil.setVisitingLockedSkull(item);
                    return;
                } else {
                    item.setMaterial(Material.BLACK_WOOL);
                }

                if (id > player.getIslands().size()) {
                    item.setTitle(ChatColor.DARK_GRAY + "???");
                    item.setLore(ChatColor.GRAY + "Can't unlock yet");
                    return;
                }

                if (id < player.getIslands().size()) {
                    boolean current = player.getCurrentIslandId() == id;
                    item.setTitle(ChatColor.YELLOW + islandConfig.getName());
                    item.setLore(ChatColor.GOLD + (current ? "Current island" : "Click to teleport"));
                    item.setMaterial(current ? Material.LIME_WOOL : Material.GREEN_WOOL);
                } else {
                    item.setTitle(ChatColor.RED + islandConfig.getName());

                    boolean canBuy = player.getGold().greaterThanOrEqual(islandConfig.getBaseCost()) && IslandsService.i().hasPrerequisites(player, islandConfig);

                    if (canBuy) {
                        item.setLore(ChatColor.GOLD + ChatColor.BOLD.toString() + "Click to unlock!");
                        item.setMaterial(Material.YELLOW_WOOL);
                        item.setGlowing();
                    } else {
                        item.setLore(ChatColor.GRAY + ChatColor.BOLD.toString() + "Locked!");
                        item.setMaterial(Material.RED_WOOL);
                        item.removeGlowing();
                    }
                    item.addLore(" ");
                    item.addLore(ChatColor.GRAY + "Required to unlock:");

                    item.addLore(MenuUtil.prerequisiteGold(player, islandConfig.getBaseCost(), " "));
                    for (IslandUnlockRequired required : islandConfig.getUnlockRequired()) {
                        WorkerType type = WorkerType.ofMobType(required.getMobType());
                        item.addLore(MenuUtil.prerequisiteWorker(player, type, required.getLevel(), " "));
                    }
                }

                item.addLore(" ");
                item.addLore(ChatColor.GRAY + "Island stats:");
                item.addLore(ChatColor.GRAY + " Gold multiplier: " + ChatColor.GREEN + "+" + ChatColor.YELLOW + Formatter.format(islandConfig.getMultiplier()));
                if (id < player.getIslands().size()) {
                    IslandModel island = player.getIslands().get(id);
                    long count = island.getBuildings().stream().mapToInt(BuildingModel::getLevel).sum();
                    long total = island.getBuildings().stream().mapToInt(buildingModel -> buildingModel.getConfig().getNames().size()).sum();
                    item.addLore(ChatColor.GRAY + " Buildings unlocked: " + (count == total ? ChatColor.GREEN : ChatColor.YELLOW) + count + "/" + total);
                    item.addLore(ChatColor.GRAY + " Parkour complete: " + (player.getParkour().getCompletedIslands().contains(id) ? ChatColor.GREEN + "yes" : ChatColor.RED + "no"));
                }
            });

            setItem(45 + col, itemUI);
            index++;
        }

        // Ascend item
        ItemUI ascendItem = new ItemUI(Material.AIR);
        ascendItem.setUpdateConsumer(new Consumer<>() {
            int ticks = 100;

            @Override
            public void accept(ItemUI itemUI) {
                if (ticks++ < 100) return;
                ticks = 0;

                boolean canAscend = AscensionServices.i().hasMinimumGold(player);
                if (!canAscend && player.getDimensionsData().getAscensionsTotal() <= 0) return;

                itemUI.setMaterial(Material.END_PORTAL_FRAME);
                itemUI.setTitle((canAscend ? ChatColor.GREEN : ChatColor.RED) + "Ascension");
                itemUI.setLore(" ");
                if (canAscend) {
                    itemUI.addLore(ChatColor.GRAY + "Ascend now and receive:");
                    itemUI.addLore(ChatColor.GREEN + "+" + ChatColor.AQUA + AscensionServices.i().getAscendSchmepls(player) + ChatColor.YELLOW + " schmepls");
                    itemUI.addLore(ChatColor.GREEN + "+" + ChatColor.AQUA + AscensionServices.i().getAscendExp(player) + ChatColor.YELLOW + " EXP");
                } else {
                    itemUI.addLore(ChatColor.GRAY + "You need to make " + AscensionServices.i().getAscendRequiredGold(player).print(player));
                    itemUI.addLore(ChatColor.GRAY + "more gold to ascend");
                }
                itemUI.addLore(" ");
                itemUI.addLore(ChatColor.GRAY + "Click for details");
            }
        });
        ascendItem.setClickConsumer(inventoryClickPack -> {
            if (player.getDimensionsData().getAscensionsTotal() <= 0 && !AscensionServices.i().hasMinimumGold(player))
                return;

            player.clickSound();
            AscensionServices.i().openAscendMenu(player, (p) -> p.getUpgradesMenu().open(p.getPlayer()));
        });
        setItem(53, ascendItem);

        // Auto upgrade
        ItemUI autoUpgradeItem = new ItemUI(Material.AIR);
        autoUpgradeItem.setUpdateConsumer(itemUI -> {
            if (!player.getTutorial().isComplete()) {
                itemUI.setMaterial(Material.AIR);
                return;
            }

            boolean enabled = player.isAutoUpgradeEnabled();
            itemUI.setMaterial(enabled ? Material.LIME_SHULKER_BOX : Material.RED_SHULKER_BOX);
            itemUI.setTitle((enabled ? ChatColor.GREEN : ChatColor.RED) + "Auto upgrade");
            itemUI.setLore("");
            itemUI.addLore(ChatColor.GRAY + "Automatically unlock/upgrade");
            itemUI.addLore(ChatColor.GRAY + "workers and pickaxe");

            itemUI.addLore(" ");
            if (!player.getRank().isAtLeast(Rank.PAID)) {
                itemUI.addLore(ChatColor.RED + "Requires Premium Membership");
                itemUI.addLore(ChatColor.GRAY + "Get yours at " + ChatColor.AQUA + "store.mineclick.net");
            } else {
                itemUI.addLore(ChatColor.GOLD + "Click to " + (enabled ? "disable" : "enable"));
            }
        });
        autoUpgradeItem.setClickConsumer(inventoryClickPack -> {
            if (!player.getTutorial().isComplete() || !player.getRank().isAtLeast(Rank.PAID)) {
                return;
            }

            player.clickSound();
            player.setAutoUpgradeEnabled(!player.isAutoUpgradeEnabled());
        });
        setItem(45, autoUpgradeItem);

        setItem(0, MenuUtil.getCloseMenu());
        setDestroyOnClose(false);
    }
}
