package me.kenvera.velocore.managers;

import me.kenvera.velocore.VeloCore;

import java.sql.SQLException;
import java.util.UUID;

public class Ban {
    private String playerUUID, issuer, reason;
    long expire, bannedTime, id;

    public Ban(Long id, String playerUUID, String issuer, String reason, long expire, long bannedTime) {
        this.playerUUID = playerUUID;
        this.issuer = issuer;
        this.reason = reason;
        this.expire = expire;
        this.bannedTime = bannedTime;
        this.id = id;
    }

    public String getUsername(VeloCore plugin) {
        return plugin.getBanManager().getUsername(playerUUID);
    }

    public String getReason() {
        return reason;
    }

    public Long getExpire() {
        return expire;
    }

    public Long getId() {
        return id;
    }

    public Long getBannedTime() {
        return bannedTime;
    }

    public String getIssuer() {
        return issuer;
    }
}
