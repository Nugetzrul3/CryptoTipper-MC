package org.Nugetzrul3.CryptoTipper;

import org.Nugetzrul3.CryptoTipper.commands.*;
import org.Nugetzrul3.CryptoTipper.db.Database;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.SQLException;

public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        // Plugin startup logic
        Server server = getServer();
        CommandSender sender = server.getConsoleSender();
        sender.sendMessage("CryptoTipper has been enabled!");
        Database db = Database.getInstance();

        try {
            db.initDb();
            new Info(this);
            new Help(this);
            new Balance(this);
            new Deposit(this);
            new Withdraw(this);
            new Tip(this);
        } catch (SQLException ex) {
            sender.sendMessage(ChatColor.RED + "Error while initializing database and commands!");
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Server server = getServer();
        CommandSender sender = server.getConsoleSender();
        sender.sendMessage("CryptoTipper has been disabled!");
    }
}
