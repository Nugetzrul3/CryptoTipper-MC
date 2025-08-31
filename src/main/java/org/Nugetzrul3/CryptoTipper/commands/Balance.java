package org.Nugetzrul3.CryptoTipper.commands;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.Nugetzrul3.CryptoTipper.CommandWrapper;
import org.Nugetzrul3.CryptoTipper.Constants;
import org.Nugetzrul3.CryptoTipper.db.dao.User;
import org.Nugetzrul3.CryptoTipper.rpcclient.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Balance implements CommandExecutor {
    private final Methods methods = new Methods();;
    private final JavaPlugin plugin;
    private final Constants constants = new Constants();

    public Balance(JavaPlugin plugin) {
        if (plugin.getDescription().getCommands().containsKey("balance")) {
            plugin.getCommand("balance").setExecutor(new CommandWrapper(this, plugin));
            this.plugin = plugin;
        } else {
            throw new Error("Balance command not found!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;

        player.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "Getting balance...");
        this.methods.userBalance(
                player.getUniqueId().toString()
        ).thenAccept(response -> {
            System.out.println(response);
            if (!(response.get("error") instanceof JsonNull)) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                        ChatColor.RED + "Error getting balance! Contact admins and show them this: \n" +
                                "Error: " + response.get("error").toString()
                ));

                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(
                        ChatColor.AQUA + ChatColor.BOLD.toString() + "Your unconfirmed balance: " + response.get("unconfBal") + " " + constants.ticker + "\n"
                        + ChatColor.GREEN + ChatColor.BOLD + "Your confirmed balance: " + response.get("confBal") + " " + constants.ticker
                );
            });

        });

        return false;
    }
}
