package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物品类：名称、描述、重量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    private String name;       // 物品名称
    private String description;// 物品描述
    private double weight;     // 物品重量

    // 简化构造器（兼容原有逻辑）
    public Item(String name, double weight) {
        this.name = name;
        this.weight = weight;
        this.description = "A common " + name;
    }
}