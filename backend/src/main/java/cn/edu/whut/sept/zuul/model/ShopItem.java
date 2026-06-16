package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 商店商品类：名称、价格、是否已售出
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopItem {
    private String name;
    private int price;
    private boolean sold;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("price", price);
        map.put("sold", sold);
        return map;
    }
}
