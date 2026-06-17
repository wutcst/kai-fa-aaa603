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

    public Bag() {
        this.inventory = new ArrayList<>();
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
     * 根据名称在背包中查找物品
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
     * 获取背包物品列表（合并同类物品，带数量统计）
     * 返回 InventoryItem 列表供前端背包UI使用
     */
    public List<InventoryItem> getBackpackItems() {
        // 统计每种物品的数量
        Map<String, List<Item>> grouped = inventory.stream()
                .collect(Collectors.groupingBy(Item::getName));

        List<InventoryItem> result = new ArrayList<>();
        int idCounter = 1;
        for (Map.Entry<String, List<Item>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<Item> items = entry.getValue();
            InventoryItem invItem = InventoryItem.fromItem(items.get(0), items.size(), idCounter++);
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
