package com.lms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "SystemSettings")
public class SystemSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer settingId;
    @Column(unique = true, nullable = false, length = 100)
    private String settingKey;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String settingValue;
    @Column(length = 255)
    private String description;

    public SystemSetting() {
    }

    public SystemSetting(Integer settingId, String settingKey, String settingValue, String description) {
        this.settingId = settingId;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
    }

    public Integer getSettingId() {
        return settingId;
    }

    public void setSettingId(Integer settingId) {
        this.settingId = settingId;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
