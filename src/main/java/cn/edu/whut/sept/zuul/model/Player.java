package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家类：管理玩家状态、背包和负重
 */
@Getter
@Setter
public class Player {
    private String name;
    private Room currentRoom;
    private List<Item> inventory;
    private double maxWeight;

    public Player(String name, Room currentRoom) {
        this.name = name;
        this.currentRoom = currentRoom;
        this.inventory = new ArrayList<>();
        this.maxWeight = 10.0;
    }

    /**
     * 计算当前背包总重量
     */
    public double getTotalWeight() {
        double total = 0;
        for (Item item : inventory) {
            total += item.getWeight();
        }
        return total;
    }

    /**
     * 判断能否携带此物品（不超过最大负重）
     */
    public boolean canCarry(Item item) {
        return getTotalWeight() + item.getWeight() <= maxWeight;
    }

    /**
     * 添加物品到背包
     */
    public boolean addItem(Item item) {
        if (canCarry(item)) {
            inventory.add(item);
            return true;
        }
        return false;
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
     * 增加最大负重
     */
    public void increaseMaxWeight(double amount) {
        this.maxWeight += amount;
    }
}