package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 背包类：管理物品存储、物品增删查改
 */
@Getter
@Setter
public class Bag {
    private List<Item> inventory;

    /** 持有玩家引用，用于判断物品是否已佩戴 */
    private Player owner;

    public Bag() {
        this.inventory = new ArrayList<>();
        this.owner = null;
    }

    /**
     * 添加物品到背包（无重量限制）
     */
    public void addItem(Item item) {
        inventory.add(item);
    }

    /**
     * 从背包移除物品
     */
    public boolean removeItem(Item item) {
        return inventory.remove(item);
    }

    /**
     * 根据名称在背包中查找物品（返回第一个匹配的）
     */
    public Item getItem(String itemName) {
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 获取背包物品列表。
     * 饰品（披风/戒指/项链）和装备（剑/盾等）不合并，每个单独占一个格子，数量始终为1；
     * 普通消耗品按名称合并统计数量。
     */
    public List<InventoryItem> getBackpackItems() {
        List<InventoryItem> result = new ArrayList<>();
        int idCounter = 1;

        // 先处理所有可装备物品（饰品+装备，不合并，每个单独占格子）
        List<Item> equippable = new ArrayList<>();
        List<Item> consumables = new ArrayList<>();

        for (Item item : inventory) {
            if (Player.getItemSlot(item.getName()) != null) {
                equippable.add(item);
            } else {
                consumables.add(item);
            }
        }

        // 可装备物品：每个单独占一格，同名物品中仅第一个佩戴标志为true
        Set<String> alreadyMarkedEquipped = new HashSet<>();
        for (Item eq : equippable) {
            boolean shouldShowEquipped = (owner != null)
                && owner.isEquipped(eq.getName())
                && !alreadyMarkedEquipped.contains(eq.getName());
            if (shouldShowEquipped) {
                alreadyMarkedEquipped.add(eq.getName());
            }
            InventoryItem invItem = InventoryItem.fromItem(eq, 1, idCounter++, shouldShowEquipped);
            result.add(invItem);
        }

        // 消耗品：按名称合并
        Map<String, List<Item>> grouped = consumables.stream()
                .collect(Collectors.groupingBy(Item::getName));

        for (Map.Entry<String, List<Item>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<Item> items = entry.getValue();
            InventoryItem invItem = InventoryItem.fromItem(items.get(0), items.size(), idCounter++, false);
            result.add(invItem);
        }

        return result;
    }

    /**
     * 使用物品（数量-1，若为0则移除）
     * @param itemName 物品名称
     * @return 返回被使用的物品，null表示失败
     */
    public Item useItem(String itemName) {
        Item item = getItem(itemName);
        if (item != null) {
            inventory.remove(item);
        }
        return item;
    }

    /**
     * 丢弃全部数量的某种物品
     * @param itemName 物品名称
     * @return 丢弃的物品数量
     */
    public int discardAllItem(String itemName) {
        int count = 0;
        Iterator<Item> it = inventory.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            if (item.getName().equalsIgnoreCase(itemName)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * 背包是否为空
     */
    public boolean isEmpty() {
        return inventory.isEmpty();
    }
}
