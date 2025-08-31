package org.Nugetzrul3.CryptoTipper;

import org.Nugetzrul3.CryptoTipper.commands.Balance;
import org.Nugetzrul3.CryptoTipper.commands.Hello;
import org.Nugetzrul3.CryptoTipper.commands.Help;
import org.Nugetzrul3.CryptoTipper.commands.Info;
import org.Nugetzrul3.CryptoTipper.db.Database;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.*;

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
            new Hello(this);
            new Info(this);
            new Help(this);
            new Balance(this);
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
