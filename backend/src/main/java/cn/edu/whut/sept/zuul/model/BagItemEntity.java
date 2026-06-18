package cn.edu.whut.sept.zuul.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bag_item")
public class BagItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "save_id")
    private Long saveId;

    @Column(name = "item_name")
    private String itemName;

    public BagItemEntity() {}
    public BagItemEntity(Long saveId, String itemName) {
        this.saveId = saveId;
        this.itemName = itemName;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSaveId() { return saveId; }
    public void setSaveId(Long saveId) { this.saveId = saveId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
}
