package com.lms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BookStorages")
@Getter
@Setter
public class StorageLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storage_id")
    private Integer id;

    @Column(name = "location_name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;
}
