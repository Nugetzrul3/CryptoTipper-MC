package org.Nugetzrul3.CryptoTipper.commands;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.Nugetzrul3.CryptoTipper.CommandWrapper;
import org.Nugetzrul3.CryptoTipper.Constants;
import org.Nugetzrul3.CryptoTipper.Utils;
import org.Nugetzrul3.CryptoTipper.db.UserRepository;
import org.Nugetzrul3.CryptoTipper.rpcclient.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.api.chat.TextComponent;

public class Withdraw implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Methods methods;
    private final Constants constants;
    private final UserRepository userRepository;

    public Withdraw(JavaPlugin plugin) {
        if (plugin.getDescription().getCommands().containsKey("withdraw")) {
            plugin.getCommand("withdraw").setExecutor(new CommandWrapper(this, plugin));
            this.plugin = plugin;
            this.methods = new Methods();
            this.constants = new Constants();
            this.userRepository = new UserRepository();
        } else {
            throw new Error("Withdraw command not found!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;

        if (args.length == 0 || args.length > 2) {
            player.sendMessage(ChatColor.RED + "Usage: /withdraw <amount> <address (optional)> \n"
                + ChatColor.WHITE + "Note: if address is not passed, it will use your last known withdrawal address, if any.");
            return false;
        }

        if (!Utils.isDouble(args[0])) {
            player.sendMessage(ChatColor.RED + "Invalid withdraw amount!");
            return false;
        }

        double amount = Double.parseDouble(args[0]);

        this.methods.getUserBalance(
            player.getUniqueId().toString()
        ).thenAccept(balResponse -> {
            if (!(balResponse.get("error") instanceof JsonNull)) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                    ChatColor.RED + "Error processing withdrawal! Contact admins and show them this: \n" +
                        "Error: " + balResponse.get("error").toString()
                ));
                return;
            }


            if (Double.parseDouble(balResponse.get("confBal").getAsString()) < amount) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                    ChatColor.RED + "That amount exceeds how much " + this.constants.ticker + " you have\n"
                        + ChatColor.WHITE + "You're current balance: " + ChatColor.GREEN + balResponse.get("confBal").getAsString()
                        + " " + this.constants.ticker
                ));
                return;
            }

            Double sendAmount = amount - this.constants.withdraw_fee;

            // Use last known address
            if (args.length == 1) {
                this.userRepository.getUserByUuid(
                    player.getUniqueId().toString()
                ).thenAccept(user -> {
                    if (user.withdraw_addr() == null || user.withdraw_addr().isEmpty() || user.withdraw_addr().equals("null")) {
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.RED + "You don't have a previous withdrawal address! Please supply one."));
                        return;
                    }

                    this.methods.withdraw(
                        user.withdraw_addr(),
                        sendAmount,
                        player.getUniqueId().toString()
                    ).thenAccept(response -> {
                        if (!(response.get("error") instanceof JsonNull)) {
                            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                                ChatColor.RED + "Error processing withdrawal! Contact admins and show them this: \n" +
                                    "Error: " + response.get("error").toString()
                            ));
                            return;
                        }

                        JsonObject withdrawResponse = response.get("result").getAsJsonObject();
                        String txid = withdrawResponse.get("txid").getAsString();
                        TextComponent tc = new TextComponent();

                        tc.setText(
                            ChatColor.GREEN + "TXID:  " + ChatColor.UNDERLINE + txid
                        );
                        tc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, constants.explorer + txid));
                        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new Text(ChatColor.GRAY + "Click to copy to open transaction")));

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(
                                ChatColor.GREEN + "Success! Withdrew " + amount + " " + constants.ticker + "\n"
                                    + ChatColor.GRAY + "Note: Transaction may not be reflected on explorer yet. But \n"
                                    + "rest assured, you're " + constants.ticker + " has been sent"
                            );
                            player.spigot().sendMessage(tc);
                        });

                    });
                });
            } else {
                this.methods.validateAddress(args[1])
                    .thenAccept(validateResponse -> {
                        if (!(validateResponse.get("error") instanceof JsonNull)) {
                            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                                ChatColor.RED + "Error processing withdrawal! Contact admins and show them this: \n" +
                                    "Error: " + validateResponse.get("error").toString()
                            ));
                            return;
                        }

                        JsonObject result = validateResponse.get("result").getAsJsonObject();

                        if (!result.get("isvalid").getAsBoolean()) {
                            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                                ChatColor.RED + "That address is not valid! Please try again."
                            ));
                            return;
                        }

                        this.methods.withdraw(
                            args[1],
                            sendAmount,
                            player.getUniqueId().toString()
                        ).thenAccept(response -> {
                            if (!(response.get("error") instanceof JsonNull)) {
                                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                                    ChatColor.RED + "Error processing withdrawal! Contact admins and show them this: \n" +
                                        "Error: " + response.get("error").toString()
                                ));
                                return;
                            }

                            JsonObject withdrawResponse = response.get("result").getAsJsonObject();
                            String txid = withdrawResponse.get("txid").getAsString();
                            TextComponent tc = new TextComponent();

                            tc.setText(
                                ChatColor.GREEN + "TXID: " + ChatColor.UNDERLINE + txid
                            );
                            tc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, constants.explorer + txid));
                            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text(ChatColor.GRAY + "Click to copy to open transaction")));

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage(
                                    ChatColor.GREEN + "Success! Withdrew " + amount + " " + constants.ticker + "\n"
                                        + ChatColor.GRAY + "Note: Transaction may not be reflected on explorer yet. But \n"
                                        + "rest assured, you're " + constants.ticker + " has been sent"
                                );
                                player.spigot().sendMessage(tc);
                            });

                            this.userRepository.updateUserAddress(
                                player.getUniqueId().toString(),
                                args[1],
                                "withdraw"
                            );
                        });

                    });
            }

        });

        return false;
    }
}
