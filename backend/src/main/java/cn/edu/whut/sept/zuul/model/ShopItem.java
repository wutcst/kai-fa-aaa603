package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 商店商品类：关联注册表中的实际物品、价格、是否已售出
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopItem {
    /** 商品对应的实际物品（来自注册表） */
    private Item item;
    /** 商品价格 */
    private int price;
    /** 是否已售出 */
    private boolean sold;

    /**
     * 根据物品名称构造（从注册表中获取物品原型）
     */
    public ShopItem(String itemName) {
        Item catalogItem = ItemRegistry.getItem(itemName);
        this.item = catalogItem != null ? catalogItem : new Item(itemName);
        this.price = catalogItem != null ? catalogItem.getPrice() : 50;
        this.sold = false;
    }

    /**
     * 获取商品名称（快捷方法）
     */
    public String getName() {
        return item != null ? item.getName() : "未知商品";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", getName());
        map.put("price", price);
        map.put("sold", sold);
        map.put("description", item != null ? item.getDescription() : "");
        return map;
    }
}
