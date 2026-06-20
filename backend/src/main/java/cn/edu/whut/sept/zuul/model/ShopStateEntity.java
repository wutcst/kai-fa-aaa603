package cn.edu.whut.sept.zuul.model;

import jakarta.persistence.*;

@Entity
@Table(name = "shop_state")
public class ShopStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "save_id")
    private Long saveId;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "item_name")
    private String itemName;

    public ShopStateEntity() {}
    public ShopStateEntity(Long saveId, String roomName, String itemName) {
        this.saveId = saveId;
        this.roomName = roomName;
        this.itemName = itemName;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSaveId() { return saveId; }
    public void setSaveId(Long saveId) { this.saveId = saveId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
}
