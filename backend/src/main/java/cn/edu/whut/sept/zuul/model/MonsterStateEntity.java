package cn.edu.whut.sept.zuul.model;

import jakarta.persistence.*;

@Entity
@Table(name = "monster_state", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"save_id", "room_name", "monster_name"})
})
public class MonsterStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "save_id")
    private Long saveId;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "monster_name")
    private String monsterName;

    @Column(name = "hp")
    private int hp;

    @Column(name = "max_hp")
    private int maxHp;

    @Column(name = "monster_type")
    private int monsterType;

    public MonsterStateEntity() {}
    public MonsterStateEntity(Long saveId, String roomName, String monsterName,
                              int hp, int maxHp, int monsterType) {
        this.saveId = saveId;
        this.roomName = roomName;
        this.monsterName = monsterName;
        this.hp = hp;
        this.maxHp = maxHp;
        this.monsterType = monsterType;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSaveId() { return saveId; }
    public void setSaveId(Long saveId) { this.saveId = saveId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getMonsterName() { return monsterName; }
    public void setMonsterName(String monsterName) { this.monsterName = monsterName; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public int getMonsterType() { return monsterType; }
    public void setMonsterType(int monsterType) { this.monsterType = monsterType; }
}
