package org.Nugetzrul3.CryptoTipper.commands;

import org.Nugetzrul3.CryptoTipper.CommandWrapper;
import org.Nugetzrul3.CryptoTipper.db.UserRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Hello implements CommandExecutor {
    private final UserRepository userRepository;
    private final JavaPlugin plugin;

    public Hello(JavaPlugin plugin) {
        if (plugin.getDescription().getCommands().containsKey("hello")) {
            plugin.getCommand("hello").setExecutor(new CommandWrapper(this, plugin));
            this.userRepository = new UserRepository();
            this.plugin = plugin;
        } else {
            throw new Error("Hello command not found!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;

        this.userRepository.getUserByUuid(
                player.getUniqueId().toString()
        ).thenAccept(user -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(
                        ChatColor.GREEN + "User details in DB: \n"
                        + ChatColor.GREEN + "UUID: " + user.uuid() + "\n"
                        + ChatColor.GREEN + "Username: " + user.username() + "\n"
                        + ChatColor.GREEN + "ID: "  + user.id() + "\n"
                );
            });
        });

        return false;

    }
}
