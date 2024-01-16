package me.kenvera.velocore.managers;

import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Utils {
    private final VeloCore plugin;
    public Utils(VeloCore plugin) {
        this.plugin = plugin;
    }

    public static Component formatBannedMessage(VeloCore plugin, String issuer, String reason, long expires, long id) {
        String defaultMessage;
        if (expires != -1) {
            defaultMessage = "§cYou have been temporarily banned from the server!";
            return Component.text(plugin.getConfigManager().getString("message.ban-message-duration", defaultMessage)
                    .replace("&", "§")
                    .replace("%reason%", reason)
                    .replace("%expire%", parseDateTime(expires, true))
                    .replace("%id%", String.valueOf(id)));
        } else {
            defaultMessage = "§cYou have been banned from the server!";
            return Component.text(plugin.getConfigManager().getString("message.ban-message-permanent", defaultMessage)
                    .replace("&", "§")
                    .replace("%reason%", reason)
                    .replace("%id%", String.valueOf(id)));
        }
    }

    public static Component formatBannedMessage(Ban ban, VeloCore plugin) {
            return formatBannedMessage(plugin, ban.getUsername(plugin), ban.getReason(), ban.getExpire(), ban.getId());
    }

    public static Component formatBanMessage(VeloCore plugin){
        String defaultMessage = "Someone has been banned!";
            return Component.text(plugin.getConfigManager().getString("message.ban-broadcast", defaultMessage).replace("&", "§"));
    }

    public static Component formatBanBroadcast(VeloCore plugin){
        String defaultMessage = "Someone has been banned!";
        return Component.text(plugin.getConfigManager().getString("message.ban-broadcast", defaultMessage).replace("&", "§"));
    }

    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String parseDateTime(long milliSeconds, boolean localTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (localTime) {
            // USE THIS FOR LOCAL TIME (JAKARTA) ZONE
            Instant instant = Instant.ofEpochMilli(milliSeconds);
            LocalDateTime dateTime = instant.atZone(ZoneId.of("Asia/Jakarta")).toLocalDateTime();
            return dateTime.format(formatter);
        } else {
            // USE THIS FOR UTC TIME ZOME
            Instant instant = Instant.ofEpochMilli(milliSeconds);
            LocalDateTime dateTime = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
            return dateTime.format(formatter);
        }
    }
}
