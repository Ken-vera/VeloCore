package me.kenvera.velocore.managers;

import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;

import java.text.SimpleDateFormat;

public class Utils {
    public final static SimpleDateFormat UNBAN_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public static Component formatBannedMessage(String issuer, String reason, long expires, long id) {
        String duration = "Permanent";
        if (expires != -1) {
            duration = UNBAN_DATE_FORMAT.format(expires * 1000);
            return Component.text("§c§lYou are temporarily banned from the server!")
                    .appendNewline()
                    .appendNewline()
                    .append(Component.text("§7Reason: " + reason))
                    .appendNewline()
                    .append(Component.text("§7Duration: " + duration))
                    .appendNewline()
                    .append(Component.text("§7ID: " + id));
        } else {
            return Component.text("§c§lYou are banned from the server!")
                    .appendNewline()
                    .appendNewline()
                    .append(Component.text("§7Reason: " + reason))
                    .appendNewline()
                    .append(Component.text("§7Duration: " + duration))
                    .appendNewline()
                    .append(Component.text("§7ID: " + id));
        }
    }

    public static Component formatBannedMessage(Ban ban, VeloCore plugin) {
        return formatBannedMessage(ban.getUsername(plugin), ban.getReason(), ban.getExpire(), ban.getId());
    }
}
