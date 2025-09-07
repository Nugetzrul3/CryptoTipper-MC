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

public class Help implements CommandExecutor {
    private final Constants constants;

    public Help(JavaPlugin plugin) {
        if (plugin.getDescription().getCommands().containsKey("tiphelp")) {
            plugin.getCommand("tiphelp").setExecutor(new CommandWrapper(this, plugin));
            this.constants = new Constants();
        } else {
            throw new Error("Help command not found!");
        }

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        player.sendMessage(ChatColor.BLUE + "Welcome to the " + ChatColor.BLUE + this.constants.coinName + " MC Tipbot. Here are my commands\n" +
            ChatColor.DARK_PURPLE + "1. /tiphelp: " + ChatColor.WHITE + "Displays this help message\n" +
            ChatColor.DARK_PURPLE + "2. /tip <username> <amount>: " + ChatColor.WHITE + "Tip's a certain amount of " + this.constants.ticker + " to another user\n" +
            ChatColor.DARK_PURPLE + "3. /deposit: " + ChatColor.WHITE + "Gives you a " + this.constants.ticker + " address to deposit " + this.constants.ticker + " to\n" +
            ChatColor.DARK_PURPLE + "4. /withdraw <amount> <" + this.constants.ticker + " address>: " + ChatColor.WHITE + "Withdraw a certain amount from your bot balance\n" +
            "Note: A withdrawal fee of " + this.constants.withdraw_fee + " " + this.constants.ticker + " will be incurred per withdrawal\n" +
            ChatColor.DARK_PURPLE + "5. /info: " + ChatColor.WHITE + "Returns general information on the " + this.constants.coinName + " blockchain\n" +
            ChatColor.DARK_PURPLE + "6. /balance: " + ChatColor.WHITE + "Returns your current account balance"
        );

        return false;
    }
}
