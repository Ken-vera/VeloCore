package me.kenvera.velocore.listeners;

import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.database.SqlManager;

public class StaffSession {
    private final VeloCore plugin;
    private final SqlManager dataBase;
    public StaffSession(VeloCore plugin) {
        this.plugin = plugin;
        this.dataBase = plugin.getSqlConnection();
    }
}
