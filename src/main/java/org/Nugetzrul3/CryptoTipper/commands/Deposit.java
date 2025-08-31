package org.Nugetzrul3.CryptoTipper.commands;

import com.google.gson.JsonNull;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.Nugetzrul3.CryptoTipper.CommandWrapper;
import org.Nugetzrul3.CryptoTipper.Constants;
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

public class Deposit implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Methods methods;
    private final Constants constants;
    private final UserRepository userRepository;

    public Deposit(JavaPlugin plugin) {
        if (plugin.getDescription().getCommands().containsKey("deposit")) {
            plugin.getCommand("deposit").setExecutor(new CommandWrapper(this, plugin));
            this.plugin = plugin;
            this.constants =  new Constants();
            this.methods = new Methods();
            this.userRepository = new UserRepository();
        } else {
            throw new Error("Deposit command not found!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;

        this.userRepository.getUserByUuid(
                player.getUniqueId().toString()
        ).thenAccept(user -> {
            if (user.address() == null || user.address().isEmpty() || user.address().equals("null")) {
                this.methods.getDepositAddress(player.getUniqueId().toString())
                        .thenAccept(response -> {
                            if (!(response.get("error") instanceof JsonNull)) {
                                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(
                                        ChatColor.RED + "Error getting deposit address! Contact admins and show them this: \n" +
                                                "Error: " + response.get("error").toString()
                                ));
                                return;
                            }

                            String newAddress = response.get("result").getAsString();

                            // Now update DB inside the async callback
                            this.userRepository.updateUserAddress(player.getUniqueId().toString(), newAddress, "deposit");

                            // Build and send the TextComponent on the main thread
                            TextComponent tc = new TextComponent();
                            tc.setText(ChatColor.GREEN + "Your " + constants.ticker +  " deposit address: " +
                                    ChatColor.LIGHT_PURPLE + newAddress + ". " + ChatColor.UNDERLINE + "Click to copy");
                            tc.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, newAddress));
                            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new Text(ChatColor.GRAY + "Click to copy to clipboard")));

                            Bukkit.getScheduler().runTask(plugin, () -> player.spigot().sendMessage(tc));
                        });
            } else {
                // Use DB value
                String dbAddress = user.address();
                TextComponent tc = new TextComponent();
                tc.setText(ChatColor.GREEN + "Your " + constants.ticker +  " deposit address: " +
                        ChatColor.LIGHT_PURPLE + dbAddress + ". " + ChatColor.UNDERLINE + "Click to copy");
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dbAddress));
                tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(ChatColor.GRAY + "Click to copy to clipboard")));

                Bukkit.getScheduler().runTask(plugin, () -> player.spigot().sendMessage(tc));
            }
        });

        return false;
    }
}
