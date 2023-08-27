package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StaffChat {
    private final ProxyServer proxy;
    private final Map<UUID, Boolean> playerStaffChat;
    private final LuckPerms luckPerms;
    public StaffChat(ProxyServer proxy, Map<UUID, Boolean> playerStaffChat) {
        this.proxy = proxy;
        this.playerStaffChat = playerStaffChat;
        this.luckPerms = LuckPermsProvider.get();
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID uuid = sender.getUniqueId();
        String message = event.getMessage().replaceAll("&&", "§");
        User user = luckPerms.getUserManager().getUser(uuid);
        String server = sender.getCurrentServer().get().getServerInfo().getName().toUpperCase();

        if (event.getResult() == PlayerChatEvent.ChatResult.denied()) {
            // This means the chat message was already denied (e.g., by another plugin), so we don't need to process it.
            return;
        }

        assert user != null;
        CachedMetaData metaData = user.getCachedData().getMetaData();
        String prefix = Objects.requireNonNull(metaData.getPrefix()).replaceAll("&", "§");
        boolean currentStatus = playerStaffChat.getOrDefault(uuid, false);



        if (currentStatus) {
            for (Player player : proxy.getAllPlayers()) {
                if (player.hasPermission("velocity.staff")) {
                    player.sendMessage(Component.text("§7[§cStaffChat§7] [§6" + server + "§7] " + prefix + " " + sender.getUsername() + " : §7" + message));
                }
            }
        }
    }
}
