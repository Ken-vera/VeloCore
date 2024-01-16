package me.kenvera.velocore.hooks;

import me.kenvera.velocore.VeloCore;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DiscordLogger {
    private final VeloCore plugin;
    public DiscordLogger(VeloCore plugin) {
        this.plugin = plugin;
    }

    public void logEmbedBan(String banId, String playerName, String uuid, String ip, String client, String banType, String duration, String expiredTime, String issuer, String reason, Long... textChannelIds) {
        for (Long textChannelId : textChannelIds) {
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Ban Report")
                    .addField("Ban ID", banId, false)
                    .addField("Player Name", playerName, true)
                    .addField("UUID", uuid, true)
                    .addField("IP Address", ip, false)
                    .addField("Client", client, false)
                    .addField("Ban Type", banType, true)
                    .addField("Duration", duration, true)
                    .addField("Expired Time", expiredTime, true)
                    .addField("Issuer", issuer, true)
                    .addField("Reason", reason, true)
                    .setThumbnail("https://minotar.net/cube/" + playerName + "/100.png")
                    .setFooter("Crazy Network:New Era", "https://cdn.discordapp.com/attachments/1193020801134375052/1195597169705635950/OIG_1.png?ex=65b491a2&is=65a21ca2&hm=eeefd4e94408a3a9e8f7918e7930dff49569f5c51768e42f087a2096ef41ba46&")
                    .setTimestamp(Instant.ofEpochSecond(longNow()))
                    .build();
            plugin.getDiscordChannel().sendEmbed(textChannelId, embed);
        }
    }

    private Long longNow() {
        ZoneId jakartaZoneId = ZoneId.of("Asia/Jakarta");
        LocalDateTime localDateTime = LocalDateTime.now(jakartaZoneId);
        return localDateTime.atZone(jakartaZoneId).toEpochSecond();
    }
}
