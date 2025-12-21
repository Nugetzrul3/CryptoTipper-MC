package org.Nugetzrul3.CryptoTipper.commands;

import org.Nugetzrul3.CryptoTipper.CommandWrapper;
import org.Nugetzrul3.CryptoTipper.Constants;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.math.BigDecimal;

public class Help implements CommandExecutor {

    public Help(JavaPlugin plugin) {
        if (plugin.getDescription().getCommands().containsKey("tiphelp")) {
            plugin.getCommand("tiphelp").setExecutor(new CommandWrapper(this, plugin));
        } else {
            throw new Error("Help command not found!");
        }

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        player.sendMessage(ChatColor.BLUE + "Welcome to the " + ChatColor.BLUE + Constants.coinName + " MC Tipbot. Here are my commands\n" +
            ChatColor.DARK_PURPLE + "1. /tiphelp: " + ChatColor.WHITE + "Displays this help message\n" +
            ChatColor.DARK_PURPLE + "2. /tip <username> <amount>: " + ChatColor.WHITE + "Tip's a certain amount of " + Constants.ticker + " to another user\n" +
            ChatColor.DARK_PURPLE + "3. /deposit: " + ChatColor.WHITE + "Gives you a " + Constants.ticker + " address to deposit " + Constants.ticker + " to\n" +
            ChatColor.DARK_PURPLE + "4. /withdraw <amount> <" + Constants.ticker + " address>: " + ChatColor.WHITE + "Withdraw a certain amount from your bot balance\n" +
            "Note: A withdrawal fee of " + BigDecimal.valueOf(Constants.withdraw_fee).toPlainString() + " " + Constants.ticker + " will be incurred per withdrawal\n" +
            ChatColor.DARK_PURPLE + "5. /info: " + ChatColor.WHITE + "Returns general information on the " + Constants.coinName + " blockchain\n" +
            ChatColor.DARK_PURPLE + "6. /balance: " + ChatColor.WHITE + "Returns your current account balance"
        );

        return false;
    }
}
