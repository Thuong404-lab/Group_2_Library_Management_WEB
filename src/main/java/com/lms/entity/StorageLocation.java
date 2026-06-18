package com.lms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BookStorages")
public class StorageLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storage_id")
    private Integer id;

    @Column(name = "location_name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    public StorageLocation(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public StorageLocation() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
