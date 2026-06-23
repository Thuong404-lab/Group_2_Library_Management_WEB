package com.lms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "SystemLogs")
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;
    @Column(name = "account_id")
    private Integer accountId;
    @Column(name = "action_type", length = 100, nullable = false)
    private String actionType;
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    @Column(name = "user_agent", columnDefinition = "NVARCHAR(MAX)")
    private String userAgent;
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime created_at;

    public SystemLog() {
    }

    public SystemLog(Integer logId, Integer accountId, String actionType, String ipAddress, String userAgent, String description, LocalDateTime created_at) {
        this.logId = logId;
        this.accountId = accountId;
        this.actionType = actionType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.description = description;
        this.created_at = created_at;
    }

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }
}
