package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.datamanager.DataBase;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class StaffSession {
    private final ProxyServer proxy;
    private final DataBase dataBase;
    public StaffSession(ProxyServer proxy, DataBase dataBase) {
        this.proxy = proxy;
        this.dataBase = dataBase;
    }

//    @Subscribe
//    public void onStaffQuit(DisconnectEvent event) {
//        Player player = event.getPlayer();
//        UUID uuid = player.getUniqueId();
//        String playerName = player.getUsername();
//
//        if (player.getPermissionValue("velocity.staff") == Tristate.TRUE) {
//            dataBase.saveStaffSession(uuid, playerName);
//        }
//    }

//    @Subscribe
//    public void onStaffJoin(ServerPostConnectEvent event) {
//        Player player = event.getPlayer();
//        UUID uuid = player.getUniqueId();
//
//        if (player.getPermissionValue("velocity.staff") == Tristate.TRUE) {
//            dataBase.loadStaffSession(uuid);
//        }
//    }
}
