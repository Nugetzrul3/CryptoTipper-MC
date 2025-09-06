package org.Nugetzrul3.CryptoTipper;

import org.Nugetzrul3.CryptoTipper.db.UserRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.*;

public class CommandWrapper implements CommandExecutor {
    private final CommandExecutor inner;
    private final JavaPlugin plugin;
    private final UserRepository userRepository;

    public CommandWrapper(CommandExecutor inner, JavaPlugin plugin) {
        this.inner = inner;
        this.plugin = plugin;
        this.userRepository = new UserRepository();
    }

    // Check if the user exists in db by searching their uuid
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players");
            return false;
        }

        String uuid = player.getUniqueId().toString();
        String username = player.getName();

        this.userRepository.upsertUser(uuid, username)
            .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> inner.onCommand(sender, cmd, label, args)))
            .exceptionally(ex -> {
                plugin.getServer().getScheduler().runTask(plugin,
                    () -> player.sendMessage(ChatColor.RED + "Database Error! Contact the admins"));
                ex.printStackTrace(System.err);
                return null;
            });

        return true;
    }
}