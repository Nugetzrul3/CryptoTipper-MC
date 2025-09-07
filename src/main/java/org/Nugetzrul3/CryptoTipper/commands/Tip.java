package org.Nugetzrul3.CryptoTipper.commands;

import com.google.gson.JsonNull;
import org.Nugetzrul3.CryptoTipper.CommandWrapper;
import org.Nugetzrul3.CryptoTipper.Constants;
import org.Nugetzrul3.CryptoTipper.Utils;
import org.Nugetzrul3.CryptoTipper.db.UserRepository;
import org.Nugetzrul3.CryptoTipper.rpcclient.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class Tip implements CommandExecutor {
    private final Methods methods;
    private final JavaPlugin plugin;
    private final Constants constants;
    private final UserRepository userRepository;

    public Tip(JavaPlugin plugin) {
        if (plugin.getDescription().getCommands().containsKey("tip")) {
            plugin.getCommand("tip").setExecutor(new CommandWrapper(this, plugin));
            this.plugin = plugin;
            this.constants = new Constants();
            this.methods = new Methods();
            this.userRepository = new UserRepository();
        } else {
            throw new Error("Info command not found!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;

        if (args.length == 0 || args.length > 2) {
            player.sendMessage(ChatColor.RED + "Usage: /tip <amount> <username> \n");
            return false;
        }

        if (!Utils.isDouble(args[0])) {
            player.sendMessage(ChatColor.RED + "Invalid tip amount!");
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

            if (balResponse.get("confBal").getAsDouble() < amount) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                    ChatColor.RED + "That amount exceeds how much " + this.constants.ticker + " you have\n"
                        + ChatColor.WHITE + "You're current balance: " + ChatColor.GREEN + balResponse.get("confBal").getAsString()
                        + " " + this.constants.ticker
                ));
                return;
            }

            // User lookup
            String receiverUsername = args[1];

            this.userRepository.getUserByUsername(receiverUsername)
                .thenAccept(user -> {
                    if (user == null) {
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                            ChatColor.RED + "That user does not exist! Either they have changed\n"
                            + ChatColor.RED + "their username or they have not used the bot yet.\n"
                            + ChatColor.RED + "Ask them to run /tiphelp to make sure their user is\n"
                            + ChatColor.RED + "updated in our database!"
                        ));
                        return;
                    }

                    if (user.uuid().equals(player.getUniqueId().toString())) {
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                            ChatColor.RED + "You cannot tip yourself!\n"
                        ));
                        return;
                    }

                    OfflinePlayer receiver = Bukkit.getOfflinePlayer(UUID.fromString(user.uuid()));

                    this.methods.move(
                        player.getUniqueId().toString(),
                        user.uuid(),
                        amount
                    ).thenAccept(response -> {
                       if (!(response.get("error") instanceof JsonNull)) {
                           Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                               ChatColor.RED + "Error sending tip! Contact admins and show them this: \n" +
                                   "Error: " + balResponse.get("error").toString()
                           ));
                           return;
                       }

                       Bukkit.getScheduler().runTask(plugin, () -> {
                           player.sendMessage(
                               ChatColor.GREEN + "Success! Tipped " + receiverUsername + " " + BigDecimal.valueOf(amount).toPlainString() + " " + this.constants.ticker + "\n"
                                   + ChatColor.WHITE + "They will also be notified of the tip if they are online :)"
                           );

                           if (receiver.isOnline()) {
                               Player recieverPlayer = receiver.getPlayer();
                               recieverPlayer.sendMessage(
                                   ChatColor.GREEN + "Hey! " + player.getName() + " just tipped you " + BigDecimal.valueOf(amount).toPlainString() + " " + this.constants.ticker + "\n"
                                       + ChatColor.WHITE + ChatColor.BOLD + "Be sure to thank them!"
                               );
                           }
                       });
                    });

                });

        });

        return false;
    }
}
