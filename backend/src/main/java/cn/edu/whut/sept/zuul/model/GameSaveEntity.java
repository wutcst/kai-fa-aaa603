package cn.edu.whut.sept.zuul.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_save")
public class GameSaveEntity {

    @Id
    private Long id;

    private String playerName = "冒险者";

    private int hp = 100;
    private int maxHp = 100;
    private int mp = 100;
    private int maxMp = 100;
    private int attack = 20;
    private int magicAttack = 15;
    private int defense = 10;
    private int magicResist = 0;
    private int speed = 100;
    private int dodge = 0;
    private int money = 50;

    private String currentRoom;
    private long mapSeed;

    private String equippedCloak;
    private String equippedRing;
    private String equippedAmulet;
    private String equippedWeapon;
    private String equippedArmor;

    @Column(columnDefinition = "TEXT")
    private String statusJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public int getMp() { return mp; }
    public void setMp(int mp) { this.mp = mp; }
    public int getMaxMp() { return maxMp; }
    public void setMaxMp(int maxMp) { this.maxMp = maxMp; }
    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = attack; }
    public int getMagicAttack() { return magicAttack; }
    public void setMagicAttack(int magicAttack) { this.magicAttack = magicAttack; }
    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = defense; }
    public int getMagicResist() { return magicResist; }
    public void setMagicResist(int magicResist) { this.magicResist = magicResist; }
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
    public int getDodge() { return dodge; }
    public void setDodge(int dodge) { this.dodge = dodge; }
    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = money; }
    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String currentRoom) { this.currentRoom = currentRoom; }
    public long getMapSeed() { return mapSeed; }
    public void setMapSeed(long mapSeed) { this.mapSeed = mapSeed; }
    public String getEquippedCloak() { return equippedCloak; }
    public void setEquippedCloak(String equippedCloak) { this.equippedCloak = equippedCloak; }
    public String getEquippedRing() { return equippedRing; }
    public void setEquippedRing(String equippedRing) { this.equippedRing = equippedRing; }
    public String getEquippedAmulet() { return equippedAmulet; }
    public void setEquippedAmulet(String equippedAmulet) { this.equippedAmulet = equippedAmulet; }
    public String getEquippedWeapon() { return equippedWeapon; }
    public void setEquippedWeapon(String equippedWeapon) { this.equippedWeapon = equippedWeapon; }
    public String getEquippedArmor() { return equippedArmor; }
    public void setEquippedArmor(String equippedArmor) { this.equippedArmor = equippedArmor; }
    public String getStatusJson() { return statusJson; }
    public void setStatusJson(String statusJson) { this.statusJson = statusJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
