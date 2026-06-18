package cn.edu.whut.sept.zuul.model;

import jakarta.persistence.*;

@Entity
@Table(name = "room_state", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"save_id", "room_name"})
})
public class RoomStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "save_id")
    private Long saveId;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "monsters_cleared")
    private boolean monstersCleared;

    @Column(name = "altar_used")
    private boolean altarUsed;

    @Column(name = "random_event_used")
    private boolean randomEventUsed;

    public RoomStateEntity() {}
    public RoomStateEntity(Long saveId, String roomName, boolean monstersCleared,
                           boolean altarUsed, boolean randomEventUsed) {
        this.saveId = saveId;
        this.roomName = roomName;
        this.monstersCleared = monstersCleared;
        this.altarUsed = altarUsed;
        this.randomEventUsed = randomEventUsed;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSaveId() { return saveId; }
    public void setSaveId(Long saveId) { this.saveId = saveId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public boolean isMonstersCleared() { return monstersCleared; }
    public void setMonstersCleared(boolean monstersCleared) { this.monstersCleared = monstersCleared; }
    public boolean isAltarUsed() { return altarUsed; }
    public void setAltarUsed(boolean altarUsed) { this.altarUsed = altarUsed; }
    public boolean isRandomEventUsed() { return randomEventUsed; }
    public void setRandomEventUsed(boolean randomEventUsed) { this.randomEventUsed = randomEventUsed; }
}
