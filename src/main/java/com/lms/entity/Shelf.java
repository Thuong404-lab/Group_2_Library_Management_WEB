package com.lms.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "Shelves")
public class Shelf {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer shelfId;
    @Column(nullable = false, length = 100)
    private String shelfName;
    @Column(length = 255)
    private String location;

    public Shelf() {
    }

    public Shelf(Integer shelfId, String shelfName, String location) {
        this.shelfId = shelfId;
        this.shelfName = shelfName;
        this.location = location;
    }

    public Integer getShelfId() { return shelfId; }
    public void setShelfId(Integer shelfId) { this.shelfId = shelfId; }
    public String getShelfName() { return shelfName; }
    public void setShelfName(String shelfName) { this.shelfName = shelfName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
