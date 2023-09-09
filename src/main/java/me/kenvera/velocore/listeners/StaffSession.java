package me.kenvera.velocore.listeners;

import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.managers.SqlConnection;

public class StaffSession {
    private final ProxyServer proxy;
    private final SqlConnection dataBase;
    public StaffSession(ProxyServer proxy, SqlConnection dataBase) {
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
